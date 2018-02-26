package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * 如果isCorrect字段是false，则重发包
 */
public class ReduceJobDispatch_ACK implements Message {
	private String ipAddress;
	private boolean isCorrect;
	
	public ReduceJobDispatch_ACK(String ipAddress, boolean isCorrect) {
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
