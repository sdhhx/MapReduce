package cc.litstar.core;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import cc.litstar.task.Task;

public class SingleRun {
	//传入Task
	private Task task;
	//MapReduce运算实现类
	private MapReduce mapReduce;
	//使用闭锁协调任务:await阻塞计数器不为0的节点
	private CountDownLatch startMapGate = new CountDownLatch(1);
	private CountDownLatch startReduceGate = new CountDownLatch(1);
	private CountDownLatch doneGate;
	
	public SingleRun(Task task) {
		super();
		this.task = task;
	}

	/**
	 * 启动一个单机MapReduce任务
	 */
	public boolean runTask() throws InterruptedException {
		mapReduce = new MapReduce(task);
		//1. 执行doSplit
		mapReduce.doSplit();
		
		//2. 执行doMap，初始化downGate后才能开始执行
		doneGate = new CountDownLatch(task.getnMap());
		startMapGate.countDown();
		for(int mapJob = 0; mapJob < task.getnMap(); mapJob++) {
			new Thread(new SingleMap(mapJob)).start();
		}
		doneGate.await();
		
		//3. 执行doReduce，使用栅栏调度，不使用线程池
		doneGate = new CountDownLatch(task.getnReduce());
		startReduceGate.countDown();
		for(int reduceJob = 0; reduceJob < task.getnReduce(); reduceJob++) {
			new Thread(new SingleReduce(reduceJob)).start();
		}
		doneGate.await();
		
		//4. 执行doMerge，完成操作
		try {
			mapReduce.doMerge();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		//5. 清理生成的中间文件
		mapReduce.cleanupFiles();
		return true;
	}
	
	class SingleMap implements Runnable {
		private int mapJob;
		public SingleMap(int mapJob) {
			super();
			this.mapJob = mapJob;
		}
		@Override
		public void run() {
			try {
				startMapGate.await();
				mapReduce.doMap(mapJob, task.getMapper().newInstance());
				doneGate.countDown();
			} catch (InstantiationException | IllegalAccessException | InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
	
	class SingleReduce implements Runnable {
		private int reduceJob;
		public SingleReduce(int reduceJob) {
			super();
			this.reduceJob = reduceJob;
		}
		@Override
		public void run() {
			try {
				startReduceGate.await();
				mapReduce.doReduce(reduceJob, task.getReducer().newInstance());
				doneGate.countDown();
			} catch (InstantiationException | IllegalAccessException | IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}	
	}		
	
}
