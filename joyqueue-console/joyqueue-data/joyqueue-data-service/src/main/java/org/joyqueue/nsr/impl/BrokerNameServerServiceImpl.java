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
package org.joyqueue.nsr.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.joyqueue.convert.NsrBrokerConverter;
import org.joyqueue.model.PageResult;
import org.joyqueue.model.QPageQuery;
import org.joyqueue.model.domain.Broker;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.domain.OperLog;
import org.joyqueue.model.query.QBroker;
import org.joyqueue.nsr.BrokerNameServerService;
import org.joyqueue.nsr.NameServerBase;
import org.joyqueue.nsr.model.BrokerQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by wangxiaofei1 on 2019/1/2.
 */
@Service("brokerNameServerService")
public class BrokerNameServerServiceImpl extends NameServerBase implements BrokerNameServerService {

    public static final String ADD_BROKER="/broker/add";
    public static final String REMOVE_BROKER="/broker/remove";
    public static final String UPDATE_BROKER="/broker/update";
    public static final String LIST_BROKER="/broker/list";
    public static final String GETBYID_BROKER="/broker/getById";
    public static final String GETBYIDS_BROKER="/broker/getByIds";
    public static final String SEARCH_BROKER="/broker/search";
    NsrBrokerConverter nsrBrokerConverter = new NsrBrokerConverter();

    @Override
    public Broker findById(Integer id) throws Exception {
        String result = post(GETBYID_BROKER,id);
        org.joyqueue.domain.Broker nsrBroker = JSON.parseObject(result, org.joyqueue.domain.Broker.class);
        return nsrBrokerConverter.revert(nsrBroker);
    }

    @Override
    public PageResult<Broker> search(QPageQuery<QBroker> query) {
        QPageQuery<BrokerQuery> queryQPageQuery = new QPageQuery<>();
        queryQPageQuery.setPagination(query.getPagination());
        queryQPageQuery.setQuery(brokerQueryConvert(query.getQuery()));
        try {
            PageResult<Broker> pageResult = new PageResult<>();
            String result = post(SEARCH_BROKER,queryQPageQuery);
            PageResult<org.joyqueue.domain.Broker> brokerPageResult = JSON.parseObject(result,new TypeReference<PageResult<org.joyqueue.domain.Broker>>(){});
            pageResult.setPagination(brokerPageResult.getPagination());
            pageResult.setResult(brokerPageResult.getResult().stream().map(broker -> nsrBrokerConverter.revert(broker)).collect(Collectors.toList()));
            return pageResult;
        } catch (Exception e) {
            logger.error("findByQuery exception",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加broker
     * @param broker
     * @throws Exception
     */
    @Override
    public int add(Broker broker) throws Exception {
        org.joyqueue.domain.Broker nsrBroker = nsrBrokerConverter.convert(broker);
        String result =  postWithLog(ADD_BROKER, nsrBroker,OperLog.Type.BROKER.value(),OperLog.OperType.ADD.value(),String.valueOf(broker.getId()));
        return isSuccess(result);
    }
    /**
     * 更新broker
     * @param broker
     * @throws Exception
     */
    @Override
    public int update(Broker broker) throws Exception {
        String result = post(GETBYID_BROKER,broker.getId());
        org.joyqueue.domain.Broker nsrBroker = JSON.parseObject(result, org.joyqueue.domain.Broker.class);

        if(nsrBroker == null) {
            nsrBroker = new org.joyqueue.domain.Broker();
        }
        if (broker.getIp() != null) {
            nsrBroker.setIp(broker.getIp());
        }
        if (broker.getRetryType() != null) {
            nsrBroker.setRetryType(broker.getRetryType());
        }
        if (broker.getPort() > 0) {
            nsrBroker.setPort(broker.getPort());
        }
        if (broker.getPermission() != null) {
            nsrBroker.setPermission(org.joyqueue.domain.Broker.PermissionEnum.value(broker.getPermission()));
        }
        nsrBroker.setId(Long.valueOf(broker.getId()).intValue());
        nsrBroker.setExternalIp(broker.getExternalIp());
        nsrBroker.setExternalPort(broker.getExternalPort());
        nsrBroker.setNode(broker.getNode());
        //nsrBroker.setDataCenter(broker.getDataCenter().getCode());
        String result1 = postWithLog(UPDATE_BROKER, nsrBroker,OperLog.Type.BROKER.value(),OperLog.OperType.UPDATE.value(),String.valueOf(broker.getId()));
        return isSuccess(result1);
    }

    @Override
    public List<Broker> getByIdsBroker(List<Integer> ids) throws Exception {
        String result = post(GETBYIDS_BROKER,ids);

        List<org.joyqueue.domain.Broker> brokerList = JSON.parseArray(result, org.joyqueue.domain.Broker.class);
        if (brokerList == null || brokerList.size() <=0) return null;
        return brokerList.stream().map(broker -> nsrBrokerConverter.revert(broker)).collect(Collectors.toList());
    }

    @Override
    public List<Broker> syncBrokers() throws Exception {
        String json  = post(LIST_BROKER,null);
        List<Broker>  nsrBrokers = new ArrayList<>();
        JSONArray jsonArray = JSON.parseArray(json);
        Iterator<Object> iterator = jsonArray.iterator();
        while (iterator.hasNext()){
            JSONObject jsonObject = (JSONObject) iterator.next();
            Broker broker = new Broker();
            broker.setDataCenter(new Identity(0L,"UNKNOW",jsonObject.getString("dataCenter")));
            broker.setId(Long.parseLong(jsonObject.getString("id")));
            broker.setIp(jsonObject.getString("ip"));
            broker.setPort(Integer.parseInt(jsonObject.getString("port")));
            broker.setRetryType(jsonObject.getString("retryType"));
            broker.setExternalIp(jsonObject.getString("externalIp"));
            broker.setExternalPort(Integer.parseInt(jsonObject.getString("externalPort")));
            broker.setNode(jsonObject.getString("node"));
            nsrBrokers.add(broker);
        }
        return nsrBrokers;
    }
    /**
     * 删除broker
     * @param broker
     * @throws Exception
     */
    @Override
    public int delete(Broker broker) throws Exception {
        Broker nsrBroker = new Broker();
        nsrBroker.setIp(broker.getIp());
        nsrBroker.setRetryType(broker.getRetryType());
        nsrBroker.setPort(broker.getPort());
        nsrBroker.setId(Long.valueOf(broker.getId()).intValue());
        String result = postWithLog(REMOVE_BROKER, nsrBroker,OperLog.Type.BROKER.value(),OperLog.OperType.DELETE.value(),String.valueOf(broker.getId()));
        return isSuccess(result);
    }
    private BrokerQuery brokerQueryConvert(QBroker query){
        BrokerQuery brokerQuery = new BrokerQuery();
        if (query != null) {
            brokerQuery.setBrokerId(query.getBrokerId());
            brokerQuery.setIp(query.getIp());
            brokerQuery.setKeyword(query.getKeyword());
            brokerQuery.setBrokerList(query.getInBrokerIds());
            brokerQuery.setExternalIp(query.getExternalIp());
        }
        return brokerQuery;
    }
}
