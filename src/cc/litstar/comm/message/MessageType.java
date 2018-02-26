package cc.litstar.comm.message;

/**
 * @author hehaoxing
 * 消息的类型
 */
public enum MessageType {
	/*
	 * 心跳信号是从节点发给主节点的
	 * 以Master开始代表由Master主动发送
	 * 以Worker开始代表由Worker主动发送
	 */
	
	//心跳信号
	HEART_HEAT(0),
	//常规信号
	MASTER_MAPJOB_DISPATCH(1),
	MASTER_REDUCEJOB_DISPATCH(2),
	MASTER_SHUTDOWN(3),
	
	WORKER_MAPJOB_FINISH(11),
	WORKER_REDUCEJOB_FINISH(12),
	WORKER_REGISTER(13),
	
	//确认ACK消息
	MASTER_MAPJOB_DISPATCH_ACK(21),
	MASTER_REDUCEJOB_DISPATCH_ACK(22),
	MASTER_SHUTDOWN_ACK(23),
	
	WORKER_MAPJOB_FINISH_ACK(31),
	WORKER_REDUCEJOB_FINISH_ACK(32),
	WORKER_REGISTER_ACK(33)
	;
	
	private Integer code;
	private MessageType(Integer code) {
		this.code = code;
	}

	public Integer getCode() {
		return code;
	}

	public String toString(){
		return String.valueOf(this.code);
	}
}
