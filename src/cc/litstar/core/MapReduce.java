package cc.litstar.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.interf.Mapper;
import cc.litstar.interf.Reducer;
import cc.litstar.task.Task;
import cc.litstar.utils.ExternalSort;

/** 
 * 本类主要实现MapReduce的整个处理流程：
 * 		1. 实现了MapReduce任务执行过程的各个环节
 * 		2. 所有文件路径：当前目录下resource文件夹 + 文件名
 */
public class MapReduce {
	//记录MapReduce的基本信息
	private Task task;
	//此变量为Reduce过程中，向后传递的缓存区大小
	private final static int MAX_LEN = 1000;
	//日志输出
	private final static Logger logger = LoggerFactory.getLogger(MapReduce.class);
	//指定工作目录：默认输出工作目录
	private static String workDir = "resource/";
	
	public MapReduce(Task task) {
		super();
		this.task = task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Task getTask() {
		return task;
	}
	
	public String getWorkDir() {
		return workDir;
	}

	/**
	 * 生成用于Map中间文件名
	 * 分割后的文件名为：mrtmp.filename-MapJob
	 */
	public String MapName(int MapJob) {
		return "mrtmp." + task.getFilename() + "-" + MapJob;
	}
	
	/**
	 * 用于Map任务的文件绝对路径
	 * 绝对路径是为了方便文件的读取与写入(读取对端绝对路径名，写入自己绝对路径名)
	 */
	public String AbsMapName(int MapJob) {
		return new File(workDir + MapName(MapJob)).getAbsolutePath();
	}
	
	/**
	 * 生成用于Reduce中间文件名
	 * 分割后的文件名为：mrtmp.filename-MapJob-ReduceJob
	 */
	public String ReduceName(int MapJob, int ReduceJob) {
		return MapName(MapJob) + "-" + ReduceJob;
	}
	
	/**
	 * 用于Reduce任务的文件绝对路径
	 */
	public String AbsReduceName(int MapJob, int ReduceJob) {
		return new File(workDir + ReduceName(MapJob, ReduceJob)).getAbsolutePath();
	}
	
	/**
	 * 生成用于Merge中间文件名
	 */
	public String MergeName(int ReduceJob) {
		return "mrtmp." + task.getFilename() + "-res-" + ReduceJob;
	}
	
	/**
	 * 用于Merge中间文件绝对路径
	 */
	public String AbsMergeName(int ReduceJob) {
		return new File(workDir + MergeName(ReduceJob)).getAbsolutePath();
	}
	
	/**
	 * MapReduce将输入文件分为nMap段
	 */
	public boolean doSplit() {
		//日志信息输出
		logger.info("Split " + task.getFilename());
		//打开文件，获取文件信息
		File inFile = new File(workDir + task.getFilename());
		//文件大小，以及文件分块方式
		long size, nchunk;
		size = inFile.length();
		nchunk = size / task.getnMap() + 1;		
		try {
			//创建输出文件，将文件分块后存入对应文件
			File outFile = new File(workDir + MapName(0));
			int trunkNum = 1, readlen = 0;
			
			//文件输入流与文件输出流
			BufferedReader inFileReader = new BufferedReader(new FileReader(inFile));
			BufferedWriter outFileWriter = new BufferedWriter(new FileWriter(outFile));
			String line = null;
			while((line = inFileReader.readLine()) != null) {
				if(readlen > (nchunk * trunkNum)) {
					outFileWriter.flush();
					outFileWriter.close();
					outFile = new File(workDir + MapName(trunkNum));
					outFileWriter = new BufferedWriter(new FileWriter(outFile));
					trunkNum++;
				}
				line += "\n";
				outFileWriter.write(line);
				readlen += line.length();
			}
			inFileReader.close();
			outFileWriter.flush();
			outFileWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} 
		return true;
	}
	
	/**
	 *  实现MapReduce的Map过程, 生成的中间文件用于Reduce操作
	 *  输出的中间文件用于下一步的Reduce操作
	 */
	public boolean doMap(int MapJob, Mapper mapper) {
		//打开对应的文件名
		File inFile = new File(workDir + MapName(MapJob));
		long size = inFile.length();
		logger.info("DoMap: read split " + inFile.getName() + ":" + size);
		
		//创建输出文件File数组
		int nReduce = task.getnReduce();
		File[] outFile = new File[nReduce];
		for(int i = 0; i < nReduce; i++) {
			outFile[i] = new File(workDir + ReduceName(MapJob, i));
		}
		try {
			BufferedReader inFileReader = new BufferedReader(new FileReader(inFile));
			BufferedWriter[] outFileWriter = new BufferedWriter[nReduce];
			for(int i = 0; i < nReduce; i++) {
				outFileWriter[i] = new BufferedWriter(new FileWriter(outFile[i]));
			}
			int select = -1;
			String line = null;
			while((line = inFileReader.readLine()) != null) {
				try {
					//不会因为一行执行失败而导致整个Map任务失败
					List<KeyValue> res = mapper.map(line);
					for(KeyValue kv : res) {
						//获取Key的哈希值，并写入对应的Map输出文件，用于Reduce操作
						select = Math.abs(kv.getKey().hashCode()) % nReduce;
						outFileWriter[select].write(kv.toString() + "\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			inFileReader.close();
			for(int i = 0; i < nReduce; i++) {
				outFileWriter[i].close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} 
		return true;
	}
	
	/**
	 * 读取Map生成的中间文件，对文件进行Reduce操作，生成用于Merge的结果
	 * 使用ExternalSort作为外部排序的实现
	 */
	public boolean doReduce(int ReduceJob, Reducer reducer) throws IOException {
		//读取文件，对文件进行外部排序
		//然后读取文件，遇到换行符或者与之前不同符号，或者长度超过链表
		//然后调用回调函数，完成任务执行
		//int nReduce = task.getnReduce();
		int nMap = task.getnMap();
		List<String> fileList = new LinkedList<>();
		for(int i = 0; i < nMap; i++) {
			fileList.add(workDir + ReduceName(i, ReduceJob));
			//输出结果为0?
			logger.info("DoReduce: read map " + ReduceName(i, ReduceJob) + ":" + new File(workDir + ReduceName(i, ReduceJob)).length());
		}
		String sortFilename = workDir + MergeName(ReduceJob) + "-original";
		//这么调用接口有一个问题，就是中间文件信息会加上这里传来的目录
		File sortFile = new ExternalSort(fileList, sortFilename).sort();
		//然后逐行读取文件，调用Reduce接口方法归并生成结果
		BufferedReader sortFileReader = new BufferedReader(new FileReader(sortFile));
		//使用上述文件生成结果，用于Merge函数调用
		File outFile = new File(workDir + MergeName(ReduceJob));
		BufferedWriter outFileWriter = new BufferedWriter(new FileWriter(outFile));
		//初始化变量
		String line = null; 
		String currentKey = null;
		List<String> currentValue = new LinkedList<>();
		KeyValue currentKeyValue = null;
		
		//读取外部排序文件，对各个文件分别调用Reduce
		while((line = sortFileReader.readLine()) != null) {
			if(currentKeyValue == null) {
				//第一个元素
				currentKeyValue = new KeyValue(line);
				currentKey = currentKeyValue.getKey();
				currentValue.add(currentKeyValue.getValue());
			} else {
				KeyValue temp = new KeyValue(line);
				if(temp.getKey().equals(currentKey) && currentValue.size() < MAX_LEN) {
					currentValue.add(temp.getValue());
				} else if (currentValue.size() >= MAX_LEN) {
					//缓存区写满后，清空链表继续Reduce(这里有个小问题，不能保证此种做法适用于大多数reduce操作)
					//实际上应该有Combiner，或许画蛇添足?
					try {
						KeyValue tmp = reducer.reduce(currentKey, currentValue);
						currentValue.clear();
						currentValue.add(tmp.getValue());
						currentValue.add(temp.getValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						//调用回调函数，结果写入文件
						KeyValue res = reducer.reduce(currentKey, currentValue);
						outFileWriter.write(res.toString() + "\n");
					} catch (Exception e) {
						e.printStackTrace();
					}
					//重置CurrentKey
					currentKeyValue = temp;
					currentKey = currentKeyValue.getKey();
					currentValue.clear();
					currentValue.add(currentKeyValue.getValue());
				}
			}
		}
		if(!currentValue.isEmpty()) {
			try {
				KeyValue res = reducer.reduce(currentKey, currentValue);
				outFileWriter.write(res.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sortFileReader.close();
		//排序文件为中间文件，清除
		sortFile.delete();
		outFileWriter.close();
		return true;
	}
	
	/**
	 * 	完成对Reduce生成文件的合并操作：
	 *  	由于doMap函数通过哈希值取余选择输出文件，简单进行外部排序即可
	 */
	public boolean doMerge() throws IOException {
		int nReduce = task.getnReduce();
		List<String> fileList = new LinkedList<>();
		for(int i = 0; i < nReduce; i++) {
			fileList.add(workDir + MergeName(i));
			logger.info("DoMerge: read reduce " + MergeName(i) + ":" + new File(workDir + MergeName(i)).length());
		}
		new ExternalSort(fileList, workDir + "mrtmp." + task.getFilename()).sort();
		return true;
	}
	
	/**
	 * 清理中间文件
	 */
	public boolean cleanupFiles() {
		cleanupSplitFiles();
		cleanupMapFiles();
		cleanupReduceFiles();
		return true;
	}
	
	/**
	 * 清理doSplit()过程的中间文件
	 */
	public boolean cleanupSplitFiles() {
		for(int i = 0; i < task.getnMap(); i++) {
			File file = new File(workDir + MapName(i));
			file.delete();
		}
		return true;
	}
	
	/**
	 * 清理doMerge()过程的中间文件
	 */
	public boolean cleanupMapFiles() {
		for(int i = 0; i < task.getnMap(); i++) {
			for(int j = 0; j < task.getnReduce(); j++) {
				File file = new File(workDir + ReduceName(i, j));
				file.delete();
			}
		}
		return true;
	}
	
	/**
	 * 清理doReduce()过程的中间文件
	 */
	public boolean cleanupReduceFiles() {
		for(int i = 0; i < task.getnReduce(); i++) {
			File file = new File(workDir + MergeName(i));
			file.delete();
		}
		return true;
	}
}

