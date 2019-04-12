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
package com.jd.journalq.nsr.network;

import com.jd.journalq.network.transport.command.handler.CommandHandler;
import com.jd.journalq.nsr.NameServiceAware;

/**
 * @author wylixiaobin
 * Date: 2019/3/14
 */
public interface NsrCommandHandler extends CommandHandler ,NameServiceAware {
    public static final String SERVER_TYPE="SERVER";
    public static final String THIN_TYPE="THIN";
}
