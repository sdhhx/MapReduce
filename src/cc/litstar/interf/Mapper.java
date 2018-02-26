package cc.litstar.interf;

import java.io.Serializable;
import java.util.List;

import cc.litstar.core.KeyValue;

/**
 * Map接口，需要实现Map方法
 */
public interface Mapper extends Serializable {
	//输入一行文本，返回一组keyValue对
	public List<KeyValue> map(String line) throws Exception;
}
