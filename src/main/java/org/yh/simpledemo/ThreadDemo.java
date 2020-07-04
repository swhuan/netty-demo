package org.yh.simpledemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.yh.handler.ChatServerHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @desc 线程模型演化对比(BIO->NIO->NETTY)
 * @author yh
 * @date 2020.07.02
 */
public class ThreadDemo {
    /**
     * 本地字符集
     */
    private static final String LocalCharSetName = "UTF-8";


    /**
     * 本地服务器监听的端口
     */
    private static final int Listenning_Port = 8888;


    /**
     * 缓冲区大小
     */
    private static final int Buffer_Size = 1024;


    /**
     * 超时时间,单位毫秒
     */
    private static final int TimeOut = 3000;


    /**
     * bio线程模型
     * 请求->server(accept()方法监听端口，阻塞等待请求)
     * ->当前线程内处理IO
     */
    public  void  bioV1Method(){
        try {
            ServerSocket server = new ServerSocket(8888);
            System.out.println("服务器已经启动！");
            // 接收客户端发送的信息
            Socket socket = server.accept();

            InputStream is = socket.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String info = null;
            while ((info = br.readLine()) != null) {
                System.out.println(info);
            }

            // 向客户端写入信息
            OutputStream os = socket.getOutputStream();
            String str = "欢迎登陆到server服务器!";
            os.write(str.getBytes());
            System.out.println("欢迎登陆到server服务器!");

            // 关闭文件流
            os.close();
            br.close();
            is.close();
            socket.close();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * bio线程模型优化版
     * 请求->server(accept()方法监听端口，阻塞等待请求)，委派工作后继续accept()
     * ->工作线程或线程池处理IO
     */
    public void bioV2Method() throws IOException {
        ServerSocket server = new ServerSocket(8888);
        System.out.println("服务器已经启动！");
        // 接收客户端发送的信息
        while(true){
            Socket socket = null;
            try {
                socket = server.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Socket finalSocket = socket;

            new Thread(() ->{
                try {
                    InputStream is = finalSocket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));

                    String info = null;
                    while ((info = br.readLine()) != null) {
                        System.out.println(info);
                    }
                    // 向客户端写入信息
                    OutputStream os = finalSocket.getOutputStream();
                    String str = "欢迎登陆到server服务器!";
                    os.write(str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * nio线程模型
     * 请求->Channel->Selector->ByteBuffer->IO操作
     */
    public void nioMethod() throws IOException {
        // 创建一个在本地端口进行监听的服务Socket信道.并设置为非阻塞方式
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(Listenning_Port));
        serverChannel.configureBlocking(false);
        //创建一个选择器并将serverChannel注册到它上面
        Selector selector = Selector.open();
        //设置为客户端请求连接时，默认客户端已经连接上
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            // 轮询监听key，select是阻塞的，accept()也是阻塞的
            if (selector.select(TimeOut) == 0) {
                System.out.println(".");
                continue;
            }
            // 有客户端请求，被轮询监听到
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                if (key.isAcceptable()) {
                    SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                    clientChannel.configureBlocking(false);
                    //意思是在通过Selector监听Channel时对读事件感兴趣
                    clientChannel.register(selector, SelectionKey.OP_READ,
                            ByteBuffer.allocate(Buffer_Size));
                }
                else if (key.isReadable()) {

                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    // 接下来是java缓冲区io操作，避免io堵塞
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    buffer.clear();
                    long bytesRead = clientChannel.read(buffer);
                    if (bytesRead == -1) {
                        // 没有读取到内容的情况
                        clientChannel.close();
                    } else {
                        // 将缓冲区准备为数据传出状态
                        buffer.flip();
                        // 将获得字节字符串(使用Charset进行解码)
                        String receivedString = Charset
                                .forName(LocalCharSetName).newDecoder().decode(buffer).toString();
                        System.out.println("接收到信息:" + receivedString);
                        String sendString = "你好,客户端. 已经收到你的信息" + receivedString;
                        buffer = ByteBuffer.wrap(sendString.getBytes(LocalCharSetName));
                        clientChannel.write(buffer);
                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
                }
                keyIter.remove();
            }
        }

    }


    /**
     * netty线程模型
     *使用Reactor模型
     * @throws InterruptedException
     */
    public void nettyMethod() throws InterruptedException {
        //主线程
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        //从线程
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).
                    //主线程监听通道
                    channel(NioServerSocketChannel.class)
                    //定义从线程的handler链，责任链模式
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LineBasedFrameDecoder(1024))
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                    .addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    .addLast(new ChatServerHandler());
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(8888).sync();
            future.channel().closeFuture().sync();
        }finally{
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ThreadDemo demo = new ThreadDemo();
        //bio
//        demo.bioV1Method();
        //bio优化版
//        demo.bioV2Method();
        //nio
//        demo.nioMethod();
        //netty
        demo.nettyMethod();
    }

}
