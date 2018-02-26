package cc.litstar.comm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.litstar.comm.message.*;
import cc.litstar.node.MasterInfo;
import cc.litstar.node.WorkerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Netty的Client，用于发送消息
 */
public class CommClient {
	
	private final static Logger logger = LoggerFactory.getLogger(CommClient.class);
	
	/**
	 * 将当前节点的信息nodeInfo，发给Master节点(注册)
	 */
	public void sendRegisterToMaster(MasterInfo masterInfo, WorkerInfo workerInfo) {
		String MasterIP = masterInfo.getIpAddress();
		int MasterPort = masterInfo.getPort();
		Packet pkt = new Packet(MessageType.WORKER_REGISTER, new Register(workerInfo));
		sendMessage(MasterIP, MasterPort, pkt);
	}
	
	/**
	 * Master在节点注册完成后，将Map任务分配给Worker节点
	 */
	public void sendMapJobToWorker(WorkerInfo workerInfo, int mapJob, MasterInfo masterInfo, String filePath) {
		String WorkerIP = workerInfo.getIpAddress();
		int WorkerPort = workerInfo.getPort();
		Packet pkt = new Packet(MessageType.MASTER_MAPJOB_DISPATCH, new MapJobDispatch(mapJob, masterInfo, filePath));
		sendMessage(WorkerIP, WorkerPort, pkt);
	}
	
	/**
	 * Worker将执行完成的任务信息发送给Master节点
	 */
	public void sendMapJobFinishToMaster(MasterInfo masterInfo, int mapJob, List<String> filePathList) {
		String MasterIP = masterInfo.getIpAddress();
		int MasterPort = masterInfo.getPort();
		Packet pkt = new Packet(MessageType.WORKER_MAPJOB_FINISH, new MapJobFinish(mapJob, filePathList));
		sendMessage(MasterIP, MasterPort, pkt);
	}
	
	/**
	 * Master在全部任务完成后，将Reduce任务分配给Worker节点
	 */
	public void sendReduceJobToWorker(WorkerInfo workerInfo, int reduceJob, List<WorkerInfo> workerList, 
			Map<Integer, Integer> workerMap, Map<Integer, String> mapFilePath) {
		String WorkerIP = workerInfo.getIpAddress();
		int WorkerPort = workerInfo.getPort();
		Packet pkt = new Packet(MessageType.MASTER_REDUCEJOB_DISPATCH, 
				new ReduceJobDispatch(reduceJob, workerList, workerMap, mapFilePath));
		sendMessage(WorkerIP, WorkerPort, pkt);
	}
	
	/**
	 * Worker将执行完成的任务信息发送给Master节点()
	 */
	public void sendReduceJobFinishToMaster(MasterInfo masterInfo, int reduceJob, String filePath) {
		String MasterIP = masterInfo.getIpAddress();
		int MasterPort = masterInfo.getPort();
		Packet pkt = new Packet(MessageType.WORKER_REDUCEJOB_FINISH, new ReduceJobFinish(reduceJob, filePath));
		sendMessage(MasterIP, MasterPort, pkt);
	}
	
	/**
	 * Master在完成了Merge任务后，发送Shutdown信息给Worker节点
	 */
	public void sendShutdownToWorker(WorkerInfo workerInfo) {
		String WorkerIP = workerInfo.getIpAddress();
		int WorkerPort = workerInfo.getPort();
		Packet pkt = new Packet(MessageType.MASTER_SHUTDOWN, new Shutdown());
		sendMessage(WorkerIP, WorkerPort, pkt);
	}
	
	/**
	 * Worker在完成全部任务后，Master向Worker节点发送Shutdown信息
	 */
	public void sendShutdownACKToMaster(MasterInfo masterInfo) {
		String MasterIP = masterInfo.getIpAddress();
		int MasterPort = masterInfo.getPort();
		Packet pkt = new Packet(MessageType.MASTER_SHUTDOWN_ACK, new Shutdown_ACK(true));
		sendMessage(MasterIP, MasterPort, pkt);
	}
	
	public void sendMessage(String ipaddress, int port, Packet packet) {
		//维护一组连接
		EventLoopGroup group = new NioEventLoopGroup();
		//启动器
		Bootstrap bootstrap = new Bootstrap()
				.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.SO_REUSEADDR, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						//入站从头到尾，出站从尾到头
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new StringEncoder());
						pipeline.addLast(new StringDecoder());
						pipeline.addLast(new CommClientHandler(packet.toString()));
					}	
				});
		logger.info("Mapreduce client init sucessful");
		ChannelFuture channelFuture = bootstrap.connect(ipaddress, port);
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					logger.info("Starting sending messages");
					Channel channel = future.channel();
					channelFuture.channel().writeAndFlush(packet.toString());
				}else {
					logger.info("Sending messages encountered problems");
					future.channel().eventLoop().schedule(() -> sendMessage(ipaddress, port, packet), 3, TimeUnit.SECONDS);
				}
			}
		});
		//存疑
		//channelFuture.channel().closeFuture().sync();
	}

}
