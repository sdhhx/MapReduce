package cc.litstar.test;

import java.util.ArrayList;
import java.util.List;

import cc.litstar.core.KeyValue;
import cc.litstar.core.SingleRun;
import cc.litstar.interf.Mapper;
import cc.litstar.interf.Reducer;
import cc.litstar.task.Task;

/**
 * 词频统计(单机测试)
 * @author hehaoxing
 */
public class SingleTest {	
	public static class TestMapper implements Mapper{
		@Override
		public List<KeyValue> map(String line) throws Exception {
			List<KeyValue> output = new ArrayList<>();
			String[] resList = line.split("\\s+");
			for(String res: resList) {
				output.add(new KeyValue(res, "1"));
			}
			return output;
		}
	}
	
	public static class TestReducer implements Reducer{
		@Override
		public KeyValue reduce(String key, List<String> value) {
			int ans = 0;
			for(String v : value) {
				try {
					ans += Integer.parseInt(v);
				} catch (Exception e) {
					System.out.println(v);
				}
			}
			return new KeyValue(key + "\t" + ans);
		}
	}
	public static void main(String[] args) throws Exception {	
		Task task = new Task()
				.setnMap(10)
				.setnReduce(5)
				.setFilename("book.txt")
				.setMapper(SingleTest.TestMapper.class)
				.setReducer(SingleTest.TestReducer.class);
		new SingleRun(task).runTask();
	}
}
