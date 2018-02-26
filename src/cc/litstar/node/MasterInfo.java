package cc.litstar.node;

//由于是Master节点，可能还有其他信息
//存放一些节点信息
public class MasterInfo extends NodeInfo{

	public MasterInfo(String ipAddress, int port, String user, String password) {
		super(ipAddress, port, user, password);
	}

	@Override
	public String toString() {
		return "MasterInfo [IpAddress=" + getIpAddress() + ", Port=" + getPort() + ", Username=" + getUsername()
				+ ", Password=" + getPassword() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj) && obj instanceof WorkerInfo) {
			return true;
		} else {
			return false;
		}
	}
}
