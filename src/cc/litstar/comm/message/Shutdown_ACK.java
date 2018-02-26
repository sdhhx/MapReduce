package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * Worker节点向Master节点回复Shutdown包
 */
public class Shutdown_ACK implements Message {
	private boolean isCorrect;
	
	public Shutdown_ACK(boolean isCorrect) {
		super();
		this.isCorrect = isCorrect;
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
