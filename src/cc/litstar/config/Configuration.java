package cc.litstar.config;

import java.util.List;

import com.google.gson.Gson;

import cc.litstar.node.MasterInfo;
import cc.litstar.node.WorkerInfo;

//借助Gson从配置文件中读取信息
public class Configuration {
	//MapReduce框架的运行模式
	private String Mode;
	//以Master运行还是以Slave运行
	private String Type;
	//手动设置当前节点IP(应对多网卡情况)
	private String LocalIP;
	//当前要使用的端口号
	private int LocalPort;
	//MapReduce框架的Master节点信息
	private MasterInfo Master;
	//MapReduce框架的Worker节点信息
	private List<WorkerInfo> WorkerList;

	public Configuration(String mode, String type, String localIP, int localPort, MasterInfo master,
			List<WorkerInfo> workerList) {
		super();
		Mode = mode;
		Type = type;
		LocalIP = localIP;
		LocalPort = localPort;
		Master = master;
		WorkerList = workerList;
	}

	public String getMode() {
		return Mode;
	}

	public void setMode(String mode) {
		Mode = mode;
	}

	public String getType() {
		return Type;
	}

	public void setType(String type) {
		Type = type;
	}

	public String getLocalIP() {
		return LocalIP;
	}

	public void setLocalIP(String localIP) {
		LocalIP = localIP;
	}

	public int getLocalPort() {
		return LocalPort;
	}

	public void setLocalPort(int localPort) {
		LocalPort = localPort;
	}

	public MasterInfo getMaster() {
		return Master;
	}

	public void setMaster(MasterInfo master) {
		Master = master;
	}

	public List<WorkerInfo> getWorkerList() {
		return WorkerList;
	}

	public void setWorkerList(List<WorkerInfo> workerList) {
		WorkerList = workerList;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
}
