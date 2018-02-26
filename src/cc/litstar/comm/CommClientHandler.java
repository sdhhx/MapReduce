package cc.litstar.comm;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 在此程序中没有起到作用
 */
//Client发送消息 -> Server接收消息 -> Server将消息写入通道 -> Client此类收到消息
public class CommClientHandler extends ChannelInboundHandlerAdapter {
	
	public CommClientHandler(String message) {
		super();
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
		System.out.println(msg.toString());
        ctx.write(msg);
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
