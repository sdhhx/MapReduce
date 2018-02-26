package cc.litstar.comm.message;

import com.google.gson.Gson;

import cc.litstar.node.WorkerInfo;

/**
 * @author hehaoxing
 * Master和Worker的心跳信息，用于更新Worker的状态
 * 由Worker节点主动发送(这个不是很着急，最后再实现)
 */
public class HeartBeat implements Message {
	//发送节点信息
	private WorkerInfo WorkerInfo;
	//发送是否存活
	private boolean isAlive;

	public HeartBeat(cc.litstar.node.WorkerInfo workerInfo, boolean isAlive) {
		super();
		WorkerInfo = workerInfo;
		this.isAlive = isAlive;
	}

	public WorkerInfo getWorkerInfo() {
		return WorkerInfo;
	}

	public void setWorkerInfo(WorkerInfo workerInfo) {
		WorkerInfo = workerInfo;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
