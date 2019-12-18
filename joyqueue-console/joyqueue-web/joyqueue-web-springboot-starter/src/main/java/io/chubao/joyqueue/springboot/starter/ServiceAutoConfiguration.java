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
package io.chubao.joyqueue.springboot.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Service auto configuration
 * Created by chenyanying3 on 18-11-16.
 */
@Configuration
@ComponentScan(value = {
        "io.chubao.joyqueue.nsr.impl",
        "io.chubao.joyqueue.service.impl",
        "io.chubao.joyqueue.util",
        "io.chubao.joyqueue.async",
        "io.chubao.joyqueue.other",})
@MapperScan(basePackages = {"io.chubao.joyqueue.repository"})
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
public class ServiceAutoConfiguration {

}
