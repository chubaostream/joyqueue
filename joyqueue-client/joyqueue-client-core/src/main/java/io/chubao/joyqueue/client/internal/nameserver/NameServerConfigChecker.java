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
package io.chubao.joyqueue.client.internal.nameserver;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

/**
 * NameServerConfigChecker
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/2/19
 */
public class NameServerConfigChecker {

    public static void check(NameServerConfig config) {
        Preconditions.checkArgument(config != null, "nameserver can not be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getAddress()), "nameserver.address can not be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getApp()), "nameserver.app can not be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(config.getToken()), "nameserver.token can not be null");
        Preconditions.checkArgument(config.getUpdateMetadataInterval() > 0, "nameserver.updateMetadataInterval must be greater than 0");
        Preconditions.checkArgument(config.getTempMetadataInterval() > 0, "nameserver.tempMetadataInterval must be greater than 0");
        Preconditions.checkArgument(config.getUpdateMetadataThread() > 0, "nameserver.updateMetadataThread must be greater than 0");
        Preconditions.checkArgument(config.getUpdateMetadataQueueSize() > 0, "nameserver.updateMetadataQueueSize must be greater than 0");
    }
}