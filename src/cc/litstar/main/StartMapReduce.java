package cc.litstar.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.config.ConfReader;
import cc.litstar.core.Master;
import cc.litstar.core.Worker;
import cc.litstar.task.Task;

/**
 * @author hehaoxing
 * 用作整个程序的入口类，也方便用户编写程序
 */
public class StartMapReduce {
	
	private Task task;

	private final static Logger logger = LoggerFactory.getLogger(StartMapReduce.class);
	
	public StartMapReduce(Task task) {
		super();
		this.task = task;
	}

	public void runTask() throws Exception {
		if(task == null) {
			return;
		}
		String nodeType = ConfReader.getConf().getType();
		if(nodeType.equals("Master")) {
			logger.info("Run as master");
			Master master = new Master();
			master.setTask(task);
			master.startMaster();
		} else if(nodeType.equals("Worker")) {
			logger.info("Run as worker");
			Worker worker = new Worker();
			worker.setTask(task);
			worker.startWorker();
		} else {
			logger.info("Cannot get node type from configuraton file");
			return;
		}
		Thread.sleep(5000);
		System.exit(0);
	}
}
