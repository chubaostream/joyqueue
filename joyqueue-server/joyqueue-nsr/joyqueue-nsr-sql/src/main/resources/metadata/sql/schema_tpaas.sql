CREATE TABLE `topic` (
  `id` varchar(255) comment 'id' NOT NULL,
  `code` varchar(255) comment '主题名称' DEFAULT NULL,
  `namespace` varchar(255) comment '作用域' DEFAULT NULL,
  `partitions` int(11) comment '分区' DEFAULT NULL,
  `priority_partitions` varchar(255) comment '优先分区' DEFAULT NULL,
  `type` tinyint(1) comment '类型' DEFAULT NULL,
  `policy` varchar(1024) comment '策略' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_code_namespace` (`code`(200),`namespace`(20))
) comment '主题' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `producer` (
  `id` varchar(255) comment 'id' NOT NULL,
  `namespace` varchar(255) comment '作用域' DEFAULT NULL,
  `topic` varchar(255) comment '主题名称' DEFAULT NULL,
  `app` varchar(255) comment '应用' DEFAULT NULL,
  `client_type` tinyint(1) comment '客户端类型' DEFAULT NULL,
  `produce_policy` varchar(1024) comment '生产策略' DEFAULT NULL,
  `limit_policy` varchar(1024) comment '限流策略' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_namespace_app` (`topic`(200),`namespace`(20),`app`(20)),
  KEY `idx_app` (`app`)
) comment '生产者' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `partition_group_replica` (
  `id` varchar(255) comment 'id' NOT NULL,
  `topic` varchar(255) comment '主题名称' DEFAULT NULL,
  `namespace` varchar(255) comment '作用域' DEFAULT NULL,
  `broker_id` bigint(11) comment 'broker id' DEFAULT NULL,
  `group` int(11) comment '组' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_namespace_group` (`topic`(200),`namespace`(20),`group`),
  KEY `idx_broker` (`broker_id`)
) comment '副本' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `partition_group` (
  `id` varchar(255) comment 'id' NOT NULL,
  `namespace` varchar(255) comment '作用域' DEFAULT NULL,
  `topic` varchar(255) comment '主题名称' DEFAULT NULL,
  `group` int(11) comment '组' DEFAULT NULL,
  `leader` int(11) comment 'leader' DEFAULT NULL,
  `isrs` varchar(255) comment 'isr' DEFAULT NULL,
  `term` int(11) comment 'term' DEFAULT NULL,
  `partitions` varchar(1024) comment '分区' DEFAULT NULL,
  `learners` varchar(1024) comment 'learners' DEFAULT NULL,
  `replicas` varchar(1024) comment '副本' DEFAULT NULL,
  `out_sync_replicas` varchar(1024) comment 'out_sync_replicas' DEFAULT NULL,
  `elect_Type` tinyint(1) comment '选举类型' DEFAULT NULL,
  `rec_leader` int(11) comment '推荐leader' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_namespace` (`topic`(200),`namespace`(20))
) comment '分区组' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `namespace` (
  `id` varchar(255) comment 'id' NOT NULL,
  `code` varchar(255) comment 'code' DEFAULT NULL,
  `name` varchar(255) comment '名称' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`)
) comment '作用域' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `datacenter` (
  `id` varchar(255) comment 'id' NOT NULL,
  `region` varchar(255) comment '区域' DEFAULT NULL,
  `code` varchar(255) comment 'code' DEFAULT NULL,
  `name` varchar(255) comment '名称' DEFAULT NULL,
  `url` varchar(255) comment 'url' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`)
) comment '数据中心' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `consumer` (
  `id` varchar(255) comment 'id' NOT NULL,
  `namespace` varchar(255) comment '作用域' DEFAULT NULL,
  `topic` varchar(255) comment '主题名称' DEFAULT NULL,
  `app` varchar(255) comment '应用' DEFAULT NULL,
  `topic_type` tinyint(1) comment '主题类型' DEFAULT NULL,
  `client_type` tinyint(1) comment '客户端类型' DEFAULT NULL,
  `group` varchar(255) comment '组' DEFAULT NULL,
  `referer` varchar(255) comment 'referer' DEFAULT NULL,
  `consume_policy` varchar(1024) comment '消费策略' DEFAULT NULL,
  `retry_policy` varchar(1024) comment '重试策略' DEFAULT NULL,
  `limit_policy` varchar(1024) comment '限流策略' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_namespace_app` (`topic`(200),`namespace`(10),`app`(20)),
  KEY `idx_referer` (`referer`),
  KEY `idx_app` (`app`)
) comment '消费者' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `config` (
  `id` varchar(255) comment 'id' NOT NULL,
  `key` varchar(255) comment 'key' DEFAULT NULL,
  `value` varchar(255) comment 'value' DEFAULT NULL,
  `group` varchar(1024) comment 'group' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_key_group` (`key`(200),`group`(50)),
  KEY `idx_group` (`group`(255))
) comment '配置' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `broker` (
  `id` bigint(11) comment 'id' NOT NULL,
  `ip` varchar(255) comment 'ip' DEFAULT NULL,
  `port` int(11) comment 'port' DEFAULT NULL,
  `data_center` varchar(255) comment '数据中心' DEFAULT NULL,
  `retry_type` varchar(255) comment '重试类型' DEFAULT NULL,
  `permission` varchar(255) comment '权限' DEFAULT NULL,
  `external_ip` varchar(255) comment '对外IP' DEFAULT NULL,
  `external_port` int(11) comment '对外端口' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ip_port` (`ip`(200),`port`),
  KEY `idx_retry_type` (`retry_type`),
  KEY `idx_data_center` (`data_center`)
) comment 'broker' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `app_token` (
  `id` varchar(255) comment 'id' NOT NULL,
  `app` varchar(255) comment '应用' DEFAULT NULL,
  `token` varchar(255) comment '令牌' DEFAULT NULL,
  `effective_time` datetime comment '生效时间' DEFAULT NULL,
  `expiration_time` datetime comment '过期时间' DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app_token` (`app`(20),`token`(200)),
  KEY `idx_token` (`token`)
) comment '令牌' ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;