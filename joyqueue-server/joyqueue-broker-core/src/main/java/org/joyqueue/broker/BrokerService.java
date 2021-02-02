/**
 * Copyright 2019 The JoyQueue Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.broker;

import com.google.common.base.Preconditions;
import org.joyqueue.broker.archive.ArchiveManager;
import org.joyqueue.broker.cluster.ClusterManager;
import org.joyqueue.broker.cluster.ClusterNameService;
import org.joyqueue.broker.config.BrokerConfig;
import org.joyqueue.broker.config.BrokerStoreConfig;
import org.joyqueue.broker.config.Configuration;
import org.joyqueue.broker.config.ConfigurationManager;
import org.joyqueue.broker.config.scan.ClassScanner;
import org.joyqueue.broker.consumer.Consume;
import org.joyqueue.broker.consumer.MessageConvertSupport;
import org.joyqueue.broker.coordinator.CoordinatorService;
import org.joyqueue.broker.election.ElectionService;
import org.joyqueue.broker.event.BrokerEventBus;
import org.joyqueue.broker.extension.ExtensionManager;
import org.joyqueue.broker.helper.AwareHelper;
import org.joyqueue.broker.manage.BrokerManageService;
import org.joyqueue.broker.manage.config.BrokerManageConfig;
import org.joyqueue.broker.monitor.BrokerMonitorService;
import org.joyqueue.broker.monitor.SessionManager;
import org.joyqueue.broker.monitor.config.BrokerMonitorConfig;
import org.joyqueue.broker.network.BrokerServer;
import org.joyqueue.broker.network.protocol.ProtocolManager;
import org.joyqueue.broker.producer.Produce;
import org.joyqueue.broker.retry.BrokerRetryManager;
import org.joyqueue.broker.store.StoreInitializer;
import org.joyqueue.broker.store.StoreManager;
import org.joyqueue.domain.Config;
import org.joyqueue.domain.Consumer;
import org.joyqueue.domain.Producer;
import org.joyqueue.helper.PortHelper;
import org.joyqueue.network.transport.config.ServerConfig;
import org.joyqueue.network.transport.config.TransportConfigSupport;
import org.joyqueue.nsr.NameService;
import org.joyqueue.nsr.nameservice.CompensatedNameService;
import org.joyqueue.security.Authentication;
import org.joyqueue.server.retry.api.MessageRetry;
import org.joyqueue.store.StoreService;
import org.joyqueue.toolkit.config.Property;
import org.joyqueue.toolkit.config.PropertyDef;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.lang.LifeCycle;
import org.joyqueue.toolkit.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BrokerService
 */
public class BrokerService extends Service {
    private static final Logger logger = LoggerFactory.getLogger(BrokerService.class);
    private static final String NAMESERVICE_NAME = "nameserver.nsr.name";
    private static final String DEFAULT_NAMESERVICE_NAME = "server";
    private BrokerConfig brokerConfig;
    private SessionManager sessionManager;
    private BrokerMonitorService brokerMonitorService;
    private BrokerManageService brokerManageService;
    private Authentication authentication;
    private ProtocolManager protocolManager;
    private BrokerServer brokerServer;
    private ClusterNameService clusterNameService;
    private ClusterManager clusterManager;
    private Produce produce;
    private Consume consume;
    private StoreService storeService;
    private StoreInitializer storeInitializer;
    private ElectionService electionService;
    private MessageRetry retryManager;
    private BrokerContext brokerContext;
    private ConfigurationManager configurationManager;
    private StoreManager storeManager;
    private NameService nameService;
    private CoordinatorService coordinatorService;
    private ArchiveManager archiveManager;
    private MessageConvertSupport messageConvertSupport;
    private ExtensionManager extensionManager;
    private BrokerEventBus brokerEventBus;
    private String[] args;

    public BrokerService() {
    }

    public BrokerService(String[] args) {
        this.args = args;
    }

    @Override
    protected void validate() throws Exception {
        this.brokerContext = new BrokerContext();
        this.configurationManager = new ConfigurationManager(args);
        configurationManager.start();
        Configuration configuration = configurationManager.getConfiguration();
        enrichServicePorts(configuration);
        brokerContext.propertySupplier(configuration);

        this.brokerEventBus = new BrokerEventBus(brokerContext);
        this.brokerContext.eventBus(brokerEventBus);

        //build broker config
        this.brokerConfig = new BrokerConfig(configuration);
        String dataPath = brokerConfig.getAndCreateDataPath();
        logger.info("Broker data path: {}.", dataPath);

        this.brokerContext.brokerConfig(brokerConfig);
        this.extensionManager = new ExtensionManager(brokerContext);
        this.extensionManager.before();

        //start name service first
        this.nameService = getNameService(brokerContext, configuration);
        this.brokerContext.nameService(nameService);

        // build and start context manager
        this.nameService.addListener(configurationManager);
        this.configurationManager.setConfigProvider(new ConfigProviderImpl(nameService));

        //build and cluster manager
        this.clusterNameService = new ClusterNameService(nameService, brokerEventBus, configuration);
        this.clusterNameService.start();
        this.brokerContext.clusterNameService(clusterNameService);

        this.clusterManager = new ClusterManager(brokerConfig, nameService, clusterNameService, brokerContext);
        this.clusterManager.start();
        this.brokerContext.clusterManager(this.clusterManager);

        // build store service
        this.storeService = getStoreService(brokerContext);
        this.brokerContext.storeService(this.storeService);

        // build session manager
        this.sessionManager = new SessionManager();
        this.brokerContext.sessionManager(this.sessionManager);

        // build authentication
        this.authentication = getAuthentication(brokerContext);
        this.brokerContext.authentication(this.authentication);

        // new AppTokenAuthentication(clusterManager, brokerConfig.getJmqAdmin());
        this.brokerMonitorService = new BrokerMonitorService(clusterManager.getBrokerId(),
                new BrokerMonitorConfig(configuration, brokerConfig),
                sessionManager,
                clusterManager);
        this.brokerContext.brokerMonitorService(this.brokerMonitorService);

        // new coordinator service
        this.coordinatorService = new CoordinatorService(configuration, clusterManager, nameService);
        this.brokerContext.coordinnatorService(this.coordinatorService);

        this.messageConvertSupport = new MessageConvertSupport();
        this.brokerContext.messageConvertSupport(this.messageConvertSupport);

        // build produce
        this.produce = getProduce(brokerContext);
        this.brokerContext.produce(produce);

        // build message retry
        this.retryManager = getMessageRetry(brokerContext);
        if(null != this.retryManager) {
            this.retryManager.setSupplier(configuration);
        }
        this.brokerContext.retryManager(retryManager);

        // build consume
        this.archiveManager = new ArchiveManager(brokerContext);
        this.brokerContext.archiveManager(archiveManager);

        // build consume
        this.consume = getConsume(brokerContext);
        this.brokerContext.consume(consume);

        // build election
        this.electionService = getElectionService(brokerContext);
        this.brokerContext.electionService(electionService);

        this.storeInitializer = new StoreInitializer(new BrokerStoreConfig(configuration), nameService,
                clusterManager, storeService, electionService);

        // manage service
        this.brokerManageService = new BrokerManageService(new BrokerManageConfig(configuration,brokerConfig),
                brokerMonitorService,
                clusterManager,
                clusterNameService,
                storeService.getManageService(),
                storeService,
                consume,
                retryManager,
                coordinatorService,
                archiveManager,
                nameService,
                electionService,
                messageConvertSupport);
        this.brokerContext.brokerManageService(brokerManageService);

        //build store manager
        this.storeManager = new StoreManager(storeService, nameService, clusterManager, electionService);
        enrichIfNecessary(storeManager, brokerContext);
        //build protocol manager
        this.protocolManager = new ProtocolManager(brokerContext);
        //build broker server
        this.brokerServer = new BrokerServer(brokerContext, protocolManager);
        //build produce policy
        this.brokerContext.producerPolicy(buildGlobalProducePolicy(configuration));
        //build consume policy
        this.brokerContext.consumerPolicy(buildGlobalConsumePolicy(configuration));
        this.extensionManager.after();

        enrichConfiguration(configuration);
    }

    private void enrichServicePorts(Configuration configuration) {
        // broker.frontend-server.transport.server.port	50088	JoyQueue Server与客户端通信的端口
        String key = BrokerConfig.BROKER_FRONTEND_SERVER_CONFIG_PREFIX + TransportConfigSupport.TRANSPORT_SERVER_PORT;
        Property basePortProperty = configuration.getOrCreateProperty(key);
        int port = ServerConfig.DEFAULT_TRANSPORT_PORT;
        try {
                port = basePortProperty.getInteger();
        } catch (NullPointerException | NumberFormatException ignored) {}
        configuration.addProperty(key, String.valueOf(port));

        // broker.backend-server.transport.server.port	50089	内部端口，JoyQueue Server各节点之间通信的端口

        key = BrokerConfig.BROKER_BACKEND_SERVER_CONFIG_PREFIX + TransportConfigSupport.TRANSPORT_SERVER_PORT;
        configuration.addProperty(key, String.valueOf(PortHelper.getBackendPort(port)));
    }

    private void enrichConfiguration(Configuration configuration) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IOException {
        Map<String, String> configMap = getEnumConstantsConfig();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (!configuration.contains(entry.getKey())&&!entry.getKey().endsWith(".")) {
                configuration.addProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, String> getEnumConstantsConfig() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, IOException {
        Map<String, String> configMap = new HashMap<>(10);
        Set<Class<?>> classes = ClassScanner.defaultSearch();
        for (Class<?> clazz : classes) {
            List<Class<?>> impls = Arrays.asList(clazz.getInterfaces());
            if (impls.contains(PropertyDef.class) && clazz.isEnum()) {
                Method method = clazz.getMethod("values");
                if (method.getReturnType().isArray()) {
                    Object[] values = (Object[]) method.invoke(null);
                    for (Object obj : values) {
                        if (obj instanceof PropertyDef) {
                            PropertyDef propertyDef = (PropertyDef) obj;
                            configMap.put(propertyDef.getName(), String.valueOf(propertyDef.getValue()));
                        }
                    }
                }
            }
        }
        return configMap;
    }


    private NameService getNameService(BrokerContext brokerContext, Configuration configuration) {
        Property property = configuration.getProperty(NAMESERVICE_NAME);
        NameService nameService = Plugins.NAMESERVICE.get(property == null ? DEFAULT_NAMESERVICE_NAME : property.getString().trim());
        Preconditions.checkArgument(nameService != null, "nameService not found!");

        CompensatedNameService compensatedNameService = new CompensatedNameService(nameService);
        enrichIfNecessary(nameService, brokerContext);
        enrichIfNecessary(compensatedNameService, brokerContext);
        return compensatedNameService;
    }


    private StoreService getStoreService(BrokerContext brokerContext) {
        StoreService storeService = Plugins.STORE.get();
        Preconditions.checkArgument(storeService != null, "store service not found!");
        enrichIfNecessary(storeService, brokerContext);
        return storeService;
    }

    private Authentication getAuthentication(BrokerContext brokerContext) {
        Authentication authentication = Plugins.AUTHENTICATION.get();
        Preconditions.checkArgument(authentication != null, "authentication can  not be null");
        enrichIfNecessary(authentication, brokerContext);
        return authentication;
    }

    private Produce getProduce(BrokerContext brokerContext) {
        Produce produce = Plugins.PRODUCE.get();
        Preconditions.checkArgument(produce != null, "produce can not be null");
        enrichIfNecessary(produce, brokerContext);
        return produce;
    }

    private MessageRetry getMessageRetry(BrokerContext brokerContext) {
        //TODO 由于要动态调整重试方式，直接new 一个默认实现
        MessageRetry messageRetry = new BrokerRetryManager(brokerContext);
        return messageRetry;
    }

    private Consume getConsume(BrokerContext brokerContext) {
        Consume consume = Plugins.CONSUME.get();
        Preconditions.checkArgument(consume != null, "consume can not be null");
        enrichIfNecessary(consume, brokerContext);
        return consume;
    }

    private ElectionService getElectionService(BrokerContext brokerContext) {
        ElectionService electionService = Plugins.ELECTION.get();
        Preconditions.checkArgument(electionService != null, "election service can not be null");
        enrichIfNecessary(electionService, brokerContext);
        return electionService;
    }

    private Consumer.ConsumerPolicy buildGlobalConsumePolicy(PropertySupplier propertySupplier) {
        //TODO
        return new Consumer.ConsumerPolicy.Builder().create();
    }

    private Producer.ProducerPolicy buildGlobalProducePolicy(PropertySupplier propertySupplier) {
        //TODO
        return new Producer.ProducerPolicy.Builder().create();
    }

    @Override
    protected void doStart() throws Exception {
        startIfNecessary(brokerEventBus);
        startIfNecessary(clusterNameService);
        startIfNecessary(clusterManager);
        startIfNecessary(storeService);
        startIfNecessary(storeInitializer);
        startIfNecessary(sessionManager);
        startIfNecessary(retryManager);
        startIfNecessary(brokerMonitorService);
        startIfNecessary(produce);
        startIfNecessary(consume);
        //must start after store manager
        startIfNecessary(storeManager);
        startIfNecessary(electionService);
        startIfNecessary(extensionManager);
        startIfNecessary(protocolManager);
        startIfNecessary(nameService);
        startIfNecessary(archiveManager);
        startIfNecessary(brokerServer);
        startIfNecessary(coordinatorService);
        startIfNecessary(brokerManageService);
        printConfig();
    }


    private void printConfig() {
        StringBuilder buffer = new StringBuilder("broker start with configuration:").append('\n');
        if (configurationManager != null && configurationManager.getConfiguration() != null) {
            List<Property> properties = new ArrayList<>(configurationManager.getConfiguration().getProperties());
            Collections.sort(properties, Comparator.comparing(Property::getKey));
            for (Property property : properties) {
                String value = property.getValue() == null ? "null" : property.getValue().toString();
                buffer.append('\t').append(property.getKey()).append(": ").append(value).append('\n');
            }
        }

        logger.info(buffer.toString());
        logger.info("broker.id[{}],ip[{}],externalIp[{}],frontPort[{}],backendPort[{}],monitorPort[{}],nameServerManager port[{}]," +
                        "nameServer port[{}],messenger port[{}]",
                brokerConfig.getBrokerId(),
                clusterManager.getBroker().getIp(),
                clusterManager.getBroker().getExternalIp(),
                brokerConfig.getFrontendConfig().getPort(),
                brokerConfig.getBackendConfig().getPort(),
                brokerConfig.getBroker().getMonitorPort(),
                brokerConfig.getBroker().getNameServerManagerPort(),
                brokerConfig.getBroker().getNameServerPort(),
                brokerConfig.getBroker().getMessengerPort(),
                brokerConfig.getBroker().getJournalkeeperPort());
    }

    @Override
    protected void doStop() {

        destroy(brokerServer);
        destroy(protocolManager);
        destroy(extensionManager);
        destroy(electionService);
        destroy(produce);
        destroy(consume);
        destroy(coordinatorService);
        destroy(sessionManager);
        destroy(clusterManager);
        destroy(clusterNameService);
        destroy(storeManager);
        destroy(storeInitializer);
        destroy(storeService);
        destroy(configurationManager);
        destroy(retryManager);
        destroy(archiveManager);
        destroy(brokerMonitorService);
        destroy(brokerManageService);
        destroy(nameService);
        destroy(brokerEventBus);

        logger.info("Broker stopped!!!!");
    }

    public void enrichIfNecessary(Object obj, BrokerContext brokerContext) {
        AwareHelper.enrichIfNecessary(obj, brokerContext);
    }

    private void startIfNecessary(Object object) throws Exception {
        if (object instanceof LifeCycle) {
            if (!((LifeCycle) object).isStarted()) {
                ((LifeCycle) object).start();
            }
        }
    }

    private void destroy(Object object) {
        if (object == null) {
            return;
        }
        if (object instanceof LifeCycle) {
            Close.close((LifeCycle) object);
        }

        if (object instanceof Closeable) {
            Close.close((Closeable) object);
        }
    }


    public BrokerContext getBrokerContext() {
        return brokerContext;
    }


    private class ConfigProviderImpl implements ConfigurationManager.ConfigProvider {
        private NameService nameService;

        ConfigProviderImpl(NameService nameService) {
            this.nameService = nameService;
        }

        @Override
        public List<Config> getConfigs() {
            return nameService.getAllConfigs();
        }

        @Override
        public String getConfig(String group, String key) {
            return nameService.getConfig(group, key);
        }
    }
}