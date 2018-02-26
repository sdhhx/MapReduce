package cc.litstar.utils;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hehaoxing
 * 网络工具类
 */
public class NetworkUtils {
	
	//判断是否是IPv4地址
	public static boolean isIpv4(String ipAddress) {
		if(ipAddress == null) {
			return false;
		}
        String ip = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }
	
	//获取本地IP列表
	public static List<String> getLocalIP() {
		try {
			Enumeration<NetworkInterface> interfaces = null;
			interfaces = NetworkInterface.getNetworkInterfaces();
			List<String> localAddressList = new ArrayList<>();
			while (interfaces.hasMoreElements()) {  
				NetworkInterface ni = interfaces.nextElement();
				List<InterfaceAddress> addressList = ni.getInterfaceAddresses();
				for(InterfaceAddress address : addressList) {
					String hostAddress = address.getAddress().getHostAddress().trim();
					//int masklen = address.getNetworkPrefixLength();
					if(isIpv4(hostAddress)/* && !hostAddress.equals("127.0.0.1")*/) {
						//System.out.println(getGeneralGatewayIP(hostAddress, masklen));
						localAddressList.add(hostAddress);
					}
				}
			} 
			return localAddressList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//掩码位数转换为子网掩码
	public static String intToMask(int bitMask) {
		if(bitMask > 32)
			return null;
		int tmpMask[] = {0,0,0,0};
		int times = bitMask / 8;
		int i = 0;
		for(; i < times ; i++) {
			tmpMask[i] = 255;
		}
		for(int j = 1; j <= 8; j++) {
			if(j <= bitMask - times*8)
				tmpMask[i] = 2*tmpMask[i] + 1;
			else
				tmpMask[i] = 2*tmpMask[i];
		}
		return Integer.toString(tmpMask[0]) + "." + Integer.toString(tmpMask[1]) +  "." +  Integer.toString(tmpMask[2])  + "." + Integer.toString(tmpMask[3]);
	}

	//获取实践中常用的网关IP(子网网段+1)
	//不是标准，只是在实践中非常常用
	public static String getGeneralGatewayIP(String ipAddress, int bitMask) {
		String maskAddress = intToMask(bitMask);
		String[] ipFrame = ipAddress.split("\\.");
		String[] maskFrame = maskAddress.split("\\.");
		if(ipFrame.length == 4) {
			int f1 = Integer.parseInt(ipFrame[0]) & Integer.parseInt(maskFrame[0]);
			int f2 = Integer.parseInt(ipFrame[1]) & Integer.parseInt(maskFrame[1]);
			int f3 = Integer.parseInt(ipFrame[2]) & Integer.parseInt(maskFrame[2]);
			int f4 = (Integer.parseInt(ipFrame[3]) & Integer.parseInt(maskFrame[3])) + 1;
			return Integer.toString(f1) + "." + Integer.toString(f2) + "." + Integer.toString(f3) + "." + Integer.toString(f4);
		} else {
			return null;
		}
	}
	
	public static void main(String[] args) {
		getLocalIP();
	}
}
