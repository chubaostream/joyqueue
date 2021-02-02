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
package org.joyqueue.service;

import org.joyqueue.model.domain.ApplicationUser;
import org.joyqueue.model.domain.User;
import org.joyqueue.model.query.QUser;

import java.util.List;

/**
 * 用户服务
 * Created by yangyang115 on 18-7-27.
 */
public interface UserService extends PageService<User, QUser> {

    /**
     * 查找应用
     *
     * @param code 应用代码
     * @return
     */
    User findByCode(String code);

    /**
     * Find user list by user code list
     * @param codes
     * @return
     */
    List<User> findByCodes(List<String> codes);

    /**
     * Find user list by user id list
     * @param ids
     * @return
     */
    List<User> findByIds(List<String> ids);

    /**
     * 根据应用ID查找
     *
     * @param appId
     * @return
     */
    List<User> findByAppId(long appId);

    /**
     * 根据应用Code查找
     *
     * @param appCode
     * @return
     */
    List<User> findByAppCode(String appCode);

    /**
     * 添加应用用户
     *
     * @param appUser
     * @return
     */
    int addAppUser(ApplicationUser appUser);

    /**
     * 删除应用用户
     *
     * @param userId * @param appId
     * @return
     */
    int deleteAppUser(long userId, long appId);

    /**
     * 删除应用用户
     *
     * @param appUserId
     * @return
     */
    int deleteAppUserById(long appUserId);

    /**
     * 根据ID查找应用用户
     *
     * @param appUserId
     * @return
     */
    ApplicationUser findAppUserById(long appUserId);

    /**
     * 根据ID查找应用用户
     *
     * @param appId
     * @param userId
     * @return
     */
    ApplicationUser findAppUserByAppIdAndUserId(long appId, long userId);

    /**
     * 判断所属关系
     *
     * @param userId
     * @param appId
     * @return
     */
    boolean belong(long userId, long appId);

    /**
     * 根据where查询sql查找用户
     * @param sql
     * @return
     */
    List<User> findByWhereSql(String sql, Object object);

    /**
     * Validate where sql
     * @param sql
     * @return
     */
    boolean validateWhereSql(String sql, Object obj);

    /**
     * 根据角色查找用户
     **/
    List<User> findByRole(int role);

    User findUserByNameAndPassword(String name, String password);

}
