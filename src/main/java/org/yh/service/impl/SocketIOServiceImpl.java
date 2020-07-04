package org.yh.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yh.service.SocketIOService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @desc Netty-SocketIO 推送功能（与前端Socket.IO框架对接）
 * @author yh
 * @date 2020.07.02
 */
@Service
public class SocketIOServiceImpl implements SocketIOService {
    private static Logger logger = LoggerFactory.getLogger(SocketIOServiceImpl.class);

    /**
     * 推送的事件
     */
    public static final String PUSH_EVENT = "push_event";


    /**
     * 客户端发送的消息
     */
    public static final String GET_EVENT = "message";

    /**
     * 客户端发送消息的类型(心跳)
     */
    public static final String JUMP = "jump";

    /**
     * 已连接的客户端
     */
    private static Map<String, SocketIOClient> clientMap = new ConcurrentHashMap<>();

    @Autowired
    private SocketIOServer socketIOServer;


    /**
     * Spring IoC容器创建之后，在加载SocketIOServiceImpl Bean之后启动
     */
    @PostConstruct
    private void autoStartup() throws Exception {
        start();
    }

    /**
     * spring IoC容器在销毁SocketIOServiceImpl Bean之前关闭,避免重启项目服务端口占用问题
     */
    @PreDestroy
    private void autoStop() throws Exception {
        stop();
    }

    /**
     * 启动服务
     * @throws Exception
     */
    @Override
    public void start() {
        // 监听客户端连接
        socketIOServer.addConnectListener(client -> {
            String guid = getParamsByClient(client);
            if (guid != null) {
                clientMap.put(guid, client);
            }
        });

        // 监听客户端断开连接
        socketIOServer.addDisconnectListener(client -> {
            String guid = getParamsByClient(client);
            if (guid != null) {
                try {
                    clientMap.remove(guid);
                    client.disconnect();
                    logger.info("thread finish addConnectListener ====================> guid:{}", guid);
                } catch (Exception e) {
                    logger.error("addConnectListener break failed ==> guid:{}", guid);
                }
            }
        });
        // 客户端发送过来的消息
        socketIOServer.addEventListener(GET_EVENT, JSONObject.class, (client, data, ackSender) -> {
            String guid = getParamsByClient(client);
            logger.info("get message by websocket:" + data + "---------" + guid + "-----------" + client);
            if (null != guid) {
                String type = data.getString("type");
                if(JUMP.equals(type)){
                    //匹配心跳类型，给客户端发送消息
                    client.sendEvent(PUSH_EVENT, JSON.toJSONString("服务端原样返回:"+data.toJSONString()));
                }
            }
        });
        socketIOServer.start();
    }

    /**
     * 停止服务
     */
    @Override
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            socketIOServer = null;
        }
    }


    /**
     * @desc 获取client连接中的参数
     */
    private String getParamsByClient(SocketIOClient client) {
        //从请求的连接取参数
        Map<String, List<String>> params = client.getHandshakeData().getUrlParams();
        List<String> list = params.get("guid");
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

}
