package cn.laifuzhi.template.aop;

import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import cn.laifuzhi.template.model.http.resp.Resp;
import cn.laifuzhi.template.utils.CommonContext;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

// 注意aop不能拦截private方法
@Slf4j
@Aspect
@Component
public class APIEntranceAspect {
    @Around("@annotation(apiEntrance)")
    private Object process(ProceedingJoinPoint pjp, APIEntrance apiEntrance) throws Throwable {
        long start = System.currentTimeMillis();
        CommonContext commonContext = CommonContext.get();
        // getServletPath()返回servlet路径，getRequestURI返回真正请求的路径，可能有多个/或者.等字符
        if (apiEntrance.recordReq()) {
            log.info("api aop url:{} req:{}", commonContext.getServletReq().getRequestURI(), JSON.toJSONString(pjp.getArgs()));
        }
        BizCodeEnum authResult = auth(apiEntrance, commonContext);
        Object result = authResult == BizCodeEnum.OK ? pjp.proceed() : Resp.build(authResult);
        if (apiEntrance.recordResp()) {
            if (result instanceof DeferredResult) {
                DeferredResult<?> deferredResult = (DeferredResult<?>) result;
                deferredResult.onCompletion(() -> {
                    log.info("api aop result:{} cost:{}", JSON.toJSONString(deferredResult.getResult()), System.currentTimeMillis() - start);
                });
            } else {
                log.info("api aop result:{} cost:{}", JSON.toJSONString(result), System.currentTimeMillis() - start);
            }
        }
        return result;
    }

    private BizCodeEnum auth(APIEntrance apiEntrance, CommonContext commonContext) {
        if (!apiEntrance.needLogin() && StringUtils.isAllBlank(apiEntrance.role(), apiEntrance.perm())) {
            return BizCodeEnum.OK;
        }
        String uid = commonContext.getUid();
        if (StringUtils.isBlank(uid)) {
            return BizCodeEnum.NO_LOGIN;
        }
        if (StringUtils.isNotBlank(apiEntrance.role()) && !validRole(uid, apiEntrance.role())) {
            return BizCodeEnum.NO_ROLE;
        }
        if (StringUtils.isNotBlank(apiEntrance.perm()) && !validPerm(uid, apiEntrance.perm())) {
            return BizCodeEnum.NO_PERM;
        }
        return BizCodeEnum.OK;
    }

    private boolean validRole(String uid, String role) {
        return true;
    }

    private boolean validPerm(String uid, String perm) {
        return true;
    }
}
