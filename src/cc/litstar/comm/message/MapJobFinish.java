package cc.litstar.comm.message;

import java.util.List;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * Worker向Master节点上报任务已经完成：
 * 包含Worker的节点信息(用于SSH)忽略，任务编号信息，所需远程文件列表
 */
public class MapJobFinish implements Message {
	//Worker的节点信息在发布时即通过数据结构维护
	private int mapNumber;
	//远程文件信息(因为一个Map任务生成了Reduce个文件,完成一个发一个)
	private List<String> filePathList;
	
	public MapJobFinish(int mapNumber, List<String> filePathList) {
		super();
		this.mapNumber = mapNumber;
		this.filePathList = filePathList;
	}
	
	public int getMapNumber() {
		return mapNumber;
	}

	public void setMapNumber(int mapNumber) {
		this.mapNumber = mapNumber;
	}

	public List<String> getFilePathList() {
		return filePathList;
	}

	public void setFilePathList(List<String> filePathList) {
		this.filePathList = filePathList;
	}

	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
