MapReduce
================================

@Author: hehaoxing

这是一个简单的MapReduce计算框架，支持单机与分布式计算。参照了Google MapReduce论文加以简单实现。

##工作流程：
	
1. Master进程启动并加载Worker节点的配置信息，并加载当前任务信息，完成初始化。
2. Worker进程启动并完成初始化后，向Master节点发送注册信息。Master依照Map任务的个数，将文件划分为nMap个。
3. Master节点向Worker节点传送Map任务信息，Worker节点收到信息后，主动获取Master节点上完成切分的文件。在对执行Map任务时，首先Worker按照用户自定义的逻辑完成Map操作，然后计算键的Hash值，写入 Hash%nReduce 文件。生成的中间文件共nMap * nReduce个，将用于接下来的Reduce操作。完成Map任务后，Worker节点向Master节点发送任务信息。当所有Map任务完成后，开始进行Reduce任务。
4. Master节点向Worker节点传送Reduce任务信息，Worker节点收到信息后，主动获取Master节点上完成Map操作的中间文件。Worker按照用户自定义的逻辑完成Map操作。在执行Reduce任务时，首先对获取的文件进行外部排序，然后按照用户自定义的逻辑完成Reduce操作，单个键值操作完成后直接将结果写入文件。生成的中间文件共nReduce个。完成Map任务后，Worker节点向Master节点发送任务信息。当所有Reduce任务完成后，开始对中间文件进行合并。
5. Master主动获取Worker节点上完成Reduce操作的中间文件。通过外部排序算法将文件合并，此文件即MapReduce操作的输出文件。
6. Master向Worker节点发送Shutdown信息。所有节点对中间文件进行清理后，任务完成。


注1: 以上为分布式MapReduce任务执行流程，单机执行不需要传送消息，流程与分布式执行流程相似。
注2: 第i个任务分配的Worker节点编号为i % nMap或者i % n Reduce。其中nMap与nReduce为Map或Reduce任务的个数。
注3: Worker节点仅为执行Map与Reduce任务的容器，无操作顺序的限制。
注4: 经过测试，本程序在单机环境，以及接入同一个交换机的分布式环境下运行正常。

##使用方法

1. 首先需要编写自定义Map与Reduce操作逻辑。这里的接口设计与论文类似：
```
	//Map
	public interface Mapper extends Serializable {
		//输入一行文本，返回一组keyValue对
		public List<KeyValue> map(String line) throws Exception;
	}
	//Reduce
	public interface Reducer extends Serializable{
		//输入一组key-List(value)，返回一个key-value
		public KeyValue reduce(String key, List<String> value) throws Exception;
	}
```	
这里以词频统计为例，介绍及使用方式：
```
	//Map
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
	
	//Reduce
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
```

2. 编写配置文件，包含了Master节点与Worker节点的信息，以便发送信息及传送文件。在这里注意需要保证Master节点与Worker节点的网络彼此之间互联互通，需要配置防火墙放行彼此之间的22端口与监听端口。其配置文件编写方式如下：
```
	1) 以;字符开头行为注释信息。
	2) 填入的用户名密码为SSH登录的账号与密码，端口为监听端口。
	3) 单机式MapReduce任务不需要填入WorkerList字段，填入Type为Master。
	4) 配置文件置于jar包当前目录的conf目录下，文件名为config.json。
```
配置文件如下：	
```
		{
			；可选Distributed与Single
			"Mode" : "Distributed",
			;可选Master与Worker；单机执行填写Master
			"Type" : "Worker",
			;本机信息
			"LocalIP" : "127.0.0.1",
			"LocalPort" : 5003,
			;Master节点信息
			"Master" : {
				"ipAddress": "127.0.0.1",
				"port": 5000,
				"username": "root",
				"password": "123456789"
			},
			;Worker节点信息
			"WorkerList" : [
				{
					"ipAddress": "127.0.0.1",
					"port": 5001,
					"username": "root",
					"password": "123456789"
				},
				{
					"ipAddress": "127.0.0.1",
					"port": 5002,
					"username": "root",
					"password": "123456789"
				},
				{
					"ipAddress": "127.0.0.1",
					"port": 5003,
					"username": "root",
					"password": "123456789"
				},
				{
					"ipAddress": "127.0.0.1",
					"port": 5004,
					"username": "root",
					"password": "123456789"
				},
				{
					"ipAddress": "127.0.0.1",
					"port": 5005,
					"username": "root",
					"password": "123456789"
				}
			]
		}
```
3. 编写程序并执行。其入口类编写方法如下：
```
	public class MutiTest {
		//用户自定义Map函数
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
		//用户自定义Reduce函数
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
					.setnMap(10)								  //Map任务数量
					.setnReduce(5)								//Reduce任务数量
					.setFilename("book.txt")				  	//输入文件名,位于jar包当前目录的resource目录下
					.setMapper(MutiTest.TestMapper.class)		 //自定义Map
					.setReducer(MutiTest.TestReducer.class);	  //自定义Reduce
			new StartMapReduce(task).runTask();				   //启动任务
		}
	}
```

4. 在源文件resource目录下生成了最终文件，即mrtmp.源文件名。



