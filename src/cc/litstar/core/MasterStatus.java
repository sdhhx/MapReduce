package cc.litstar.core;

/**
 * @author hehaoxing
 * 这样分阶段是有问题的，后面再改
 */
public enum MasterStatus {

	/* Worker节点状态信息：*/
	INIT(1),							//正在初始化
	SPLIT_AND_REGISTER(2),				//正在分割初始文件
	DO_MAP(3),							//等待Worker节点完成Map任务
	DO_REDUCE(4),						//等待Worker节点完成Reduce任务
	DO_MERGE(5),						//正在执行Merge任务
	SHUTDOWN(6),						//完成整个流程后执行，通知Worker节点完成清理后并清理
	ERROR(7),							//工作遇到问题
	;
	
	private int code;
	private MasterStatus(int code) {
		this.code = code;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
}
