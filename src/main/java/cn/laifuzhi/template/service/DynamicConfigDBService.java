package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.DynamicConfigDao;
import cn.laifuzhi.template.model.DynamicConfig;
import cn.laifuzhi.template.model.PO.DynamicConfigPO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cn.laifuzhi.template.utils.Const.DEFAULT_DYNAMIC_CONFIG_KEY;

@Slf4j
@Component
public class DynamicConfigDBService {
    private static final int REFRESH_PERIOD_SECOND = 5;

    private ScheduledExecutorService scheduledExecutor;
    private long version;
    private volatile DynamicConfig dynamicConfig;

    @Resource
    private DynamicConfigDao configDao;

    @PostConstruct
    private void init() {
        log.info("DynamicConfigDBService init");
        refresh();
        if (dynamicConfig == null) {
            throw new RuntimeException("DynamicConfigDBService null");
        }
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("DynamicConfigDBService shutdown ...");
        scheduledExecutor.shutdown();
        while (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("DynamicConfigDBService shutdown wait");
        }
        log.info("DynamicConfigDBService shutdown finished");
    }

    private synchronized void refresh() {
        try {
            Optional<DynamicConfigPO> optional = configDao.select(DEFAULT_DYNAMIC_CONFIG_KEY, version);
            if (!optional.isPresent()) {
                return;
            }
            DynamicConfigPO dynamicConfigPO = optional.get();
            if (StringUtils.isBlank(dynamicConfigPO.getConfigJson())) {
                return;
            }
            dynamicConfig = JSON.parseObject(dynamicConfigPO.getConfigJson(), DynamicConfig.class);
            version = dynamicConfigPO.getVersion();
            log.info("DynamicConfigDBService refresh:{}", JSON.toJSONString(dynamicConfigPO));
        } catch (Throwable t) {
            log.error("DynamicConfigDBService refresh error", t);
        }
    }

    public void start() {
        scheduledExecutor.scheduleWithFixedDelay(this::refresh, REFRESH_PERIOD_SECOND, REFRESH_PERIOD_SECOND, TimeUnit.SECONDS);
    }

    public Optional<DynamicConfig> getDynamicConfig() {
        return Optional.ofNullable(dynamicConfig);
    }
}
