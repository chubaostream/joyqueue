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
package org.joyqueue.message;


public enum SourceType {

    JMQ((byte) 0),

    KAFKA((byte) 1),

    MQTT((byte) 2),

    JOYQUEUE((byte) 3),

    JOYQUEUE0((byte) 4),

    OTHERS((byte) 10),

    INTERNAL((byte) 11),

    ;

    private byte value;

    SourceType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static SourceType valueOf(byte value) {
        for (SourceType type : SourceType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return OTHERS;
    }
}
