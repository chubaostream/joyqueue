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
package org.joyqueue.client.internal.producer.domain;

import org.joyqueue.exception.JoyQueueCode;

import java.util.List;

/**
 * FetchFeedbackData
 *
 * author: gaohaoxiang
 * date: 2018/12/24
 */
public class FetchFeedbackData {

    private List<FeedbackData> data;
    private JoyQueueCode code;

    public List<FeedbackData> getData() {
        return data;
    }

    public void setData(List<FeedbackData> data) {
        this.data = data;
    }

    public JoyQueueCode getCode() {
        return code;
    }

    public void setCode(JoyQueueCode code) {
        this.code = code;
    }
}