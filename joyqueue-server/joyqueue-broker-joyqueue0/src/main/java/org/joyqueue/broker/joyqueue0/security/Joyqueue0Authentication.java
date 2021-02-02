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
package org.joyqueue.broker.joyqueue0.security;

import org.apache.commons.lang3.StringUtils;
import org.joyqueue.exception.JoyQueueCode;
import org.joyqueue.exception.JoyQueueException;
import org.joyqueue.response.BooleanResponse;
import org.joyqueue.security.Authentication;
import org.joyqueue.security.PasswordEncoder;
import org.joyqueue.security.UserDetails;
import org.joyqueue.security.impl.DefaultPasswordEncoder;
import org.joyqueue.security.impl.DefaultUserDetail;
import org.joyqueue.toolkit.security.Encrypt;
import org.joyqueue.toolkit.security.Md5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * @author wylixiaobin
 * Date: 2019/1/21
 */
public class Joyqueue0Authentication implements Authentication {
    protected static final Logger logger = LoggerFactory.getLogger(Joyqueue0Authentication.class);

    // 管理员用户
    private String adminUser;
    // 管理员密码
    private String adminPassword;
    // 用户
    private Map<String, String> users;
    private String tokenPrefix = "";
    private static DefaultPasswordEncoder passwordEncoder = new DefaultPasswordEncoder();
    protected Authentication jmq4Authentication;
    public Joyqueue0Authentication(Authentication authentication, String adminUser, String adminPassword){
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.jmq4Authentication = authentication;
    }
    public Joyqueue0Authentication(Authentication authentication, String adminUser, String adminPassword, String tokenPrefix){
        this(authentication,adminUser,adminPassword);
        this.tokenPrefix = tokenPrefix;
    }
    @Override
    public BooleanResponse auth(String userName, String password) {
        return auth(userName,password,false);
    }

    @Override
    public BooleanResponse auth(String userName, String password, boolean checkAdmin) {
        BooleanResponse response =  jmq4Authentication.auth(userName,password,checkAdmin);
        if(response.isSuccess())return response;
        boolean authResult = passwordEncoderAuth(userName,password);
        if(authResult)return BooleanResponse.success();
        return BooleanResponse.failed(JoyQueueCode.CN_AUTHENTICATION_ERROR);
    }

    @Override
    public boolean isAdmin(String userName) {
        if(StringUtils.isBlank(userName))return false;
        return userName.equals(adminUser);
    }

    private boolean passwordEncoderAuth(String userName,String password) {
        try {
            UserDetails user  = getUser(userName);
            // 校验用户是否有效
            if (user == null || !user.isEnabled() || user.isExpired() || user.isLocked()) {
                return false;
            }
            // 校验密码
            String encodedPassword = getPasswordEncode().encode(password);
            if (!user.getPassword().equalsIgnoreCase(encodedPassword)) {
                return false;
            }
            return true;
        } catch (JoyQueueException e) {
            logger.error("auth exception, user: {}, password: {}",userName, password, e);
            return false;
        }
    }
    /**
     * 根据用户名产生密码
     *
     * @param user
     * @return
     */
    public static String createPassword(final String user) throws JoyQueueException {
        try {
            // 构造用户名，不足32位则右填充‘0’
            StringBuilder builder = new StringBuilder(32).append(user == null ? "" : user);
            int length = builder.length();
            if (length < 32) {
                for (int i = 0; i < 32 - length; i++) {
                    builder.append('0');
                }
            }
            // 用户名的MD5
            String source = builder.toString();
            try {
                source = Encrypt.encrypt(source, Encrypt.DEFAULT_KEY, Md5.INSTANCE);
            } catch (NoSuchAlgorithmException ignored) {
            }
            // 取0,4,8,12,16,20,24,28位字符
            builder.delete(0, builder.length());
            length = source.length() / 4;
            for (int i = 0; i < length; i++) {
                builder.append(source.charAt(i * 4));
            }
            source = builder.toString();
            return source;
        } catch (Exception e) {
            throw new JoyQueueException(JoyQueueCode.CN_AUTHENTICATION_ERROR);
        }
    }

    public UserDetails getUser(final String user) throws JoyQueueException {
        boolean admin = false;
        String password;
        if (adminUser != null && adminUser.equalsIgnoreCase(user)) {
            password = adminPassword;
            admin = true;
        } else if (users != null && users.containsKey(user)) {
            // 兼容原有密码
            password = users.get(user);
        } else {
            // 原始密码
            password = createPassword(tokenPrefix + user);
        }

        // 加密
        password = getPasswordEncode().encode(password);

        return new DefaultUserDetail(user, password, admin);
    }

    public PasswordEncoder getPasswordEncode() {
        return passwordEncoder;
    }
}
