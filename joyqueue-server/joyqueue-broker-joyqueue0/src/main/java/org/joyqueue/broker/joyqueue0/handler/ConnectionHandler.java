/**
 * Copyright 2019 The JoyQueue Authors.
 *
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
package org.joyqueue.broker.joyqueue0.handler;

import org.joyqueue.broker.joyqueue0.Joyqueue0CommandHandler;
import org.joyqueue.broker.joyqueue0.Joyqueue0CommandType;
import org.joyqueue.broker.joyqueue0.command.AddConnection;
import org.joyqueue.broker.joyqueue0.command.BooleanAck;
import org.joyqueue.broker.joyqueue0.command.RemoveConnection;
import org.joyqueue.broker.joyqueue0.security.Joyqueue0Authentication;
import org.joyqueue.broker.BrokerContext;
import org.joyqueue.broker.BrokerContextAware;
import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.broker.helper.SessionHelper;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.message.SourceType;
import org.joyqueue.network.session.Connection;
import org.joyqueue.network.transport.Transport;
import org.joyqueue.network.transport.command.Command;
import org.joyqueue.network.transport.command.Types;
import org.joyqueue.network.transport.exception.TransportException;
import org.joyqueue.security.Authentication;
import org.joyqueue.toolkit.config.Property;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.network.IpUtil;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 连接处理
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/28
 */
public class ConnectionHandler implements Joyqueue0CommandHandler, Types, BrokerContextAware {
    public static final String JOYQUEUE0_ADMIN_USER = "broker.security.joyqueue0_admin_user";
    public static final String JOYQUEUE0_ADMIN_PASS = "broker.security.joyqueue0_admin_pass";

    protected static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private BrokerConfig brokerConfig;
    private SessionManager sessionManager;
    private Authentication authentication;

    @Override
    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerConfig = brokerContext.getBrokerConfig();
        this.sessionManager = brokerContext.getSessionManager();
        PropertySupplier propertySupplier = brokerContext.getPropertySupplier();
        Property jmq2User = propertySupplier.getOrCreateProperty(JOYQUEUE0_ADMIN_USER);
        Property jmq2Pass = propertySupplier.getOrCreateProperty(JOYQUEUE0_ADMIN_PASS);
        this.authentication = new Joyqueue0Authentication(brokerContext.getAuthentication(), jmq2User.getString(), jmq2Pass.getString());
    }

    @Override
    public Command handle(Transport transport, Command command) throws TransportException {
        Object payload = command.getPayload();

        if (payload instanceof AddConnection) {
            return addConnection(transport, command, (AddConnection) payload);
        } else if (payload instanceof RemoveConnection) {
            return removeConnection(transport, command, (RemoveConnection) payload);
        } else {
            throw new TransportException.RequestErrorException(JoyQueueCode.CN_COMMAND_UNSUPPORTED.getMessage(payload.getClass()));
        }
    }

    protected Command addConnection(Transport transport, Command request, AddConnection addConnection) {
        if (!authentication.auth(addConnection.getUser(), addConnection.getPassword()).isSuccess()) {
            logger.warn("user auth failed, transport: {}, user: {}, app: {}", transport, addConnection.getUser(), addConnection.getApp());
            return BooleanAck.build(JoyQueueCode.CN_AUTHENTICATION_ERROR.getCode(),
                    JoyQueueCode.CN_AUTHENTICATION_ERROR.getMessage() + String.format(", user: %s", addConnection.getUser()));
        }

        Connection connection = new Connection();
        connection.setTransport(transport);
        connection.setApp(addConnection.getApp());
        connection.setId(addConnection.getConnectionId().getConnectionId());
        connection.setLanguage(addConnection.getLanguage());
        connection.setSource(SourceType.JOYQUEUE0.name());
        connection.setVersion(addConnection.getClientVersion());
        connection.setAddressStr(IpUtil.toAddress(transport.remoteAddress()));
        connection.setHost(((InetSocketAddress) transport.remoteAddress()).getHostString());
        connection.setAddress(IpUtil.toByte((InetSocketAddress) transport.remoteAddress()));
        connection.setServerAddress(brokerConfig.getFrontendConfig().getHost().getBytes());
        connection.setCreateTime(SystemClock.now());
        connection.setAuth(true);

        if (!sessionManager.addConnection(connection)) {
            logger.warn("connection maybe already exists. app={} address={} id={}", connection.getApp(), transport, connection.getId());
        }

        // 绑定连接
        SessionHelper.setConnection(transport, connection);
        return BooleanAck.build();
    }

    protected Command removeConnection(Transport transport, Command request, RemoveConnection removeConnection) {
        sessionManager.removeConnection(removeConnection.getConnectionId().getConnectionId());
        return BooleanAck.build();
    }

    @Override
    public int[] types() {
        return new int[]{Joyqueue0CommandType.ADD_CONNECTION.getCode(), Joyqueue0CommandType.REMOVE_CONNECTION.getCode()};
    }
}