package com.alibaba.messaging.ops2.aop;

import com.alibaba.buc.sso.client.vo.BucSSOUser;
import com.alibaba.fastjson.JSON;
import com.alibaba.messaging.ops2.client.AclClient;
import com.alibaba.messaging.ops2.dao.OperatorHistoryDao;
import com.alibaba.messaging.ops2.filter.CommonFilter;
import com.alibaba.messaging.ops2.model.PO.OperatorHistoryPO;
import com.alibaba.messaging.ops2.model.enumeration.RecordOperateEnum;
import com.alibaba.messaging.ops2.model.req.BaseReq;
import com.alibaba.messaging.ops2.model.resp.Resp;
import com.alibaba.messaging.ops2.utils.CommonContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import static com.alibaba.messaging.ops2.utils.Utils.indexOf;

/**
 * @see CommonContext
 * @see CommonFilter
 */
@Slf4j
@Aspect
@Component
public class CommonAopImpl {
    private static final String ACL_WRITE_PERMISSION = "mq-ops-vpc-write";
    @Resource
    private OperatorHistoryDao historyDao;
    @Resource
    private AclClient aclClient;

    @Around("@annotation(commonAop)")
    private Object process(ProceedingJoinPoint pjp, CommonAop commonAop) {
        long start = System.currentTimeMillis();
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        if (returnType.getRawType() != Resp.class && returnType.getRawType() != DeferredResult.class) {
            return Resp.fail("return type wrong, only support Resp and DeferredResult");
        }
        if (returnType.getRawType() == DeferredResult.class && returnType.getActualTypeArguments()[0] != Resp.class) {
            return Resp.fail("return type wrong, DeferredResult only support DeferredResult<Resp>");
        }
        int baseReqIndex = indexOf(method.getParameterTypes(), BaseReq.class::isAssignableFrom);
        if (baseReqIndex < 0) {
            return Resp.fail("param type wrong, no BaseReq");
        }
        BaseReq req = (BaseReq) pjp.getArgs()[baseReqIndex];

        CommonContext commonContext = CommonContext.get();
        HttpServletRequest servletRequest = commonContext.getServletReq();
        req.setEmpId(commonContext.getEmpId());
        req.setEmpName(commonContext.getEmpName());
        try {
            BucSSOUser bucSSOUser = commonContext.getBucSSOUser();
            if (bucSSOUser != null && commonAop.recordHistory() != RecordOperateEnum.NONE && !aclClient.checkPermission(bucSSOUser, ACL_WRITE_PERMISSION)) {
                return Resp.nonePerm(ACL_WRITE_PERMISSION);
            }
            Object result = pjp.proceed();
            if (result instanceof DeferredResult) {
                DeferredResult<?> deferredResult = (DeferredResult<?>) result;
                deferredResult.onCompletion(() -> recordHistory(servletRequest, commonAop, req, deferredResult.getResult(), start));
                return result;
            }
            recordHistory(servletRequest, commonAop, req, result, start);
            return result;
        } catch (Throwable t) {
            log.error("CommonAop error", t);
            Resp<Void> result = Resp.fail(StringUtils.defaultString(t.getMessage(), t.toString()));
            recordHistory(servletRequest, commonAop, req, result, start);
            return result;
        }
    }

    private void recordHistory(HttpServletRequest servletRequest, CommonAop commonAop, BaseReq req, Object result, long start) {
        try {
            log.info("CommonAop url:{} ip:{} cost:{}ms req:{} result:{}",
                    servletRequest.getRequestURI(),
                    servletRequest.getRemoteHost(),
                    System.currentTimeMillis() - start,
                    commonAop.recordReqLog() ? JSON.toJSONString(req) : null,
                    commonAop.recordRespLog() ? JSON.toJSONString(result) : null);

            if (commonAop.recordHistory() == RecordOperateEnum.NONE) {
                return;
            }
            BaseReq cloneReq = req.clone();
            cloneReq.setEmpId(null);
            cloneReq.setEmpName(null);
            cloneReq.setHistoryMessage(null);

            OperatorHistoryPO historyPO = new OperatorHistoryPO();
            historyPO.setOperateType(commonAop.recordHistory().name());
            historyPO.setOperateParam(StringUtils.substring(JSON.toJSONString(cloneReq), 0, 4096));
            historyPO.setOperateResult(StringUtils.substring(JSON.toJSONString(result), 0, 4096));
            historyPO.setMessage(StringUtils.substring(StringUtils.defaultString(req.getHistoryMessage()), 0, 512));
            historyPO.setEmpId(req.getEmpId());
            historyPO.setEmpName(req.getEmpName());
            historyDao.insert(historyPO);
        } catch (Throwable t) {
            log.error("recordHistory error", t);
        }
    }
}
