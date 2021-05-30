CREATE TABLE IF NOT EXISTS `lock_info`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lock_key`    varchar(128)        NOT NULL COMMENT '加锁的key',
    `expire_time` datetime(3)         NOT NULL COMMENT '过期时间',
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
