package cc.litstar.node;

//Worker节点的基本信息
//存放一些节点信息
public class WorkerInfo extends NodeInfo{

	public WorkerInfo(String ipAddress, int port, String user, String password) {
		super(ipAddress, port, user, password);
	}
	
	@Override
	public String toString() {
		return "WorkerInfo [IpAddress=" + getIpAddress() + ", Port=" + getPort() + ", Username=" + getUsername()
				+ ", Password=" + getPassword() + "]";
	}
	
	@Override
	//Worker与Master是不相等的
	public boolean equals(Object obj) {
		if(super.equals(obj) && obj instanceof WorkerInfo) {
			return true;
		} else {
			return false;
		}
	}
}
