package cc.litstar.core;

/**
 * KeyValue类，MapReduce的基本数据类型
 */
public class KeyValue {
	private String key = "";
	private String value = "";
	
	public KeyValue() {
		super();
	}

	public KeyValue(String line) {
		super();
		if(line != null) {
			String[] lineSplit = line.split("\t");
			this.key = lineSplit[0];
			for(int i = 1; i < lineSplit.length; i++) {
				if(lineSplit[i] != null) {
					this.value += lineSplit[i];
				} 
				if(i != lineSplit.length - 1) {
					this.value += "\t";
				}
			}
		}
	}
	
	public KeyValue(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return key + "\t" + value;
	}

}
