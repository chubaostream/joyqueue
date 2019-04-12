/**
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
package com.jd.journalq.handler.routing.command.log;

import com.jd.journalq.handler.routing.command.CommandSupport;
import com.jd.journalq.model.domain.OperLog;
import com.jd.journalq.model.query.QOperLog;
import com.jd.journalq.service.OperLogService;

/**
 * Created by wangxiaofei1 on 2018/12/3.
 */
public class OperLogCommand extends CommandSupport<OperLog,OperLogService,QOperLog> {

}
