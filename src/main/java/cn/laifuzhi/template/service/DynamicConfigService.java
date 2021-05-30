package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.DynamicConfigDao;
import cn.laifuzhi.template.model.DynamicConfig;
import cn.laifuzhi.template.model.PO.DynamicConfigPO;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cn.laifuzhi.template.utils.Const.DEFAULT_DYNAMIC_CONFIG_KEY;

@Slf4j
@Component
public class DynamicConfigService {
    private static final int REFRESH_PERIOD_SECOND = 5;

    private ScheduledExecutorService executorService;
    private long version;
    @Resource
    private DynamicConfigDao configDao;
    @Getter
    private volatile DynamicConfig config;

    @PostConstruct
    private void init() {
        log.info("DynamicConfig init");
        refresh();
        if (config == null) {
            throw new RuntimeException("DynamicConfig null");
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("DynamicConfig shutdown ...");
        executorService.shutdown();
        while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("DynamicConfig shutdown wait");
        }
        log.info("DynamicConfig shutdown finished");
    }

    private synchronized void refresh() {
        try {
            DynamicConfigPO dynamicConfigPO = configDao.select(DEFAULT_DYNAMIC_CONFIG_KEY, version);
            if (dynamicConfigPO == null || StringUtils.isBlank(dynamicConfigPO.getConfigJson())) {
                return;
            }
            config = JSON.parseObject(dynamicConfigPO.getConfigJson(), DynamicConfig.class);
            version = dynamicConfigPO.getVersion();
            log.info("DynamicConfig refresh:{}", JSON.toJSONString(dynamicConfigPO));
        } catch (Exception e) {
            log.error("DynamicConfig refresh error", e);
        }
    }

    public void start() {
        executorService.scheduleWithFixedDelay(this::refresh, REFRESH_PERIOD_SECOND, REFRESH_PERIOD_SECOND, TimeUnit.SECONDS);
    }
}
