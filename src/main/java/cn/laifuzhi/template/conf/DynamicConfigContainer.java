package cn.laifuzhi.template.conf;

import cn.laifuzhi.template.dao.DynamicConfigDao;
import cn.laifuzhi.template.model.PO.DynamicConfigPO;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 如果使用文件存储配置，不要使用文件修改时间判断内容是否更新，可以文件内容中维护版本号或者通过文件内容对比，而且需要对文件的操作加读写锁避免读到写入一半的内容
// https://heapdump.cn/article/3778858 jdk bug造成感知不到文件更新
@Slf4j
@Component
public class DynamicConfigContainer {
    private static final String DEFAULT_DYNAMIC_CONFIG_KEY = "default";

    @Resource
    private DynamicConfigDao configDao;

    private ScheduledExecutorService scheduledExecutor;
    private String dynamicConfigRawString;
    @Getter
    private volatile DynamicConfig dynamicConfig;

    @PostConstruct
    private void init() {
        log.info("DynamicConfigContainer init");
        refresh();
        if (dynamicConfig == null) {
            throw new RuntimeException("DynamicConfigContainer null");
        }
        scheduledExecutor = new ScheduledThreadPoolExecutor(1, new CustomizableThreadFactory(getClass().getSimpleName()));
        scheduledExecutor.scheduleWithFixedDelay(this::refresh, 5, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("DynamicConfigContainer shutdown ...");
        scheduledExecutor.shutdown();
        while (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("DynamicConfigContainer shutdown wait");
        }
        log.info("DynamicConfigContainer shutdown finished");
    }

    private synchronized void refresh() {
        try {
            Optional<DynamicConfigPO> configPOOptional = configDao.select(DEFAULT_DYNAMIC_CONFIG_KEY);
            if (!configPOOptional.isPresent()
                    || StringUtils.isBlank(configPOOptional.get().getConfigJson())
                    || StringUtils.equals(dynamicConfigRawString, configPOOptional.get().getConfigJson())) {
                return;
            }
            DynamicConfigPO dynamicConfigPO = configPOOptional.get();
            dynamicConfigRawString = dynamicConfigPO.getConfigJson();
            dynamicConfig = JSON.parseObject(dynamicConfigPO.getConfigJson(), DynamicConfig.class);
            log.info("DynamicConfigContainer refresh:{}", JSON.toJSONString(dynamicConfigPO));
        } catch (Throwable t) {
            log.error("DynamicConfigContainer refresh error", t);
        }
    }
}
