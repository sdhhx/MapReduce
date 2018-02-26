package cc.litstar.task;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hehaoxing
 * 记录一个任务的工作信息：
 * 	1. 记录节点在注册节点表中的节点信息
 * 	2. 记录此节点对应的任务编号
 *  3. 任务完成后，若为Map任务，记录其绝对路径表
 *  4. 任务完成后，若为Reduce任务，记录其绝对路径表
 *  
 *  Master维护两组Job列表，分别用于Map和Reduce
 */
public class Job {
	//注册节点表中的节点编号
	private int nodeNumber;
	//此任务的任务编号
	private int taskNumber;
	//Map任务生成的文件列表，这里约定第i项为第i个reduce任务所需的文件
	private List<String> mapFilelist;
	//Reduce任务生成的文件，Reduce只生成一个文件
	private String reduceFile;
	
	public Job(int nodeNumber, int taskNumber) {
		super();
		this.nodeNumber = nodeNumber;
		this.taskNumber = taskNumber;
		this.mapFilelist = new ArrayList<>();
		this.reduceFile = "";
	}

	public int getNodeNumber() {
		return nodeNumber;
	}

	public void setNodeNumber(int nodeNumber) {
		this.nodeNumber = nodeNumber;
	}

	public int getTaskNumber() {
		return taskNumber;
	}

	public void setTaskNumber(int taskNumber) {
		this.taskNumber = taskNumber;
	}

	public List<String> getMapFilelist() {
		return mapFilelist;
	}

	public void setMapFilelist(List<String> mapFilelist) {
		this.mapFilelist = mapFilelist;
	}

	public String getReduceFile() {
		return reduceFile;
	}

	public void setReduceFile(String reduceFile) {
		this.reduceFile = reduceFile;
	}

	@Override
	public String toString() {
		return "Job [nodeNumber=" + nodeNumber + ", taskNumber=" + taskNumber + ", mapFilelist=" + mapFilelist
				+ ", reduceFile=" + reduceFile + "]";
	}

}

