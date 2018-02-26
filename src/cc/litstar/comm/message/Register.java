package cc.litstar.comm.message;

import com.google.gson.Gson;
import cc.litstar.node.WorkerInfo;

/**
 * @author hehaoxing
 * Worker节点启动后发送注册信息
 */
public class Register implements Message{
	//Worker发送自己的节点信息
	private WorkerInfo WorkerInfo;
	
	//注册信息内容：组装当前节点信息
	public Register(WorkerInfo workerInfo) {
		super();
		WorkerInfo = workerInfo;
	}
	
	public WorkerInfo getWorkerInfo() {
		return WorkerInfo;
	}
	
	public void setWorkerInfo(WorkerInfo workerInfo) {
		WorkerInfo = workerInfo;
	}

	@Override
	//借助消息类型与数据构造字符串
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
}
