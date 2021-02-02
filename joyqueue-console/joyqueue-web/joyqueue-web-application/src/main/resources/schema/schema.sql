
CREATE TABLE IF NOT EXISTS `application` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增id',
  `code` varchar(128) NOT NULL COMMENT '应用 代码',
  `name` varchar(64) NOT NULL COMMENT '应用 名称',
  `system` varchar(128) DEFAULT NULL COMMENT '所属系统',
  `department` varchar(128) DEFAULT NULL COMMENT '所属部门',
  `owner_id` bigint(20)  DEFAULT NULL COMMENT '拥有者id',
  `owner_code` varchar(64) DEFAULT NULL COMMENT '拥有者code',
  `source` tinyint(4) NOT NULL DEFAULT '0' COMMENT '来源：0 新建',
  `sign` int(11) NOT NULL DEFAULT '0' COMMENT '签名',
  `create_by` bigint(20) NOT NULL COMMENT '创建者id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：-1 删除，0 禁用，1 启用',
  `alias_code` varchar(128) DEFAULT NULL COMMENT '别名',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `application_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `app_id` bigint(20) NOT NULL COMMENT '应用id',
  `app_code` varchar(128) NOT NULL COMMENT '应用code',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `user_code` varchar(64) NOT NULL COMMENT '用户code',
  `update_by` bigint(20) DEFAULT '0' COMMENT '修改人',
  `update_by_code` varchar(64) DEFAULT 'auto' COMMENT '修改人',
  `update_time` datetime NOT NULL COMMENT '记录更新时间',
  `create_by` bigint(20) DEFAULT '0' COMMENT '创建人',
  `create_by_code` varchar(64) DEFAULT 'auto' COMMENT '创建人code',
  `create_time` datetime NOT NULL COMMENT '记录创建时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：-1 删除，0 禁用，1 启用',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `code` varchar(64) NOT NULL COMMENT '用户英文名',
  `name` varchar(64) DEFAULT NULL COMMENT '用户中文名',
  `org_id` varchar(20) DEFAULT NULL COMMENT '组织id',
  `org_name` varchar(128) DEFAULT NULL COMMENT '组织名',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `role` tinyint(4) NOT NULL DEFAULT '0' COMMENT '权限：0 普通用户，1 管理员',
  `sign` int(11) NOT NULL DEFAULT '0' COMMENT '签名',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：-1 删除，0 禁用，1 启用',
  `password` varchar(64) DEFAULT '' COMMENT '用户密码',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `oper_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` int(11) NOT NULL COMMENT '类型',
  `identity` varchar(100) NOT NULL COMMENT '操作资源ID',
  `oper_type` int(11) NOT NULL COMMENT '操作类型',
  `target` varchar(1536) DEFAULT NULL COMMENT '目标',
  `result` varchar(1024) DEFAULT NULL COMMENT '操作结果，成功或异常信息',
  `description` varchar(512) DEFAULT NULL COMMENT '操作描述，url或其他',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新人',
  `status` tinyint(4) NOT NULL COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `metric` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(128) NOT NULL COMMENT '代码',
  `alias_code` varchar(256) NOT NULL COMMENT '值,唯一',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `type` tinyint(4) NOT NULL COMMENT '类型：0 atomic, 1 aggregator, 10 others(mdc)',
  `source` varchar(128) DEFAULT NULL COMMENT '来源指标code',
  `provider` varchar(128) DEFAULT NULL COMMENT '指标提供方',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `user_permission` tinyint(4) NOT NULL DEFAULT '0' COMMENT '普通用户是否权限，0：否，1：是',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_by` bigint(20) NOT NULL COMMENT '修改人',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：-1 删除，0 禁用，1 启用',
  `collect_interval` int(11) NOT NULL DEFAULT 1 COMMENT '采集间隔',
  `category` varchar(128) COMMENT '指标类型: producer,consumer,broker',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `broker_group` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `code` varchar(128) DEFAULT NULL COMMENT '代码',
  `name` varchar(64) DEFAULT NULL COMMENT '名称',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `policies` varchar(2048) default null COMMENT '策略',
  `labels` varchar(1024) DEFAULT NULL COMMENT '标签',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_by` bigint(20) NOT NULL COMMENT '修改人',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：-1 删除，0 禁用，1 启用',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `broker_group_related` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'brokerId',
  `group_id` bigint(20) DEFAULT NULL COMMENT 'Broker分组id',
  `group_code` varchar(128) DEFAULT NULL COMMENT 'Broker分组code',
  `node_name` varchar(128) DEFAULT NULL COMMENT '主机名',
  `node_ip` varchar(20) DEFAULT NULL COMMENT '主机IP',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_by` bigint(20) NOT NULL COMMENT '修改人',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态: -1 删除，0 禁用，1 启用',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `broker_related` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'brokerId',
  `group_id` bigint(20) DEFAULT NULL COMMENT 'Broker分组id',
  `group_code` varchar(128) DEFAULT NULL COMMENT 'Broker分组code',
  `node_name` varchar(128) DEFAULT NULL COMMENT '主机名',
  `node_ip` varchar(20) DEFAULT NULL COMMENT '主机IP',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint(20) NOT NULL COMMENT '创建人',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_by` bigint(20) NOT NULL COMMENT '修改人',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态: -1 删除，0 禁用，1 启用',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `message_retry` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
	`message_id` varchar(50) NOT NULL COMMENT '消息编号',
	`business_id` varchar(100) DEFAULT NULL COMMENT '业务编号',
	`topic` varchar(100) NOT NULL COMMENT '主题',
	`app` varchar(100) NOT NULL COMMENT '应用',
	`send_time` datetime NOT NULL COMMENT '发送时间',
	`expire_time` datetime NOT NULL COMMENT '过期时间',
	`retry_time` datetime NOT NULL COMMENT '重试时间',
	`retry_count` int(10) NOT NULL DEFAULT '0' COMMENT '重试次数',
	`data` mediumblob NOT NULL COMMENT '消息体',
	`exception` blob COMMENT '异常信息',
	`create_time` datetime NOT NULL COMMENT '创建时间',
	`create_by` int(10) NOT NULL DEFAULT '0' COMMENT '创建人',
	`update_time` datetime NOT NULL COMMENT '更新时间',
	`update_by` int(10) NOT NULL DEFAULT '0' COMMENT '更新人',
	`status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态,0:成功,1:失败,-2:过期',
	PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `topic_msg_filter` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
 `topic_code` varchar(128) NOT NULL COMMENT '主题代码',
 `app` varchar(128) NOT NULL COMMENT '应用',
 `token` varchar(128) NOT NULL COMMENT 'token',
 `partition` int(11) NOT NULL default -1 COMMENT '分区',
 `filter` varchar(128) NOT NULL COMMENT '消息过滤条件',
 `msg_fmt` varchar(64) NOT NULL COMMENT '消息格式',
 `offset` bigint(20) COMMENT '位点',
 `start_time` datetime COMMENT 'offset开始时间',
 `end_time` datetime COMMENT 'offset结束时间',
 `query_count` int(11) default 1 NOT NULL COMMENT '查询次数',
 `total_count` int(11) default 100000 NOT NULL COMMENT '允许查询总条数',
 `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：-1 结束，0 未执行，1 正在执行',
 `url` varchar(512) COMMENT '任务结束后生成的s3链接,默认有7天有效时间',
 `obj_key` varchar(128) COMMENT '如果url失效,根据obj_key重新生成url',
 `create_time` datetime NOT NULL COMMENT '消息过滤任务创建时间',
 `create_by` int(10) NOT NULL DEFAULT '0' COMMENT '创建人',
 `update_time` datetime NOT NULL COMMENT '更新时间',
 `update_by` int(10) NOT NULL DEFAULT '0' COMMENT '更新人',
 `description` varchar(1024) NOT NULL DEFAULT '备注信息' COMMENT '备注信息',
 PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `message_retry` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
 `message_id` varchar(50) NOT NULL COMMENT '消息编号',
 `business_id` varchar(100) DEFAULT NULL COMMENT '业务编号',
 `topic` varchar(100) NOT NULL COMMENT '主题',
 `app` varchar(100) NOT NULL COMMENT '应用',
 `send_time` datetime NOT NULL COMMENT '发送时间',
 `expire_time` datetime NOT NULL COMMENT '过期时间',
 `retry_time` datetime NOT NULL COMMENT '重试时间',
 `retry_count` int(10) NOT NULL DEFAULT '0' COMMENT '重试次数',
 `data` mediumblob NOT NULL COMMENT '消息体',
 `exception` blob COMMENT '异常信息',
 `create_time` datetime NOT NULL COMMENT '创建时间',
 `create_by` int(10) NOT NULL DEFAULT '0' COMMENT '创建人',
 `update_time` datetime NOT NULL COMMENT '更新时间',
 `update_by` int(10) NOT NULL DEFAULT '0' COMMENT '更新人',
 `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态,0:成功,1:失败,-2:过期',
 PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;

-- init default admin USER
INSERT INTO
 `user`(
    `id`,
    `code`,
    `name`,
    `org_id`,
    `org_name`,
    `email`,
    `mobile`,
    `role`,
    `sign`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`,
    `status`,
    `password`)
SELECT
  1, 'admin', 'Admin', NULL, NULL, NULL, NULL, 1, 0, NULL, NOW(), -1, NOW(), 1, '123456'
FROM
  Dual
WHERE
  NOT EXISTS (SELECT 1 FROM  `user`);


-- init default admin USER
-- MERGE INTO `user`
-- (`id`, `code`, `name`, `org_id`, `org_name`, `email`, `mobile`, `role`, `sign`, `create_by`, `create_time`, `update_by`, `update_time`, `status`)
-- VALUES (1, 'admin', 'Admin', NULL, NULL, NULL, NULL, 1, 0, NULL, '2019-01-01 00:00:00', -1, '2019-01-01 00:00:00', 1);