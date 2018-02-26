package cc.litstar.utils;

import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import cc.litstar.node.NodeInfo;
import cc.litstar.node.WorkerInfo;

//借助SSH协议完成远程数据传输
public class SftpUtils {
	
	//获取远端一组数据文件，我们需要提供Worker节点的各项信息
	//同理：put可以将文件发送到远端
	public static boolean getnFileFromRemote(NodeInfo nodeInfo, List<String> srcPathList, String dstDir) {
		String username = nodeInfo.getUsername();
		String ipAddress = nodeInfo.getIpAddress();
		String password = nodeInfo.getPassword();
		
		JSch jsch = new JSch();
		try{
			//connect session
			Session session = jsch.getSession(username, ipAddress, 22);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");  
			session.connect();
			// sftp remotely  
		    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");  
		    channel.connect();  
		    // get ： 获取文件并存放
		    for(String srcPath : srcPathList) {
		    	channel.get(srcPath, dstDir);  
		    }
		    // close connection
		    channel.disconnect();
		    session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean putFileToRemote(NodeInfo nodeInfo, String srcPath, String dstPath) {
		String username = nodeInfo.getUsername();
		String ipAddress = nodeInfo.getIpAddress();
		String password = nodeInfo.getPassword();
		
		JSch jsch = new JSch();
		try{
			//connect session
			Session session = jsch.getSession(username, ipAddress, 22);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");  
			session.connect();
			// sftp remotely  
		    ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");  
		    channel.connect();  
		    // get ： 获取文件并存放
		    channel.put(srcPath, dstPath, ChannelSftp.OVERWRITE);  
		    // close connection
		    channel.disconnect();
		    session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//Jsch的使用方法，目的是拖文件
	//后续改成根据节点信息读取文件
	public static void main(String[] args) throws Exception {	
		WorkerInfo workerInfo = new WorkerInfo("10.108.101.220", 5001, "root", "1zbsy,10npjs");
		List<String> fileList = new ArrayList<>();
		fileList.add("./test.sh");
		getnFileFromRemote(workerInfo, fileList, "./");
		putFileToRemote(workerInfo, "./pom.xml", "./");
	}
}
