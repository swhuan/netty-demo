package org.yh.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * @desc http服务端相关的自定义业务逻辑处理类
 * @author yh
 * @date 2020.07.01
 */
public class HttpServerChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

   @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest rq) throws Exception {

        ctx.channel().remoteAddress();

        System.out.println("请求方法名称:" + rq.method().name());
        System.out.println("uri:" + rq.uri());
        ByteBuf buf = rq.content();
        System.out.print(buf.toString(CharsetUtil.UTF_8));

        ByteBuf byteBuf = Unpooled.copiedBuffer("hello world", CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
        ctx.writeAndFlush(response);
    }
}
