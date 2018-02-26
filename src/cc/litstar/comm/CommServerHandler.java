package cc.litstar.comm;

import cc.litstar.comm.message.Message;
import cc.litstar.comm.message.Packet;
import cc.litstar.core.MessageQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/** 
 * 处理接收到的消息：
 * 		1. 将消息写入消息队列以模块解耦
 * 		2. 借助json字符串代替具体编码，降低开发工作量
 */
public class CommServerHandler extends ChannelInboundHandlerAdapter {
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object pkt) {
		//收到的数据是String，且能够通过解析恢复成Message
		//则写入消息队列，通信模块不负责业余消息
		if(pkt instanceof String) {
			Message message = Packet.parseMessage((String)pkt);
			if(message != null && message instanceof Message) {
				MessageQueue.getMQ().offer(message);
			}
		}
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
