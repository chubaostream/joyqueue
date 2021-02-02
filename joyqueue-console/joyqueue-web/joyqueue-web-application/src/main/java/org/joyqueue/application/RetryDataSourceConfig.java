package org.joyqueue.application;

import org.joyqueue.exception.JoyQueueConfigException;
import org.joyqueue.server.retry.api.ConsoleMessageRetry;
import org.joyqueue.server.retry.console.DbConsoleMessageRetry;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wangxiaofei1 on 2018/12/5.
 */
@Configuration
public class RetryDataSourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(RetryDataSourceConfig.class);

    // 配置路径加名称
    private static final String configFile = "application.properties";

    private static PropertySupplier propertySupplier;

    static {
        Properties properties = new Properties();
        InputStream inputStream = RetryDataSourceConfig.class.getClassLoader().getResourceAsStream(configFile);
        if (null == inputStream) {
            throw new JoyQueueConfigException("cannot load application.properties.");
        }
        try {
            properties.load(inputStream);
            Map<String, Object> propertiesMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() != null && ((String) entry.getKey()).startsWith("retry")) {
                    propertiesMap.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            propertySupplier = new PropertySupplier.MapSupplier(propertiesMap);
        } catch (IOException e) {
            throw new JoyQueueConfigException("load and parse retry config error.", e);
        }

        logger.info("success init CacheRetryConfig.");
    }

    @Bean(value="consoleMessageRetry", destroyMethod="stop")
    public ConsoleMessageRetry retryService() {
        ConsoleMessageRetry consoleMessageRetry = new DbConsoleMessageRetry();
        try {
            consoleMessageRetry.setSupplier(propertySupplier);
            consoleMessageRetry.start();
        } catch (Throwable e) {
            logger.error("cacheDbConsoleMessageRetry.start error",e);
            return null;
        }
        return consoleMessageRetry;
    }
}
