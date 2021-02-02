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
package org.joyqueue.server.retry.db;

import org.joyqueue.datasource.DataSourceConfig;
import org.joyqueue.datasource.DataSourceFactory;
import org.joyqueue.domain.ConsumeRetry;
import org.joyqueue.domain.Partition;
import org.joyqueue.domain.TopicName;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.server.retry.api.MessageRetry;
import org.joyqueue.server.retry.api.RetryPolicyProvider;
import org.joyqueue.server.retry.db.config.DbRetryConfigKey;
import org.joyqueue.server.retry.model.RetryMessageModel;
import org.joyqueue.server.retry.model.RetryStatus;
import org.joyqueue.server.retry.util.RetryUtil;
import org.joyqueue.toolkit.config.PropertySupplier;
import org.joyqueue.toolkit.db.DaoUtil;
import org.joyqueue.toolkit.lang.Close;
import org.joyqueue.toolkit.retry.RetryPolicy;
import org.joyqueue.toolkit.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 数据库重试管理，负责存放重试数据到数据库中
 */
public class DBMessageRetry implements MessageRetry<Long> {

    protected static final Logger logger = LoggerFactory.getLogger(DBMessageRetry.class);
    protected static final String QUERY_SQL_NOID =
            "select id, business_id, topic, app, data, exception, send_time from message_retry where status = " + RetryStatus.RETRY_ING.getValue() +
                    " and topic = ? and app = ? and retry_time<=? limit ?, ?";
    protected static final String QUERY_COUNT_SQL =
            "select count(1) from message_retry where status = " + RetryStatus.RETRY_ING.getValue() + " and topic = ? and app = ?";
    protected static final String ERROR_UPDATE_SQL = "update message_retry set retry_time = ?, " +
            "retry_count = retry_count + 1, update_time = ?, status = ? where topic = ? and app = ? and id = ? and status = " + RetryStatus.RETRY_ING.getValue();
    protected static final String EXPIRE_UPDATE_SQL = "update message_retry set status = " + RetryStatus.RETRY_EXPIRE.getValue() + ", " +
            "update_time = ? where topic = ? and app = ? and id = ? and status = " + RetryStatus.RETRY_ING.getValue();
    protected static final String SUCCESS_UPDATE_SQL = "update message_retry set status = " + RetryStatus.RETRY_SUCCESS.getValue() + ", " +
            "retry_count = retry_count + 1, update_time = ? where topic = ? and app = ? and id = ? and status = " + RetryStatus.RETRY_ING.getValue();
    protected static final String CREATE_SQL = "insert into message_retry (message_id, business_id, topic, app, " +
            "send_time, expire_time, retry_time, retry_count, status, data, exception, create_time, " +
            "update_time) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    protected static final String GET_SQL =
            "select id, business_id, topic, app, data, exception, send_time from message_retry where status = " + RetryStatus.RETRY_ING.getValue() +
                    " and topic = ? and app = ? and retry_time <= ? and id = ?";
    protected static final String QUERY_ENTITY_SQL = "select id, create_time, retry_count from message_retry where id = ? and topic = ?";
    protected static final String QUERY_ID_RETRY_TIME_SQL =
            "select id, retry_time from message_retry where status = " + RetryStatus.RETRY_ING.getValue() +
                    " and topic = ? and app = ? and retry_time < NOW() limit ?";

    // 数据源
    protected DataSource writeDataSource;
    // read only data source
    protected DataSource readDataSource;

    // start flag
    protected boolean isStartFlag = false;
    // 重试策略
    protected RetryPolicyProvider retryPolicyProvider;
    protected DataSourceConfig writeDataSourceConfig = null;
    protected DataSourceConfig readDataSourceConfig = null;
    protected RetryPolicy retryPolicy = null;

    public DBMessageRetry() {
    }

    public DataSource getDataSource() {
        return writeDataSource;
    }

    /**
     * @return  read only data source
     **/

    public DataSource getReadDataSource() {
        return readDataSource;
    }


    @Override
    public void start() {
        this.writeDataSource = DataSourceFactory.build(writeDataSourceConfig);
        this.readDataSource = DataSourceFactory.build(readDataSourceConfig);
        isStartFlag = true;
        logger.info("db retry manager is started");
    }

    @Override
    public boolean isStarted() {
        return isStartFlag;
    }

    @Override
    public void stop() {
        if (writeDataSource != null) {
            Close.close(writeDataSource);
            writeDataSource = null;
        }
        if (readDataSource != null) {
            Close.close(readDataSource);
            readDataSource = null;
        }
        isStartFlag = false;
        logger.info("db retry manager is stopped");
    }

    @Override
    public void setRetryPolicyProvider(RetryPolicyProvider retryPolicyProvider) {
        this.retryPolicyProvider = retryPolicyProvider;
    }

    @Override
    public void addRetry(List<RetryMessageModel> retryMessageModelList) throws JoyQueueException {
        List<ConsumeRetry> consumeRetries = generateConsumeRetry(retryMessageModelList);
        insertConsumeRetry(consumeRetries);

    }

    /**
     * 生成消费重试实例
     *
     * @param retryMessageModelList
     * @return
     */
    public List<ConsumeRetry> generateConsumeRetry(List<RetryMessageModel> retryMessageModelList) throws JoyQueueException {
        List<ConsumeRetry> resultList = new LinkedList<>();
        for (RetryMessageModel retryMessageModel : retryMessageModelList) {
            ConsumeRetry consumeRetry = new ConsumeRetry();
            consumeRetry.setMessageId(RetryUtil.generateMessageId(retryMessageModel.getTopic(), retryMessageModel.getPartition(), retryMessageModel.getIndex(),retryMessageModel.getSendTime()));
            consumeRetry.setBusinessId(retryMessageModel.getBusinessId());
            consumeRetry.setTopic(retryMessageModel.getTopic());
            consumeRetry.setApp(retryMessageModel.getApp());
            consumeRetry.setSendTime(retryMessageModel.getSendTime());

            RetryPolicy retryPolicy = retryPolicyProvider.getPolicy(TopicName.parse(retryMessageModel.getTopic()), retryMessageModel.getApp());

            consumeRetry.setExpireTime(getExpireTime(retryPolicy, SystemClock.now()));
            consumeRetry.setRetryTime(getRetryTime(retryPolicy, SystemClock.now(), 1));
            consumeRetry.setRetryCount(0);
            consumeRetry.setData(retryMessageModel.getBrokerMessage());
            consumeRetry.setException(retryMessageModel.getException());
            consumeRetry.setCreateTime(SystemClock.now());
            consumeRetry.setUpdateTime(SystemClock.now());
            consumeRetry.setStatus(RetryStatus.RETRY_ING.getValue());

            resultList.add(consumeRetry);
        }

        return resultList;
    }

    /**
     * 插入重试消息到数据库
     *
     * @param messages
     * @return
     * @throws JoyQueueException
     */
    public Map<Long, ConsumeRetry> insertConsumeRetry(List<ConsumeRetry> messages) throws JoyQueueException {
        final Map<Long, ConsumeRetry> dbIdConsumeRetryMap = new HashMap<>();

        String topic = messages.get(0).getTopic();
        String app = messages.get(0).getApp();

        try {
            int count = DaoUtil.insert(writeDataSource, messages, CREATE_SQL,
                    new DaoUtil.InsertCallback<ConsumeRetry>() {

                        @Override
                        public void after(ResultSet resultSet, ConsumeRetry consumeRetry) throws Exception {
                            long dbId = resultSet.getLong(1);
                            dbIdConsumeRetryMap.put(dbId, consumeRetry);
                        }

                        @Override
                        public void before(final PreparedStatement statement, final ConsumeRetry consumeRetry) throws
                                Exception {
                            //message_id
                            statement.setString(1, consumeRetry.getMessageId());
                            //business_id
                            statement.setString(2, consumeRetry.getBusinessId());
                            if (consumeRetry.getBusinessId() != null && consumeRetry.getBusinessId().length() > 100) {
                                logger.error("businessId to long,topic:{},app:{},businessId:{}.", consumeRetry.getTopic(), consumeRetry.getApp(), consumeRetry.getBusinessId());
                            }
                            //topic
                            statement.setString(3, consumeRetry.getTopic());
                            //app
                            statement.setString(4, consumeRetry.getApp());
                            //send_time
                            statement.setTimestamp(5, new Timestamp(consumeRetry.getSendTime()));
                            //expire_time
                            statement.setTimestamp(6, new Timestamp(consumeRetry.getExpireTime()));
                            // retry_time 下一次重试时间
                            statement.setTimestamp(7, new Timestamp(consumeRetry.getRetryTime()));
                            //retry_count
                            statement.setInt(8, consumeRetry.getRetryCount()); // 重试次数初始化为0
                            //status
                            statement.setShort(9, consumeRetry.getStatus());
                            //data
                            statement.setBytes(10, consumeRetry.getData());
                            //exception
                            statement.setBytes(11, consumeRetry.getException());
                            //create_time
                            statement.setTimestamp(12, new Timestamp(consumeRetry.getCreateTime()));
                            //update_time
                            statement.setTimestamp(13, new Timestamp(consumeRetry.getUpdateTime()));
                        }
                    });
        } catch (Exception e) {
            logger.error("insertConsumeRetry error.", e);

            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR.getMessage() + ",topic:" + topic + ",app:" + app, e,
                    JoyQueueCode.CN_DB_ERROR.getCode());
        }
        return dbIdConsumeRetryMap;
    }


    /**
     * 获取重试消息的过期时间，默认30天过期；
     * 用户配置过期时间不能小于30 minutes,否则不生效
     *
     * @param retryPolicy
     * @param currentTime
     * @return
     */
    public long getExpireTime(RetryPolicy retryPolicy, long currentTime) {
        //如果 <= 0 表示无过期时间限制
        long expireTime = retryPolicy.getExpireTime() != null ? retryPolicy.getExpireTime() : 0;
        if (expireTime > 0) {
            expireTime = currentTime + expireTime;
        } else {
            // 30天后过期
            expireTime = currentTime + 30 * 24 * 3600 * 1000L;
        }
        if (expireTime < 1000) {
            expireTime = 1000;
        }
        return expireTime;
    }

    /**
     * 获取重试时间
     *
     * @param retryPolicy
     * @param currentTime
     * @param retryTimes
     * @return
     */
    private long getRetryTime(RetryPolicy retryPolicy, long currentTime, int retryTimes) {
        return retryPolicy.getTime(currentTime, retryTimes, currentTime);
    }

    @Override
    public void retrySuccess(String topic, String app, Long[] messageIds) throws JoyQueueException {
        if (topic == null || topic.isEmpty() || app == null || app
                .isEmpty() || messageIds == null || messageIds.length == 0) {
            return;
        }

        List<Long> idList = new ArrayList<>(messageIds.length);
        for (long messageId : messageIds) {
            if (messageId > 0) {
                idList.add(messageId);
            }
        }

        try {
            final Timestamp timestamp = new Timestamp(SystemClock.now());
            DaoUtil.update(writeDataSource, idList, SUCCESS_UPDATE_SQL, (DaoUtil.UpdateCallback<Long>) (statement, target) -> {
                //update_time
                statement.setTimestamp(1, timestamp);
                //topic
                statement.setString(2, topic);
                //app
                statement.setString(3, app);
                //id
                statement.setLong(4, target);
            });
        } catch (Exception e) {
            logger.error("retrySuccess error.", e);

            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR, e);
        }
    }

    @Override
    public void retryError(String topic, String app, Long[] messageIds) throws JoyQueueException {
        if (topic == null || topic.isEmpty() || app == null || app
                .isEmpty() || messageIds == null || messageIds.length == 0) {
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = writeDataSource.getConnection();
            connection.setAutoCommit(false);

            statement = connection.prepareStatement(ERROR_UPDATE_SQL);

            long now = SystemClock.now();
            Timestamp timestamp = new Timestamp(now);

            // 遍历重试消息ID
            for (long messageId : messageIds) {
                // 获取重试数据
                Long nextRetryTime = getNextRetryTime(messageId, topic, app);
                if (nextRetryTime != null) {
                    // 计算下次重试时间
                    if (nextRetryTime <= 0) {
                        // 过期了
                        statement.setTimestamp(1, timestamp);
                        statement.setTimestamp(2, timestamp);
                        statement.setInt(3, RetryStatus.RETRY_EXPIRE.getValue());
                    } else {
                        // 没有过期
                        statement.setTimestamp(1, new Timestamp(nextRetryTime));
                        statement.setTimestamp(2, timestamp);
                        statement.setInt(3, RetryStatus.RETRY_ING.getValue());
                    }
                    statement.setString(4, topic);
                    statement.setString(5, app);
                    statement.setLong(6, messageId);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            logger.error("retryError error.", e);

            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR, e);
        } finally {
            Close.close(connection, statement, null);
        }
    }

    /**
     * 用于重试失败，获取下一次重试时间
     *
     * @param id
     * @param topic
     * @return
     * @throws JoyQueueException
     */
    protected Long getNextRetryTime(long id, String topic, String app) throws JoyQueueException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Long nextRetryTime = null; // 下一次重试时间
        try {
            connection = writeDataSource.getConnection();
            statement = connection.prepareStatement(QUERY_ENTITY_SQL);

            if (id <= 0) {
                return null;
            }

            statement.setLong(1, id);
            statement.setString(2, topic);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                resultSet.getLong(1);  // 使用分区消息序号暂存主键ID
                Timestamp createTime = resultSet.getTimestamp(2);
                long startTime = createTime.getTime(); // 临时存放创建时间
                int retryCount = resultSet.getInt(3);

                nextRetryTime = retryPolicyProvider.getPolicy(TopicName.parse(topic), app).getTime(SystemClock.now(), retryCount, startTime);
            }
        } catch (Exception e) {
            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR, e);
        } finally {
            Close.close(connection, statement, resultSet);
        }

        return nextRetryTime;
    }

    @Override
    public void retryExpire(String topic, String app, Long[] messageIds) throws JoyQueueException {
        if (topic == null || topic.isEmpty() || app == null || app
                .isEmpty() || messageIds == null || messageIds.length == 0) {
            return;
        }

        List<Long> idList = new ArrayList<>(messageIds.length);
        for (long messageId : messageIds) {
            if (messageId > 0) {
                idList.add(messageId);
            }
        }

        try {
            DaoUtil.update(writeDataSource, idList, EXPIRE_UPDATE_SQL, (DaoUtil.UpdateCallback<Long>) (statement, target) -> {
                //update_time
                statement.setTimestamp(1, new Timestamp(SystemClock.now()));
                //topic
                statement.setString(2, topic);
                //app
                statement.setString(3, app);
                //id
                statement.setLong(4, target);
            });
        } catch (Exception e) {
            logger.error("retryExpire error", e);

            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR, e);
        }
    }

    @Override
    public List<RetryMessageModel> getRetry(final String topic, final String app, final short count,
                                            final long startIndex) throws JoyQueueException {
        if (topic == null || topic.isEmpty() || app == null || app.isEmpty() || count <= 0) {
            return new ArrayList<>(0);
        }
        final long nowTime = SystemClock.now();
        try {
            List<RetryMessageModel> list = DaoUtil.queryList(readDataSource, QUERY_SQL_NOID, new DaoUtil.QueryCallback<RetryMessageModel>() {
                @Override
                public RetryMessageModel map(final ResultSet rs) throws Exception {
                    RetryMessageModel message = new RetryMessageModel();

                    message.setIndex(rs.getInt(1));  // 使用分区消息序号暂存主键ID
                    message.setBusinessId(rs.getString(2));
                    message.setTopic(rs.getString(3));
                    message.setApp(rs.getString(4));
                    message.setPartition(Partition.RETRY_PARTITION_ID);
                    message.setBrokerMessage(rs.getBytes(5));
                    message.setException(rs.getBytes(6));
                    message.setSendTime(rs.getLong(7));

                    return message;
                }

                @Override
                public void before(final PreparedStatement statement) throws Exception {
                    statement.setString(1, topic);
                    statement.setString(2, app);
                    statement.setTimestamp(3, new Timestamp(nowTime));
                    statement.setLong(4, startIndex);
                    statement.setInt(5, count);
                }
            });
            return list;
        } catch (Exception e) {
            logger.error("getRetry error.", e);

            throw new JoyQueueException(String.format("%s topic:%s,app:%s,count:%d", JoyQueueCode.CN_DB_ERROR.getMessage(), topic, app, count), e, JoyQueueCode.CN_DB_ERROR.getCode());
        }
    }

    @Override
    public int countRetry(final String topic, final String app) throws JoyQueueException {
        if (topic == null || topic.isEmpty() || app == null || app.isEmpty()) {
            return 0;
        }
        long start = SystemClock.now();
        try {
            int count = DaoUtil.queryObject(readDataSource, QUERY_COUNT_SQL, new DaoUtil.QueryCallback<Integer>() {
                @Override
                public Integer map(final ResultSet rs) throws Exception {
                    return rs.getInt(1);
                }

                @Override
                public void before(final PreparedStatement statement) throws Exception {
                    statement.setString(1, topic);
                    statement.setString(2, app);
                }
            });
            return count;
        } catch (Exception e) {
            logger.error("countRetry error.", e);

            throw new JoyQueueException(JoyQueueCode.CN_DB_ERROR, e);
        } finally {
            if (logger.isDebugEnabled()) {
                long end = SystemClock.now();
                logger.debug("从数据库获取重试记录总数耗时统计,topic=" + topic + ",app=" + app + ",time=" + (end - start));
            }
        }
    }

    public RetryMessageModel getMessageById(final String topic, final String app, final long id) throws JoyQueueException {
        final long nowTime = SystemClock.now();
        try {
            RetryMessageModel retryMessageModel = DaoUtil.queryObject(readDataSource, GET_SQL, new DaoUtil.QueryCallback<RetryMessageModel>() {
                @Override
                public void before(PreparedStatement preparedStatement) throws Exception {
                    preparedStatement.setString(1, topic);
                    preparedStatement.setString(2, app);
                    preparedStatement.setTimestamp(3, new Timestamp(nowTime));
                    preparedStatement.setLong(4, id);
                }

                @Override
                public RetryMessageModel map(ResultSet rs) throws Exception {
                    RetryMessageModel message = new RetryMessageModel();

                    message.setIndex(rs.getInt(1));  // 使用分区消息序号暂存主键ID
                    message.setBusinessId(rs.getString(2));
                    message.setTopic(rs.getString(3));
                    message.setApp(rs.getString(4));
                    message.setPartition(Partition.RETRY_PARTITION_ID);
                    message.setBrokerMessage(rs.getBytes(5));
                    message.setException(rs.getBytes(6));
                    message.setSendTime(rs.getLong(7));

                    return message;
                }
            });
            return retryMessageModel;
        } catch (Exception e) {
            logger.error("getMessageById error", e);

            throw new JoyQueueException(String.format("%s topic:%s,app:%s,id:%d", JoyQueueCode.CN_DB_ERROR.getMessage(), topic, app, id), e, JoyQueueCode.CN_DB_ERROR.getCode());
        }
    }


    /**
     * 查询指定条数待重试消息Id和retry_time
     *
     * @param topic
     * @param app
     * @param count
     * @return
     * @throws JoyQueueException
     */
    public List<long[]> queryIdAndRetryTime(final String topic, final String app, final int count) throws JoyQueueException {
        try {
            List<long[]> dbIds = DaoUtil.queryList(readDataSource, QUERY_ID_RETRY_TIME_SQL, new DaoUtil.QueryCallback<long[]>() {

                @Override
                public void before(PreparedStatement preparedStatement) throws Exception {
                    preparedStatement.setString(1, topic);
                    preparedStatement.setString(2, app);
                    preparedStatement.setInt(3, count);
                }

                @Override
                public long[] map(ResultSet resultSet) throws Exception {
                    long id = resultSet.getLong(1);
                    long retryTime = resultSet.getTimestamp(2).getTime();
                    long[] rst = new long[]{id, retryTime};
                    return rst;
                }

            });
            return dbIds;
        } catch (Exception e) {
            throw new JoyQueueException(String.format("%s topic:%s,app:%s,count:%d", JoyQueueCode.CN_DB_ERROR.getMessage(), topic, app, count), e, JoyQueueCode.CN_DB_ERROR.getCode());
        }
    }
    @Override
    public void setSupplier(PropertySupplier supplier) {

        writeDataSourceConfig = new DataSourceConfig();
        writeDataSourceConfig.setDriver(supplier.getValue(DbRetryConfigKey.DRIVER));
        writeDataSourceConfig.setUrl(supplier.getValue(DbRetryConfigKey.WRITE_URL));
        writeDataSourceConfig.setUser(supplier.getValue(DbRetryConfigKey.WRITE_USER_NAME));
        writeDataSourceConfig.setPassword(supplier.getValue(DbRetryConfigKey.WRITE_PASSWORD));

        readDataSourceConfig = new DataSourceConfig();
        readDataSourceConfig.setDriver(supplier.getValue(DbRetryConfigKey.DRIVER));
        readDataSourceConfig.setUrl(supplier.getValue(DbRetryConfigKey.READ_URL));
        readDataSourceConfig.setUser(supplier.getValue(DbRetryConfigKey.READ_USER_NAME));
        readDataSourceConfig.setPassword(supplier.getValue(DbRetryConfigKey.READ_PASSWORD));

        retryPolicy = new RetryPolicy(supplier.getValue(DbRetryConfigKey.RETRY_DELAY), supplier.getValue(DbRetryConfigKey.MAX_RETRY_TIMES));
    }

}
