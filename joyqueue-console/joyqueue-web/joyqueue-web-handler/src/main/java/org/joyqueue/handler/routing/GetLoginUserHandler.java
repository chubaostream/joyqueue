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
package org.joyqueue.handler.routing;

import org.joyqueue.handler.Constants;
import org.joyqueue.model.domain.User;
import org.joyqueue.util.LocalSession;
import com.jd.laf.web.vertx.Command;
import com.jd.laf.web.vertx.RoutingHandler;
import com.jd.laf.web.vertx.response.Responses;
import io.vertx.ext.web.RoutingContext;

/**
 * 获取当前登录用户
 * Created by yangyang36 on 2018/9/18.
 */
public class GetLoginUserHandler implements RoutingHandler {

    @Override
    public void handle(final RoutingContext context) {
        User user = context.get(Constants.USER_KEY);
        if (user != null) {
            context.put(Command.RESULT, Responses.success(user));
            LocalSession.getSession().setUser(user);
            context.next();
        }
    }

    @Override
    public String type() {
        return "getLoginUser";
    }
}
