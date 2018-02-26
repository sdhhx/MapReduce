package cc.litstar.interf;

import java.io.Serializable;
import java.util.List;

import cc.litstar.core.KeyValue;

/**
 * Reduce接口，需要实现reduce方法
 */
public interface Reducer extends Serializable{
	//输入一组key-List(value)，返回一个key-value
	public KeyValue reduce(String key, List<String> value) throws Exception;
}
