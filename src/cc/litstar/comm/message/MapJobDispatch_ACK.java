package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * 如果isCorrect字段是false，则重发包(一般是因为字段解析不出来)
 */
public class MapJobDispatch_ACK implements Message {
	//Worker的节点IP，
	private String ipAddress;
	private boolean isCorrect;
	
	public MapJobDispatch_ACK(String ipAddress, boolean isCorrect) {
		super();
		this.ipAddress = ipAddress;
		this.isCorrect = isCorrect;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public void setCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}
	
	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
