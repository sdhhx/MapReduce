package cc.litstar.comm.message;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import cc.litstar.node.WorkerInfo;

/**
 * @author hehaoxing
 * 用于Master向Worker派发Reduce任务：
 * 包含所有Map操作的节点信息(用于SSH)，任务编号信息，所需远程文件目录信息
 * 这里需要将数据压缩存储
 */
public class ReduceJobDispatch implements Message {
	//Reduce任务编号
	private int reduceNumber;
	//Master的注册文件列表
	private List<WorkerInfo> workerList;
	//Map编号与Worker节点的一一对应关系(单节点多Worker压缩报文)
	private Map<Integer, Integer> workerMap;
	//Map任务提供给对应Reduce任务的文件(按照约定，只有一个)
	private Map<Integer, String> mapFilePath;
	
	public ReduceJobDispatch(int reduceNumber, List<WorkerInfo> workerList, Map<Integer, Integer> workerMap,
			Map<Integer, String> mapFilePath) {
		super();
		this.reduceNumber = reduceNumber;
		this.workerList = workerList;
		this.workerMap = workerMap;
		this.mapFilePath = mapFilePath;
	}

	public int getReduceNumber() {
		return reduceNumber;
	}

	public void setReduceNumber(int reduceNumber) {
		this.reduceNumber = reduceNumber;
	}

	public List<WorkerInfo> getWorkerList() {
		return workerList;
	}

	public void setWorkerList(List<WorkerInfo> workerList) {
		this.workerList = workerList;
	}

	public Map<Integer, Integer> getWorkerMap() {
		return workerMap;
	}

	public void setWorkerMap(Map<Integer, Integer> workerMap) {
		this.workerMap = workerMap;
	}

	public Map<Integer, String> getMapFilePath() {
		return mapFilePath;
	}

	public void setMapFilePath(Map<Integer, String> mapFilePath) {
		this.mapFilePath = mapFilePath;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
