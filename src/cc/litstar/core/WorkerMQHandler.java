package cc.litstar.core;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.comm.message.MapJobDispatch;
import cc.litstar.comm.message.Message;
import cc.litstar.comm.message.ReduceJobDispatch;
import cc.litstar.comm.message.Shutdown;

/**
 * @author hehaoxing
 * 监听事件，调用Worker的方法
 */
public class WorkerMQHandler implements Runnable {
	
	private Worker worker;
	private final static Logger logger = LoggerFactory.getLogger(WorkerMQHandler.class);

	public WorkerMQHandler(Worker worker) {
		super();
		this.worker = worker;
	}

	@Override
	public void run() {
		LinkedBlockingQueue<Message> messageQueue = worker.getMQ();
		try {
			while(!Thread.currentThread().isInterrupted()) {
				Message message = messageQueue.take();
				logger.info("Get Message: " + message.toString());
				if(message instanceof MapJobDispatch) {
					worker.receiveMapTaskFromMaster((MapJobDispatch)message);
				} else if(message instanceof ReduceJobDispatch) {
					worker.receiveReduceTaskFromMaster((ReduceJobDispatch)message);
				} else if(message instanceof Shutdown) {
					worker.receiveShutdownFromMaster((Shutdown)message);
					return;
				}
			}
		} catch (InterruptedException e) {
			//线程被中断
		} finally {
			;
		}
	}
	

}
