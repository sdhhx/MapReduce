package cc.litstar.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.comm.CommClient;
import cc.litstar.comm.CommServer;
import cc.litstar.comm.message.MapJobDispatch;
import cc.litstar.comm.message.Message;
import cc.litstar.comm.message.ReduceJobDispatch;
import cc.litstar.comm.message.Shutdown;
import cc.litstar.config.ConfReader;
import cc.litstar.config.Configuration;
import cc.litstar.node.MasterInfo;
import cc.litstar.node.WorkerInfo;
import cc.litstar.task.Task;
import cc.litstar.utils.NetworkUtils;
import cc.litstar.utils.SftpUtils;

/**
 * 实现Worker节点的基本工作流程：
 * 		Worker仅仅是执行Map或Reduce操作的容器
 * 		当Master节点发送Shutdown信号时，节点关闭
 */
public class Worker {	
	//状态：单机或分布式。
	//Worker节点只能运行在分布式环境下
	private String mode;
	//当前节点信息(Master会进行校验，从配置文件中读取)
	private WorkerInfo workerInfo;
	
	//当前节点所对应的Master
	private MasterInfo masterInfo;
	
	//上面的一些信息需要读取配置文件
	private Configuration config;
	//任务信息，在执行过程中会被初始化若干次
	private Task task;
	private MapReduce taskExecutor;
	
	//Worker的消息队列
	LinkedBlockingQueue<Message> MQ = null;
	
	//输出日志信息
	private final static Logger logger = LoggerFactory.getLogger(Worker.class);
	
	public Worker() {
		config = ConfReader.getConf();
		mode = config.getMode();
		workerInfo = getWorkerInfo();
		masterInfo = config.getMaster();
		MQ = MessageQueue.getMQ();
	}
	
	/**
	 * 功能：校验当前节点的配置文件与实际情况，并返回当前节点信息：
	 * 		配置文件中的LocalIP是本机IP地址，而且在配置文件的IP中有这一项
	 * 		端口也要一致(一个程序实际占用消息队列端口和22号SFTP端口)
	 */
	public WorkerInfo getWorkerInfo() {
		//读取本机IP地址列表，判断当前IP地址是否在列表中
		List<String> localIPList = NetworkUtils.getLocalIP();		
		String confLocalIP = config.getLocalIP();
		for(String localIP : localIPList) {
			if(localIP.equals(confLocalIP)) {
				List<WorkerInfo> workerList = config.getWorkerList();
				for(WorkerInfo worker : workerList) {
					if(worker.getIpAddress().equals(confLocalIP) && worker.getPort() == config.getLocalPort()) {
						return worker;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * 设定当前任务的各项信息
	 */
	public Worker setTask(Task task) {
		this.task = task;
		this.taskExecutor = new MapReduce(task);
		return this;
	}
	
	public LinkedBlockingQueue<Message> getMQ() {
		return MQ;
	}
	
	/***************************************以下为任务执行逻辑********************************************/
	//启动Netty客户端用于通信协调工作流程
	private CommServer server = null;
	private CommClient client = null;
	//使用闭锁协调任务，收到Shutdown后退出任务，工作靠信号驱动
	private CountDownLatch doneGate = null;
	//线程池，协调同时执行的任务数量(IO密集型)
	ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	
	//初始化闭锁
	private void latchInit() {
		doneGate = new CountDownLatch(1);
	}
	
	/**
	 * 启动一个Worker节点，它可以接受并执行Master节点的各项任务
	 */
	public boolean startWorker() throws InterruptedException {
		if(mode.equals("Single")) {
			return false;
		} else if(mode.equals("Distributed")) {
			if(workerInfo == null) {
				logger.info("Worker init fail, cannot found config in file.");
				return false;
			}
			//启动netty通信流程
			server = new CommServer(config.getLocalIP(), config.getLocalPort());
			client = new CommClient();
			server.startServer();
			//初始化闭锁
			latchInit();
			//启动线程，用于消息监听与通知
			Thread MQListener = new Thread(new WorkerMQHandler(this));
			MQListener.start();
			sendRegisterToMaster();
			//只有Shutdown信号能打开这个闭锁
			doneGate.await();
			//等待任务完成的通知信号(假定不能预知会有多少个任务信息)
			MQListener.interrupt();
			threadPool.shutdown();
			logger.info("All down");
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Worker节点向Master节点发送注册信息
	 */
	public void sendRegisterToMaster() {
		logger.info("Sending register information to master");
		client.sendRegisterToMaster(config.getMaster(), workerInfo);
	}
	
	/**
	 * Worker接收到了Master发来的Map任务：
	 * 		1. 从Master节点接收所需要的文件(注意不要复制节点自身的文件。会导致出现文件大小为0的情况)
	 * 		2. 执行Map任务
	 * 		3. 收集Master节点所需要的信息，并发送任务完成信息
	 */
	public void receiveMapTaskFromMaster(MapJobDispatch message) {
		threadPool.execute(() -> {
			//1. 从Master节点接收到信息后，首先借助SFTP获取执行所需的文件
			int mapNumber = message.getMapNumber();
			List<String> src = new ArrayList<>();
			src.add(message.getFilePath());
			String dst = taskExecutor.AbsMapName(message.getMapNumber());
			logger.info("Get file: " + src + " from master");
			SftpUtils.getnFileFromRemote(masterInfo, src, dst);
			//2. 执行Map任务
			try {
				taskExecutor.doMap(mapNumber, task.getMapper().newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
			//3. 收集Master节点所需要的信息，并传回消息
			List<String> filePathList = new ArrayList<>();
			//约定Worker节点按照用于Reduce任务的顺序添加到文件列表中
			for(int i = 0; i < task.getnReduce(); i++) {
				filePathList.add(taskExecutor.AbsReduceName(mapNumber, i));
			}
			client.sendMapJobFinishToMaster(masterInfo, mapNumber, filePathList);
			logger.info("sending successful");
		});
	}
	
	/**
	 * Worker节点接收到了Master节点发来的Reduce任务：
	 * 		1. 从其他Worker节点获取自己所需的文件(注意不要复制节点自身的文件。会导致出现文件大小为0的情况)
	 * 		2. 执行Reduce任务
	 * 		3. 收集Master节点所需要的信息，发送任务完成信息
	 */
	public void receiveReduceTaskFromMaster(ReduceJobDispatch message) {
		threadPool.execute(() -> {
			//1. 从Master节点接收搭配消息后，首先借助SFTP收到获取所需的文件
			int reduceNumber = message.getReduceNumber();
			//节点列表
			List<WorkerInfo> workerList = message.getWorkerList();
			Map<Integer, Integer> workerMap = message.getWorkerMap();
			for(int i = 0; i < task.getnMap(); i++) {
				WorkerInfo mapWorker = workerList.get(workerMap.get(i));
				//源文件位置(远端)
				List<String> src = new ArrayList<>();
				src.add(message.getMapFilePath().get(i));
				//目的文件位置(本机)
				String dst = taskExecutor.AbsReduceName(i, reduceNumber);
				//从远端下载文件
				if(!new File(dst).exists()) {
					SftpUtils.getnFileFromRemote(mapWorker, src, dst);
				}
				logger.info("Get file: " + src + " from worker " + workerMap.get(i));
			}
			//2. 执行Reduce任务
			try {
				taskExecutor.doReduce(reduceNumber, task.getReducer().newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
			//3. 收集信息，构造报文回送Master，任务完成
			String reducePath = taskExecutor.AbsMergeName(reduceNumber);
			client.sendReduceJobFinishToMaster(masterInfo, reduceNumber, reducePath);
			logger.info("sending successful");
		});
	}
	
	/**
	 * 所有任务执行完成后，Worker节点收到Shutdown信息：
	 * 		Worker节点完成清理后退出
	 */
	public void receiveShutdownFromMaster(Shutdown message) {
		threadPool.execute(() -> {
			//消息传递正确
			if(message.getInfo().equals("ShutDown")) {
				//清理文件
				taskExecutor.cleanupFiles();
				client.sendShutdownACKToMaster(masterInfo);
				doneGate.countDown();
			}
		});
	}
}
