/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.journalq.network.transport;

import com.jd.journalq.network.transport.command.Command;
import com.jd.journalq.network.transport.command.CommandCallback;
import com.jd.journalq.network.transport.exception.TransportException;

import java.net.SocketAddress;
import java.util.concurrent.Future;

/**
 * 通信
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/13
 */
public interface Transport {

    /**
     * 同步发送，需要应答
     *
     * @param command 命令
     * @return 应答命令
     * @throws TransportException
     */
    Command sync(Command command) throws TransportException;

    /**
     * 同步发送，需要应答
     *
     * @param command 命令
     * @param timeout 超时
     * @return 应答命令
     * @throws TransportException
     */
    Command sync(Command command, long timeout) throws TransportException;

    /**
     * 异步发送，需要应答
     *
     * @param command  命令
     * @param callback 回调
     * @throws TransportException
     */
    void async(Command command, CommandCallback callback) throws TransportException;

    /**
     * 异步发送，需要应答
     *
     * @param command  命令
     * @param timeout  超时
     * @param callback 回调
     * @throws TransportException
     */
    void async(Command command, long timeout, CommandCallback callback) throws TransportException;

    /**
     * 异步发送，需要应答
     *
     * @param command  命令
     * @throws TransportException
     */
    Future<?> async(Command command) throws TransportException;

    /**
     * 异步发送，需要应答
     *
     * @param command  命令
     * @param timeout  超时
     * @throws TransportException
     */
    Future<?> async(Command command, long timeout) throws TransportException;

    /**
     * 单向发送，不需要应答
     *
     * @param command 命令
     * @throws TransportException
     */
    void oneway(Command command) throws TransportException;

    /**
     * 单向发送，需要应答
     *
     * @param command 命令
     * @param timeout 超时
     * @throws TransportException
     */
    void oneway(Command command, long timeout) throws TransportException;

    /**
     * 应答
     *
     * @param request  请求
     * @param response 响应
     * @throws TransportException
     */
    void acknowledge(Command request, Command response) throws TransportException;

    /**
     * 应答
     *
     * @param request  请求
     * @param response 响应
     * @param callback 回调
     * @throws TransportException
     */
    void acknowledge(Command request, Command response, CommandCallback callback) throws TransportException;

    /**
     * 获取远端地址
     *
     * @return 远端地址
     */
    SocketAddress remoteAddress();

    /**
     * 属性
     * @return
     */
    TransportAttribute attr();

    /**
     * 设置属性
     * @return
     */
    void attr( TransportAttribute attribute);

    /**
     * 状态
     * @return
     */
    TransportState state();

    /**
     * 停止
     */
    void stop();
}