package org.yh.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.yh.initializer.HttpServerInitializer;

/**
 * @author yh
 * @desc http服务启动类
 * @date 2020.07.01
 */
public class HttpServer {
    public static void main(String[] args) {
        //构造两个线程组，默认线程数为 CPU 核心数乘以 2
        //bossGroup 用于接收客户端传过来的请求，接收到请求后将后续操作交由workerGroup处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //服务端启动辅助类,配置一些必要组件
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    //用于指定服务器端监听套接字通道 NioServerSocketChannel
                    .channel(NioServerSocketChannel.class)
                    //设置业务职责链
                    .childHandler(new HttpServerInitializer());
            //绑定端口
            ChannelFuture future = bootstrap.bind(8088).sync();
            //等待服务端口关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 退出并释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
