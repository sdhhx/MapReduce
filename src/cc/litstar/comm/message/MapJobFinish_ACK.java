package cc.litstar.comm.message;

import com.google.gson.Gson;

/**
 * @author hehaoxing
 * 回包，主要数据是包的类型
 * 如果isCorrect字段是false，则重发包
 */
public class MapJobFinish_ACK implements Message {
	//任务编号
	private int num;
	private boolean isCorrect;
	
	public MapJobFinish_ACK(int num, boolean isCorrect) {
		super();
		this.num = num;
		this.isCorrect = isCorrect;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
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
