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
package org.joyqueue.broker.joyqueue0.helper;

import org.joyqueue.broker.joyqueue0.Joyqueue0Consts;
import org.joyqueue.domain.Topic;
import org.joyqueue.domain.TopicConfig;

/**
 * 版本号工具
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/30
 */
public class VersionHelper {

    /**
     * 比较版本号
     * @param versionOne
     * @param versionTwo
     * @return
     */
    public static int compareVersion(String versionOne, String versionTwo) {
        String[] clientVersionArray = versionOne.split("\\.");
        String[] needMinVersionArray = versionTwo.split("\\.");
        int result = 0;
        int index = 0;
        int minLength = Math.min(clientVersionArray.length, needMinVersionArray.length);//取最小长度值
        while (index < minLength
                && (result = clientVersionArray[index].length() - needMinVersionArray[index].length()) == 0//先比较长度
                && (result = clientVersionArray[index].compareTo(needMinVersionArray[index])) == 0) {//再比较字符
            ++index;
        }
        //相等的情况下,有子版本的为大；
        result = (result != 0) ? result : clientVersionArray.length - needMinVersionArray.length;
        return result;
    }

    /**
     * 验证客户端版本是否符合要求,低于2.0.0版本客户端禁止使用广播和严格顺序消费功能
     * @param topicConfig
     * @param app
     * @param clientVersion
     * @return
     */
    public static boolean checkVersion(TopicConfig topicConfig, String app, String clientVersion) {
        if (VersionHelper.compareVersion(clientVersion, Joyqueue0Consts.MIN_SUPPORTED_VERSION_STR) >= 0) {
            return true;
        }
        if (topicConfig.getType().equals(Topic.Type.BROADCAST) || topicConfig.getType().equals(Topic.Type.SEQUENTIAL)) {
            return false;
        }
        return true;
    }
}