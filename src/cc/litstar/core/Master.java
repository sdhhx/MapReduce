package cc.litstar.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.comm.CommClient;
import cc.litstar.comm.CommServer;
import cc.litstar.comm.message.MapJobFinish;
import cc.litstar.comm.message.Message;
import cc.litstar.comm.message.ReduceJobFinish;
import cc.litstar.comm.message.Register;
import cc.litstar.comm.message.Shutdown_ACK;
import cc.litstar.config.ConfReader;
import cc.litstar.config.Configuration;
import cc.litstar.node.MasterInfo;
import cc.litstar.node.WorkerInfo;
import cc.litstar.task.Task;
import cc.litstar.task.Job;
import cc.litstar.utils.NetworkUtils;
import cc.litstar.utils.SftpUtils;

/**
 * 实现Master节点的工作流程：
 * 		这里主线程处理信令，其他线程处理业务
 */
public class Master {
	//Master节点的运行模式：单机或分布式
	private String mode;
	//Master节点记录了节点自身信息(需要检验当前节点是否是Master)
	private MasterInfo masterInfo;
	//Master节点的状态
	private MasterStatus status;

	//上面的一些信息需要读取配置文件
	//Congfiguration需要填上本地节点的IP地址，以应对单节点多网卡的情况
	private Configuration config;
	
	//需要执行的任务，以及任务执行器(配置文件读取完成后才能执行任务)
	private Task task;
	private MapReduce taskExecutor;
	
	//注册的节点信息表，注册后写入表单(收到注册信息，做简单包装后写入(需要验证是否是配置文件中的Master))
	private List<WorkerInfo> workerList = new ArrayList<>();
	
	//Map任务的编号分发表
	private Map<Integer, Job> mapJobTable = new ConcurrentHashMap<>();
	//Map任务的完成情况，任务全部完成后开始执行Reduce任务
	private Map<Integer, Boolean> mapJobStatus = new ConcurrentHashMap<>();
	
	//Reduce任务的编号分发表
	private Map<Integer, Job> reduceJobTable = new ConcurrentHashMap<>();
	//Reduce任务的完成情况，任务全部完成后开始执行Merge任务
	private Map<Integer, Boolean> reduceJobStatus = new ConcurrentHashMap<>();
	
	//Master的消息队列
	private LinkedBlockingQueue<Message> MQ = null;
	
	//日志输出
	private final static Logger logger = LoggerFactory.getLogger(Master.class);
	
	public Master() {
		config = ConfReader.getConf();
		mode = config.getMode();
		masterInfo = getMasterInfo();
		MQ = MessageQueue.getMQ();
	}
	
	/**
	 * 功能：校验当前节点的配置文件与实际情况，并返回当前节点信息：
	 * 		配置文件中的LocalIP是本机IP地址，而且在配置文件的IP中有这一项
	 * 		端口也要一致(一个程序实际占用消息队列端口和22号SFTP端口)
	 */
	public MasterInfo getMasterInfo() {
		//读取本机IP地址列表，判断当前IP地址是否在列表中
		List<String> localIPList = NetworkUtils.getLocalIP();		
		String confMasterIP = config.getMaster().getIpAddress();
		int confMasterPort = config.getMaster().getPort();
		if(config.getLocalIP().equals(confMasterIP) && config.getLocalPort() == confMasterPort) {
			for(String localIP : localIPList) {
				if(localIP.equals(confMasterIP)) {
					return config.getMaster();
				}
			}
		}
		return null;
	}
	
	/**
	 * 设置Master节点任务信息
	 */
	public Master setTask(Task task) {
		this.task = task;
		this.taskExecutor = new MapReduce(task);
		return this;
	}
	
	/**
	 * 获取Master节点的消息队列
	 */
	LinkedBlockingQueue<Message> getMQ() {
		return MQ;
	}
	
	/***************************************以下为任务执行逻辑********************************************/
	//启动Netty客户端用于通信协调工作流程
	private CommServer server = null;
	private CommClient client = null;
	
	//闭锁：一种业务不能在必须在其准备工作完成后才能开始工作
	
	//注册闭锁，当配置文件中所有节点注册完成后通过
	private CountDownLatch registerDone = null;
	//Split任务闭锁，当Split任务结束后通过
	private CountDownLatch doSplitDone = null;
	//Map任务闭锁，当所有Worker节点完成Map任务后通过
	private CountDownLatch doMapDone = null;
	//Reduce任务闭锁，当所有Worker节点完成Reduce任务后通过
	private CountDownLatch doReduceDone = null;
	//Merge任务闭锁，当Master节点完成Merge任务后通过
	private CountDownLatch doMergeDone = null;
	//所有节点向Master节点回复Shutdown信息并清理退出，Master节点退出
	private CountDownLatch doneGate = null;
	
	//线程池，用于执行任务
	ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	
	/**
	 * 初始化闭锁，使得任务按顺序完成
	 */
	private void latchInit() {
		registerDone = new CountDownLatch(config.getWorkerList().size());
		doSplitDone = new CountDownLatch(1);
		doMapDone = new CountDownLatch(task.getnMap());
		doReduceDone = new CountDownLatch(task.getnReduce());
		doMergeDone = new CountDownLatch(1);
		doneGate = new CountDownLatch(config.getWorkerList().size());
	}
	
	/**
	 * 启动一个Master节点：
	 * 		1. 节点可以单机执行任务
	 * 		2. 节点可以调度Worker节点，执行分布式任务
	 */
	public boolean startMaster() throws InterruptedException {
		if(task == null) {//不执行空任务
			return false;
		}
		if(mode.equals("Single")) {	//执行单机任务
			try {
				return new SingleRun(task).runTask();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else if(mode.equals("Distributed")) {	//执行分布式任务，使用消息队列进行调度(状态转移编程)
			if(masterInfo == null) {
				logger.info("Master init fail, cannot found config in file.");
				return false;
			}
			//初始化：包括Netty的初始化，闭锁的初始化与消息监听的初始化
			status = MasterStatus.INIT;
			server = new CommServer(config.getLocalIP(), config.getLocalPort());
			client = new CommClient();
			server.startServer();
			latchInit();
			//启动线程，用于消息监听与通知
			Thread MQListener = new Thread(new MasterMQHandler(this));		
			MQListener.start();
			
			//1. 等待所有节点注册完成，同时等待split任务完成
			status = MasterStatus.SPLIT_AND_REGISTER;
			masterDoSplit();
			registerDone.await();
			logger.info("All worker node registered");
			doSplitDone.await();
			logger.info("Do Split Done");	
			
			//2. 调度一组Map任务的完成
			status = MasterStatus.DO_MAP;
			sendMapTaskToWorker();
			doMapDone.await();
			logger.info("Do Map Done");	
			
			//3. 调度一组Reduce任务的完成
			status = MasterStatus.DO_REDUCE;
			sendReduceTaskToWorker();
			doReduceDone.await();
			logger.info("Do Reduce Done");	
			
			//4. 所有任务完成，执行Merge任务
			status = MasterStatus.DO_MERGE;
			masterDoMerge();
			doMergeDone.await();
			logger.info("Do Merge Done");		
			
			//5. 向所有节点发送Shutdown信息，完成清理后退出
			status = MasterStatus.SHUTDOWN;
			sendShutdownToWorker();
			doneGate.await();
			taskExecutor.cleanupFiles();
			
			logger.info("All Down");
			MQListener.interrupt();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Master节点执行Split任务
	 */
	public void masterDoSplit() {
		threadPool.execute(() -> {
			taskExecutor.doSplit();
			doSplitDone.countDown();
		});
	}
	
	/**
	 * Master节点执行Merge任务：
	 * 		1. 从远端获取文件
	 * 		2. 执行Merge文件件
	 */
	public void masterDoMerge() {
		threadPool.execute(() -> {
			//从远端节点获取文件
			for(int i = 0; i < task.getnReduce(); i++) {
				WorkerInfo workerInfo = workerList.get(getReduceNodeNumber(i));
				Job reduceJob = reduceJobTable.get(i);
				List<String> src = new ArrayList<>();
				src.add(reduceJob.getReduceFile());
				String dst = taskExecutor.AbsMergeName(i);
				SftpUtils.getnFileFromRemote(workerInfo, src, dst);
				logger.info("Get file: " + src + " from worker " + getReduceNodeNumber(i));
			}
			//执行Merge任务
			try {
				taskExecutor.doMerge();
			} catch (IOException e) {
				e.printStackTrace();
			}
			doMergeDone.countDown();
		});
	}
	
	/**
	 * 将Map任务编号转换为workerList中节点编号
	 */
	public int getMapNodeNumber(int mapNumber) {
		return mapNumber % workerList.size();
	}
	
	/**
	 * 将Reduce任务编号转换为workerList中节点编号
	 */
	public int getReduceNodeNumber(int reduceNumber) {
		return reduceNumber % workerList.size();
	}
	
	/**
	 * 完成一次Worker节点的注册：
	 * 		1. 收到节点信息并通过配置文件检查其合理性
	 * 		2. 将注册信息添加至注册信息表中
	 */
	public void receiveRegisterFromWorker(Register message) {
		threadPool.execute(() -> {
			logger.info("Receive register information from master.");
			WorkerInfo worker = message.getWorkerInfo();
			for(WorkerInfo sworker : config.getWorkerList()) {
				//防止重复注册
				if(sworker.equals(worker) && !workerList.contains(worker)) {
					workerList.add(worker);
					registerDone.countDown();
				}
			}
		});
	}
	
	/**
	 * 将Map任务信息发送给Worker节点：
	 * 		1. 将任务数据发送至Worker节点
	 * 		2. 记录Map任务信息表
	 */
	public void sendMapTaskToWorker() {
		threadPool.execute(() -> {
			logger.info("Send map task to worker");
			for(int i = 0; i < task.getnMap(); i++) {
				WorkerInfo worker = workerList.get(getMapNodeNumber(i));
				Job jobStatus = new Job(getMapNodeNumber(i), i);
				client.sendMapJobToWorker(worker, i, masterInfo, taskExecutor.AbsMapName(i));
				mapJobTable.put(i, jobStatus);
				mapJobStatus.put(i, false);
			}
		});
	}
	
	/**
	 * 接收来自Worker节点的任务完成信息：
	 * 		解析数据包，将信息保存至Map任务信息表
	 */
	public void receiceMapFinishFromWorker(MapJobFinish message) {
		threadPool.execute(()-> {
			logger.info("Receice map task finished from worker");
			//Map任务编号，不接受重复报文
			int mapNumber = message.getMapNumber();
			if(mapJobStatus.get(mapNumber) == false) {
				Job mapJob = mapJobTable.get(mapNumber);
				//保存节点的远程文件名
				mapJob.setMapFilelist(message.getFilePathList());
				mapJobStatus.put(mapNumber, true);
				//标记Map任务已完成
				doMapDone.countDown();
			}
		});	
	}
	
	/**
	 * 将Map任务信息发送给Worker节点：
	 * 		1. 将任务数据发送至Worker节点
	 * 		2. 记录Map任务信息表
	 * 		任务执行时的约定：执行第i个Reduce需要每个Map任务产生的第i个文件
	 */
	public void sendReduceTaskToWorker() {
		threadPool.execute(() -> {
			logger.info("Send reduce task to worker");
			Map<Integer, Integer> workerMap = new HashMap<>();
			for(int i = 0; i < task.getnMap(); i++) {
				workerMap.put(i, getMapNodeNumber(i));
			}
			for(int i = 0; i < task.getnReduce(); i++) {
				WorkerInfo worker = workerList.get(getReduceNodeNumber(i));
				Job jobStatus = new Job(getReduceNodeNumber(i), i);
				Map<Integer, String> filePathMap = new HashMap<>();
				for(int j = 0; j < task.getnMap(); j++) {
					filePathMap.put(j, mapJobTable.get(j).getMapFilelist().get(i));
				}
				client.sendReduceJobToWorker(worker, i, this.workerList, workerMap, filePathMap);
				//存入节点信息
				reduceJobTable.put(i, jobStatus);
				reduceJobStatus.put(i, false);
			}
		});
	}

	/**
	 * 接收来自Worker节点的任务完成信息：
	 * 		解析数据包，将信息保存至Reduce任务信息表
	 */
	public void receiveReduceFinishFromWorker(ReduceJobFinish message) {
		threadPool.execute(() -> {
			logger.info("Receice reduce task finished from worker");
			//Reduce任务编号，不接收重复消息
			int reduceNumber = message.getReduceNumber();
			if(reduceJobStatus.get(reduceNumber) == false) {
				Job reduceJob = reduceJobTable.get(reduceNumber);
				//保存节点的远程文件名
				reduceJob.setReduceFile(message.getReduceFilePath());
				reduceJobStatus.put(reduceNumber, true);
				//标记Reduce任务已完成
				doReduceDone.countDown();
			}
		});
	}
	
	/**
	 * 为所有节点发送Shutdown命令，清理所有Worker节点
	 */
	public void sendShutdownToWorker() {
		threadPool.execute(() -> {
			for(int i = 0; i < workerList.size(); i++) {
				client.sendShutdownToWorker(workerList.get(i));
			}
		});
	}
	
	/**
	 * 所有节点回复Shutdown命令后，Master节点退出
	 */
	public void receiveShutdownACKFromMaster(Shutdown_ACK message) {
		threadPool.execute(() -> {
			if(message.isCorrect() == true) {
				doneGate.countDown();
			}
		});	
	}
}

