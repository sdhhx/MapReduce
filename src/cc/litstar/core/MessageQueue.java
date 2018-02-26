package cc.litstar.core;

import java.util.concurrent.LinkedBlockingQueue;

import cc.litstar.comm.message.Message;

/**
 * @author hehaoxing
 * 
 * 利用消息队列将通信模块与业务模块解耦
 * 作为进程的消息队列，供其他模块使用，要求消息先进先出
 * 采用单例设计模式
 */
public class MessageQueue {
	private static LinkedBlockingQueue<Message> MQ = null;
	//空构造方法
	private MessageQueue() {}
	
	//获取消息的过程并不是线程安全的
	public static synchronized LinkedBlockingQueue<Message> getMQ() {
		if(MQ == null) {
			MQ = new LinkedBlockingQueue<>();
		}
		return MQ;
	}
}
