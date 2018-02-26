package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * Master向所有节点发送关闭信息
 */
public class Shutdown implements Message {
	
	private String info ;
	
	public Shutdown() {
		this.info = "ShutDown";
	}
	
	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
