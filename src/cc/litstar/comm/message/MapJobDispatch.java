package cc.litstar.comm.message;

import com.google.gson.Gson;

import cc.litstar.node.MasterInfo;

/**
 * @author hehaoxing
 * 用于Master向Worker派发Map任务：
 * 包含Master的节点信息(用于SSH)，任务编号信息，所需远程文件目录信息
 */
public class MapJobDispatch implements Message {
	//Map任务编号
	private int mapNumber;
	//Master节点信息
	private MasterInfo masterInfo;
	//远程文件目录(注意发送绝对目录)
	private String filePath;
	
	public MapJobDispatch(int mapNumber, MasterInfo masterInfo, String filePath) {
		super();
		this.mapNumber = mapNumber;
		this.masterInfo = masterInfo;
		this.filePath = filePath;
	}
	
	public int getMapNumber() {
		return mapNumber;
	}

	public void setMapNumber(int mapNumber) {
		this.mapNumber = mapNumber;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public MasterInfo getMasterInfo() {
		return masterInfo;
	}
	
	public void setMasterInfo(MasterInfo masterInfo) {
		this.masterInfo = masterInfo;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
}
