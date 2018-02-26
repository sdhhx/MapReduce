package cc.litstar.core;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.comm.message.MapJobFinish;
import cc.litstar.comm.message.Message;
import cc.litstar.comm.message.ReduceJobFinish;
import cc.litstar.comm.message.Register;
import cc.litstar.comm.message.Shutdown_ACK;

/**
 * @author hehaoxing
 * 监听事件，然后调用Worker的方法
 */
public class MasterMQHandler implements Runnable{
	
	private Master master;
	private final static Logger logger = LoggerFactory.getLogger(MasterMQHandler.class);
	
	public MasterMQHandler(Master master) {
		super();
		this.master = master;
	}

	@Override
	public void run() {
		LinkedBlockingQueue<Message> messageQueue = master.getMQ();
		try {
			while(!Thread.currentThread().isInterrupted()) {
				//只用take()才能产生阻塞效果，poll不能
				Message message = messageQueue.take();
				logger.info("Get Message: " + message.toString());
				//调用master的方法，即完成一次远程方法调用
				if(message instanceof Register) {
					master.receiveRegisterFromWorker((Register)message);
				} else if(message instanceof MapJobFinish) {
					master.receiceMapFinishFromWorker((MapJobFinish)message);
				} else if(message instanceof ReduceJobFinish) {
					master.receiveReduceFinishFromWorker((ReduceJobFinish)message);
				} else if(message instanceof Shutdown_ACK) {
					master.receiveShutdownACKFromMaster((Shutdown_ACK)message);
				}
			}
		} catch (InterruptedException e) {
			//线程被中断
		} finally {
			;
		}
	}

}
