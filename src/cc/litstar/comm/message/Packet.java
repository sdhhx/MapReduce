package cc.litstar.comm.message;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * MapReduce通过在节点间传动报文，以实现根据消息调用方法
 * 		报文由两部分构成，消息类型与消息信息，二层json封装
 */

public class Packet implements Serializable {
	//消息类型
	private MessageType type;
	//消息数据，消息类json化
	private String data;
	
	public Packet(MessageType type, String data) {
		super();
		this.type = type;
		this.data = data;
	}
	
	public Packet(MessageType type, Message message) {
		super();
		this.type = type;
		this.data = message.toString();
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	//将自身转化为Gson
	public String toString() {
		Gson json = new Gson();
		return json.toJson(this);
	}
	
	/**
	 * 将消息字符串解析，返回消息数据
	 */
	public static Message parseMessage(String packet) {
		Gson gson = new Gson();
		//包装了消息类型的消息
		Packet pkt = gson.fromJson(packet, Packet.class);
		//消息字符串
		String data = pkt.getData();
		//Reflect消息类型，一个枚举项对应一个pojo
		Type type = null;
		//根据类别不同进行分别解析，写枚举不容易写错
		switch (pkt.getType()) {
			case HEART_HEAT:
				type = new TypeToken<HeartBeat>(){}.getType();
				break;
			case MASTER_MAPJOB_DISPATCH:
				type = new TypeToken<MapJobDispatch>(){}.getType();
				break;
			case MASTER_REDUCEJOB_DISPATCH:
				type = new TypeToken<ReduceJobDispatch>(){}.getType();
				break;
			case MASTER_SHUTDOWN:
				type = new TypeToken<Shutdown>(){}.getType();
				break;
			case WORKER_MAPJOB_FINISH:
				type = new TypeToken<MapJobFinish>(){}.getType();
				break;
			case WORKER_REDUCEJOB_FINISH:
				type = new TypeToken<ReduceJobFinish>(){}.getType();
				break;
			case WORKER_REGISTER:
				type = new TypeToken<Register>(){}.getType();
				break;
			case MASTER_MAPJOB_DISPATCH_ACK:
				type = new TypeToken<MapJobDispatch_ACK>(){}.getType();
				break;
			case MASTER_REDUCEJOB_DISPATCH_ACK:
				type = new TypeToken<ReduceJobDispatch_ACK>(){}.getType();
				break;
			case MASTER_SHUTDOWN_ACK:
				type = new TypeToken<Shutdown_ACK>(){}.getType();
				break;
			case WORKER_MAPJOB_FINISH_ACK:
				type = new TypeToken<MapJobFinish_ACK>(){}.getType();
				break;
			case WORKER_REDUCEJOB_FINISH_ACK:
				type = new TypeToken<ReduceJobFinish_ACK>(){}.getType();
				break;
			case WORKER_REGISTER_ACK:
				type = new TypeToken<Register_ACK>(){}.getType();
				break;
			default:
				return null;
		}
		return gson.fromJson(data, type);
	}
}
