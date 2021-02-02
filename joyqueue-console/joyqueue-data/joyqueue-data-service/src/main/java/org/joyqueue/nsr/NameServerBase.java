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
package org.joyqueue.nsr;

import com.alibaba.fastjson.JSON;
import org.joyqueue.model.domain.Identity;
import org.joyqueue.model.domain.OperLog;
import org.joyqueue.model.domain.User;
import org.joyqueue.service.OperLogService;
import org.joyqueue.toolkit.security.EscapeUtils;
import org.joyqueue.util.HttpUtil;
import org.joyqueue.util.LocalSession;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Date;

/**
 * Created by wangxiaofei1 on 2019/1/2.
 */
public class NameServerBase {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String SPLICE = ".";

    @Autowired
    private OperLogService operLogService;

    @Autowired
    private NsrServiceProvider nsrServiceProvider;
    /**
     * 带操作日志请求
     * @param uri
     * @param obj
     * @param type
     * @param identity
     * @return
     */
    public String postWithLog(String uri,Object obj,Integer type,Integer operType,String identity){
        OperLog operLog = null;
        String result = null;
        StringBuilder target= new StringBuilder();
        //组装数据
        try {
            //记录操作日志
            operLog = new OperLog();
            operLog.setType(type);
            operLog.setOperType(operType);
            target.append(nsrServiceProvider.getBaseUrl()+uri).append(",").append(JSON.toJSONString(obj));
            //执行请求
            result = post(uri,obj);
            //操作结果记录到数据库
            target.append(",").append(result);
        } catch (Exception e) {
            target.append(",").append(e.getMessage());
            logger.error("post exception",e);
            throw new RuntimeException("post exception",e);
        } finally {
            //执行记录日志
            try {
                //最长200
                String targetStr = target.toString();
                if (targetStr.length() > 500) {
                    targetStr = targetStr.substring(0,490);
                }
                if (identity.length() >=50) {
                    identity = identity.substring(0,49);
                }

                if (LocalSession.getSession() != null && LocalSession.getSession().getUser() != null){
                    User user = LocalSession.getSession().getUser();
                    operLog.setUpdateBy(new Identity(user.getId(), user.getCode()));
                    operLog.setCreateBy(new Identity(user.getId(), user.getCode()));
                } else {
                    operLog.setUpdateBy(new Identity(0L));
                    operLog.setCreateBy(new Identity(0L));
                }
                operLog.setCreateTime(new Date());

                operLog.setCreateTime(new Date());
                operLog.setIdentity(identity);
                operLog.setTarget(targetStr);
                operLogService.add(operLog);
            } catch (Exception e) {
                logger.error("operLogService add",e);
            }
        }
        return result;
    }

    public int isSuccess(String s){
        if (s.equals("success")) {
            return 1;
        }
        return 0;
    }

    /**
     * POST 公共方法
     * @param
     * @param uri
     * @param obj
     * @return
     * @throws Exception
     */
    public String post(String uri, Object obj) {
        HttpPost post = new HttpPost(nsrServiceProvider.getBaseUrl() + uri);
        if(null!=obj) {
            StringEntity entity = new StringEntity(JSON.toJSONString(obj), Charsets.UTF_8);//解决中文乱码问题
            entity.setContentEncoding(CharEncoding.UTF_8);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            post.setEntity(entity);
        }
        return HttpUtil.request(post);
    }

    @Deprecated
    private String onResponse(CloseableHttpResponse response, HttpUriRequest request) {
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != statusCode) {
                String message = String.format("monitorUrl [%s],reuqest[%s] error code [%s],response[%s]",
                        request.getURI().toString(),request.toString(),statusCode, EntityUtils.toString(response.getEntity()));
                throw new Exception(message);
            }
            String result =  EntityUtils.toString(response.getEntity());
            logger.info("request[{}] response[{}]",request.toString(),result);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }
    private String getEscapeTopic(String topic){
        return EscapeUtils.escape(topic,"/","^");
    };
}
