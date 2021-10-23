CREATE TABLE IF NOT EXISTS `lock_info`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lock_key`    varchar(128)        NOT NULL COMMENT '加锁的key',
    `expire_time` bigint unsigned     NOT NULL COMMENT '过期时间',
    `create_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lock_key` (`lock_key`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT ='分布式锁信息表';

CREATE TABLE IF NOT EXISTS `dynamic_config`
(
    `config_key`    varchar(32)        NOT NULL COMMENT '配置key',
    `config_json` text NOT NULL  COMMENT '动态配置json字符串',
    `version` bigint unsigned DEFAULT 1 NOT NULL  COMMENT '当前版本',
    `create_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT ='动态配置表';
INSERT IGNORE INTO `lfz`.`dynamic_config`(`config_key`, `config_json`) VALUES ('default', '{}');

CREATE TABLE IF NOT EXISTS `sequence_info`
(
    `sequence_key` varchar(32)        NOT NULL COMMENT 'sequence key',
    `sequence`   bigint unsigned   NOT NULL DEFAULT 0 COMMENT '当前sequence',
    `create_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`sequence_key`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT ='sequence配置表';
INSERT IGNORE INTO `lfz`.`sequence_info`(`sequence_key`) VALUES ('default');

CREATE TABLE IF NOT EXISTS `user_info`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    varchar(128)        NOT NULL COMMENT '用户名',
    `create_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin AUTO_INCREMENT = 10000000 COMMENT ='用户信息表';

CREATE TABLE IF NOT EXISTS `order_info`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`     bigint unsigned NOT NULL COMMENT '用户id',
    `order_id`    bigint unsigned       NOT NULL COMMENT '订单id',
    `state`    tinyint       NOT NULL COMMENT '订单状态',
    `create_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `update_ts`   datetime(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    INDEX `idx_uid` (`uid`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT ='订单信息表';
