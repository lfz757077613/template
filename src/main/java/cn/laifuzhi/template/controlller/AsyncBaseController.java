package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.conf.StaticConfig;
import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import cn.laifuzhi.template.model.http.resp.Resp;
import cn.laifuzhi.template.utils.CommonRunnable;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AsyncBaseController {
    @Resource
    private StaticConfig staticConfig;

    protected static ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            0,
            1,
            10, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new CustomizableThreadFactory("MyScheduler")){{
        allowCoreThreadTimeOut(true);
    }};

    protected void submit(DeferredResult<ResponseEntity<Resp<?>>> result, Runnable runnable) {
        try {
            executorService.execute(new CommonRunnable(runnable));
        } catch (Exception e) {
            result.setResult(ResponseEntity.internalServerError().body(Resp.build(BizCodeEnum.UNKNOWN_ERROR)));
        }
    }

    protected <T> DeferredResult<Resp<T>> buildDeferredResp() {
        DeferredResult<Resp<T>> deferredResult = new DeferredResult<>();
        deferredResult.onCompletion(new CommonRunnable(()-> log.info("deferred complete, resp:{}", JSON.toJSONString(deferredResult.getResult()))));
        return deferredResult;
    }

    protected <T> DeferredResult<ResponseEntity<T>> buildDeferredResponseEntity() {
        DeferredResult<ResponseEntity<T>> deferredResult = new DeferredResult<>();
        deferredResult.onCompletion(new CommonRunnable(() -> log.info("deferred complete")));
        return deferredResult;
    }
}
