package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * Worker节点上报Reduce任务完成
 * 包含Worker的节点信息(用于SSH)，任务编号信息，所需远程文件列表
 */
public class ReduceJobFinish implements Message {
	//Reduce任务的编号
	private int reduceNumber;
	//Reduce任务生成的文件目录信息，Master节点执行Merge的源文件
	private String reduceFilePath;
	
	public ReduceJobFinish(int reduceNumber, String reduceFilePath) {
		super();
		this.reduceNumber = reduceNumber;
		this.reduceFilePath = reduceFilePath;
	}

	public int getReduceNumber() {
		return reduceNumber;
	}

	public void setReduceNumber(int reduceNumber) {
		this.reduceNumber = reduceNumber;
	}

	public String getReduceFilePath() {
		return reduceFilePath;
	}

	public void setReduceFilePath(String reduceFilePath) {
		this.reduceFilePath = reduceFilePath;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
}
