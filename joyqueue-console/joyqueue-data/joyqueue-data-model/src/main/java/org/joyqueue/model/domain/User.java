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
package org.joyqueue.model.domain;


/**
 * Created by yangyang36 on 2018/9/12.
 */
public class User extends BaseModel implements Identifier {

    private String code;
    private String name;
    private String orgId;
    private String password;
    private String orgName;
    private String email;
    private String mobile;
    //签名
    private int sign;
    private int role;
    private int status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public int getSign() {
        return sign;
    }

    public void setSign(int sign) {
        this.sign = sign;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public enum UserRole {

        NORMAL(0, "普通用户"),
        ADMIN(1, "管理员");

        private int value;
        private String description;

        UserRole(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int value() {
            return value;
        }

        public String description() {
            return description;
        }

        public static UserRole valueOf(int value) {
            switch (value) {
                case 0:
                    return NORMAL;
                case 1:
                    return ADMIN;
                default:
                    return NORMAL;
            }
        }
    }

    public enum UserStatus {

        ENABLE(1, "可用"),
        UNABLE(0, "禁用");

        private int value;
        private String description;

        UserStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int value() {
            return value;
        }

        public String description() {
            return description;
        }

        public static UserStatus valueOf(int value) {
            switch (value) {
                case 1:
                    return ENABLE;
                case 0:
                    return UNABLE;
                default:
                    return ENABLE;
            }
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", orgId='" + orgId + '\'' +
                ", orgName='" + orgName + '\'' +
                ", email='" + email + '\'' +
                ", mobile='" + mobile + '\'' +
                ", sign=" + sign +
                ", role=" + role +
                ", status=" + status +
                ", createBy=" + createBy +
                ", createTime=" + createTime +
                ", updateBy=" + updateBy +
                ", updateTime=" + updateTime +
                '}';
    }
}
