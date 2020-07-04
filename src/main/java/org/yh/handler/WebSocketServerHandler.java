package org.yh.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;

import java.time.LocalDateTime;

/**
 * @desc WebSocket服务端处理类
 * @author yh
 * @date 2020.07.01
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof FullHttpRequest){
            HandleHttpRequest(ctx,(FullHttpRequest)msg);
        }
        if(msg instanceof WebSocketFrame){
            handleWebSocket(ctx,(WebSocketFrame)msg);
        }
    }

    private void HandleHttpRequest(ChannelHandlerContext ctx,FullHttpRequest request){
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8888/websocket",null,false);
        handshaker = factory.newHandshaker(request);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else{
            handshaker.handshake(ctx.channel(),request);
        }
    }

    private void handleWebSocket(ChannelHandlerContext ctx,WebSocketFrame frame){

        if(frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(), (CloseWebSocketFrame)frame.retain());
        }

        if(frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if(!(frame instanceof TextWebSocketFrame)){
            return;
        }
        String requestStr = ((TextWebSocketFrame) frame).text();
        System.out.println(requestStr);
        ctx.channel().writeAndFlush(new TextWebSocketFrame("欢迎使用netty websocket： "+ LocalDateTime.now()));
    }



}