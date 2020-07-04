package org.yh.service;


/**
 * @desc Netty-SocketIO 推送功能（与前端Socket.IO框架对接）
 * @author yh
 * @date 2020.07.02
 */
public interface SocketIOService {

    /**
     * 启动服务
     * @throws Exception
     */
    void start() throws Exception;

    /**
     * 停止服务
     */
    void stop();




}
