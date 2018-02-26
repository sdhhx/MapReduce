package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * 判断是否需要重发
 * 合理的回包应该附带任务信息
 */
public class Register_ACK {
	private boolean isCorrect;
	
	public Register_ACK(boolean isCorrect) {
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
