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
package io.chubao.joyqueue.client.internal.transport;

import io.chubao.joyqueue.client.internal.exception.ClientException;
import io.chubao.joyqueue.client.internal.nameserver.NameServerConfig;
import io.chubao.joyqueue.client.internal.nameserver.NameServerConfigChecker;
import io.chubao.joyqueue.client.internal.transport.config.TransportConfig;
import io.chubao.joyqueue.client.internal.transport.config.TransportConfigChecker;
import io.chubao.joyqueue.exception.JoyQueueCode;
import io.chubao.joyqueue.network.domain.BrokerNode;
import io.chubao.joyqueue.network.transport.TransportClient;
import io.chubao.joyqueue.network.transport.codec.support.JoyQueueCodec;
import io.chubao.joyqueue.network.transport.config.ClientConfig;
import io.chubao.joyqueue.network.transport.support.DefaultTransportClientFactory;
import io.chubao.joyqueue.toolkit.concurrent.NamedThreadFactory;
import io.chubao.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ClientManager
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/11/28
 */
// TODO 引用处理
public class ClientManager extends Service {

    protected static final Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private TransportConfig transportConfig;
    private NameServerConfig nameServerConfig;
    private TransportClient transportClient;
    private ClientGroupManager clientGroupManager;
    private ScheduledExecutorService heartbeatThreadScheduler;

    public ClientManager(TransportConfig transportConfig, NameServerConfig nameServerConfig) {
        TransportConfigChecker.check(transportConfig);
        NameServerConfigChecker.check(nameServerConfig);

        this.transportConfig = transportConfig;
        this.nameServerConfig = nameServerConfig;
    }

    @Override
    protected void validate() throws Exception {
        clientGroupManager = new ClientGroupManager(transportConfig);
        transportClient = new DefaultTransportClientFactory(new JoyQueueCodec()).create(convertToClientConfig(transportConfig));
        heartbeatThreadScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("joyqueue-client-heartbeat"));
    }

    @Override
    protected void doStart() throws Exception {
        clientGroupManager.start();
        heartbeatThreadScheduler.scheduleWithFixedDelay(new ClientHeartbeatThread(transportConfig, clientGroupManager),
                transportConfig.getHeartbeatInterval(), transportConfig.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doStop() {
        if (heartbeatThreadScheduler != null) {
            heartbeatThreadScheduler.shutdown();
        }
        if (clientGroupManager != null) {
            clientGroupManager.stop();
        }
        if (transportClient != null) {
            transportClient.stop();
        }
    }

    public ClientGroup getClientGroup(BrokerNode node) {
        checkState();
        return clientGroupManager.getClientGroup(node);
    }

    public Client createClient(BrokerNode node) {
        checkState();
        try {
            Client client = new Client(node, transportConfig, transportClient, nameServerConfig);
            client.start();
            client.handleAddConnection();
            return client;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public Client getOrCreateClient(BrokerNode node) {
        checkState();
        Client client = doGetOrCreateClientGroup(node).getClient();
        client.handleAddConnection();
        return client;
    }

    public Client doGetOrCreateClient(BrokerNode node) {
        checkState();
        return doGetOrCreateClientGroup(node).getClient();
    }

    protected ClientGroup doGetOrCreateClientGroup(final BrokerNode node) {
        try {
            return clientGroupManager.getClientGroup(node, new Callable<ClientGroup>() {
                @Override
                public ClientGroup call() throws Exception {
                    ClientGroup clientGroup = new ClientGroup(node, transportConfig, transportClient, nameServerConfig);
                    clientGroup.start();
                    return clientGroup;
                }
            });
        } catch (Exception e) {
            logger.debug("get client exception, node: {}", node, e);
            throw new ClientException(e);
        }
    }

    public Client tryGetClient(BrokerNode node) {
        checkState();
        ClientGroup clientGroup = clientGroupManager.tryGetClientGroup(node);
        if (clientGroup == null) {
            return null;
        }
        return clientGroup.tryGetClient();
    }

    public Client getClient(BrokerNode node) {
        checkState();
        ClientGroup clientGroup = clientGroupManager.getClientGroup(node);
        if (clientGroup == null) {
            return null;
        }
        Client client = clientGroup.getClient();
        client.handleAddConnection();
        return client;
    }

    public void closeClient(BrokerNode node) {
        checkState();
        ClientGroup clientGroup = clientGroupManager.getClientGroup(node);
        if (clientGroup == null) {
            return;
        }
        for (Client client : clientGroup.getClients()) {
            client.handleDisconnection();
        }
        clientGroupManager.closeClientGroup(clientGroup);
    }

    protected void checkState() {
        if (!isStarted()) {
            throw new ClientException("clientManager is not started", JoyQueueCode.CN_SERVICE_NOT_AVAILABLE.getCode());
        }
    }

    protected ClientConfig convertToClientConfig(TransportConfig transportConfig) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setSendTimeout(transportConfig.getSendTimeout());
        clientConfig.setIoThread(transportConfig.getIoThreads());
        clientConfig.setCallbackThreads(transportConfig.getCallbackThreads());
        clientConfig.setSoLinger(transportConfig.getSoLinger());
        clientConfig.setTcpNoDelay(transportConfig.isTcpNoDelay());
        clientConfig.setKeepAlive(transportConfig.isKeepAlive());
        clientConfig.setSoTimeout(transportConfig.getSoTimeout());
        clientConfig.setSocketBufferSize(transportConfig.getSocketBufferSize());
        clientConfig.setMaxOneway(transportConfig.getMaxOneway());
        clientConfig.setMaxAsync(transportConfig.getMaxAsync());
        clientConfig.setRetryPolicy(transportConfig.getRetryPolicy());
        clientConfig.setNonBlockOneway(transportConfig.isNonBlockOneway());
        clientConfig.setIoThreadName("joyqueue-client-io-eventLoop");
        return clientConfig;
    }
}