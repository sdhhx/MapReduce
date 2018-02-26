package cc.litstar.task;

import java.io.Serializable;
import cc.litstar.interf.Mapper;
import cc.litstar.interf.Reducer;

//创建一个任务的配置信息
public class Task implements Serializable {
	//Map任务的个数
	private int nMap;
	//Reduce任务的个数
	private int nReduce;
	//输入文件名
	private String filename;
	//Map任务接口
	private Class<? extends Mapper> mapper;
	//Reduce任务接口
	private Class<? extends Reducer> reducer;
	
	//使用getter和setter进行配置
	public Task() {
		super();
	}

	public int getnMap() {
		return nMap;
	}

	public Task setnMap(int nMap) {
		this.nMap = nMap;
		return this;
	}

	public int getnReduce() {
		return nReduce;
	}

	public Task setnReduce(int nReduce) {
		this.nReduce = nReduce;
		return this;
	}

	public String getFilename() {
		return filename;
	}

	public Task setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	public Class<? extends Mapper> getMapper() {
		return mapper;
	}

	public Task setMapper(Class<? extends Mapper> mapper) {
		this.mapper = mapper;
		return this;
	}

	public Class<? extends Reducer> getReducer() {
		return reducer;
	}

	public Task setReducer(Class<? extends Reducer> reducer) {
		this.reducer = reducer;
		return this;
	}

	@Override
	//不能构造Gson
	public String toString() {
		return "Task [nMap=" + nMap + ", nReduce=" + nReduce + ", filename=" + filename + ", mapper=" + mapper
				+ ", reducer=" + reducer + "]";
	}

}
