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
package org.joyqueue.handler.routing;


import com.jd.laf.binding.annotation.Value;
import com.jd.laf.web.vertx.handler.RemoteIpHandler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import static com.jd.laf.web.vertx.Environment.USER_KEY;

/**
 * @author jiangnan53
 * @date 2020/6/11
 **/
public class UserLogoutHandler extends RemoteIpHandler {

    @Value(value = "user.session.key", defaultValue = "user")
    protected String userSessionKey;

    @Override
    public void handle(RoutingContext context) {
        Session session = context.session();
        session.remove(userSessionKey);
        context.remove(USER_KEY);
        context.clearUser();
        context.next();
    }

    @Override
    public String type() {
        return "UserLogout";
    }
}
