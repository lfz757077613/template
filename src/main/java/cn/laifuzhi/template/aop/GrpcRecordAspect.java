package cn.laifuzhi.template.aop;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static cn.laifuzhi.template.utils.Const.MDC_TRACE_ID;

@Slf4j
@Aspect
@Component
public class GrpcRecordAspect {

    @Around("@annotation(grpcRecord)")
    public Object process(ProceedingJoinPoint pjp, GrpcRecord grpcRecord) throws Throwable {
        String traceId = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_TRACE_ID, traceId)) {
            long start = System.currentTimeMillis();
            Object obj = pjp.proceed();
            StringBuilder sb = new StringBuilder(pjp.getSignature().toShortString());
            if (grpcRecord.recordReq()) {
                sb.append(" param:").append(JSON.toJSONString(pjp.getArgs()));
            }
            if (grpcRecord.recordResp()) {
                sb.append(" result:").append(JSON.toJSONString(obj));
            }
            sb.append(" cost:").append(System.currentTimeMillis() - start);
            log.info(sb.toString());
            return obj;
        }
    }
}
