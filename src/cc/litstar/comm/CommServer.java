package cc.litstar.comm;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Netty的Server，用于监听消息
 */
public class CommServer {
	//IP地址
	private String serverIP;
	//端口号
	private int port;
	//日志输出
	private final static Logger logger = LoggerFactory.getLogger(CommServer.class);
	
	public CommServer(String serverIP, int port) {
		this.serverIP = serverIP;
		this.port = port;
	}
	
	//启动Netty服务器
	public void startServer() {
		//代表绑定所有端口的socket，消息转发给worker
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		//代表服务器已经接受的连接
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();
		
		//启动器
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 128)
			//心跳保活
			.childOption(ChannelOption.SO_KEEPALIVE, true)
			//TCP低延迟
			.childOption(ChannelOption.TCP_NODELAY, true)
			//捆绑端口容错
			.childOption(ChannelOption.SO_REUSEADDR, true)
			//缓存区设置
			.childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
			/* 自动增加或减少反馈上预测的缓冲区大小。
			 * 容量动态调整的接收缓冲区分配器，它会根据之前Channel接收到的数据报大小进行计算，如果连续填充满接收缓冲区的可写空间，则动态扩展容量。
			 * 如果连续2次接收到的数据报都小于指定值，则收缩当前的容量，以节约内存。
			 * 参数有3个：预期缓冲区包含的下限，没有接收返回的初始缓存区大小，以及缓存区大小上限。
			 */
			//.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(64, 16384, 1024 * 1024))
			//固定大小的缓存区默认1024，这样耗内存但绝对不会爆掉
			.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65536))
			.childHandler(new ChannelInitializer<SocketChannel>() {	
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					//入站从头到尾，出站从尾到头
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new StringEncoder());
					pipeline.addLast(new StringDecoder());
					pipeline.addLast(new CommServerHandler());
				}
		});
		logger.info("Mapreduce server init sucessful");
		
		//等待服务器关闭
		//绑定端口，开始接受进来的连接
		ChannelFuture f = b.bind(serverIP, port);
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					Channel channel = future.channel();
					logger.info("Mapreduce server start sucessful");
					//等待服务器Socket关闭
					ChannelFuture closeFuture = f.channel().closeFuture();
					closeFuture.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							workerGroup.shutdownGracefully();
							bossGroup.shutdownGracefully();
							logger.info("Exit server");
						}
					});
				} else {
					logger.info("Mapreduce server start failed, retrying");
					future.channel().eventLoop().schedule(() -> startServer(), 3, TimeUnit.SECONDS);
				}
			}
		});		
	}
}
