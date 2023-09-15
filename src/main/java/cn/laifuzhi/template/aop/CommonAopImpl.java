package cn.laifuzhi.template.aop;

import com.alibaba.fastjson.JSON;
import com.alibaba.messaging.ops2.dao.OperatorHistoryDao;
import com.alibaba.messaging.ops2.filter.CommonFilter;
import com.alibaba.messaging.ops2.model.PO.OperatorHistoryPO;
import com.alibaba.messaging.ops2.model.enumeration.RecordOperateEnum;
import com.alibaba.messaging.ops2.model.req.BaseReq;
import com.alibaba.messaging.ops2.model.resp.Resp;
import com.alibaba.messaging.ops2.utils.CommonContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @see CommonContext
 * @see CommonFilter
 */
@Slf4j
@Aspect
@Component
public class CommonAopImpl {
    @Resource
    private OperatorHistoryDao historyDao;

    @Around("@annotation(commonAop)")
    private Object process(ProceedingJoinPoint pjp, CommonAop commonAop) {
        long start = System.currentTimeMillis();
        CommonContext commonContext = CommonContext.get();
        HttpServletRequest servletRequest = commonContext.getServletReq();
        if (ArrayUtils.getLength(pjp.getArgs()) == 0 || !(pjp.getArgs()[0] instanceof BaseReq)) {
            return Resp.fail("BaseReq error");
        }
        BaseReq req = (BaseReq) pjp.getArgs()[0];
        req.setEmpId(commonContext.getEmpId());
        req.setEmpName(commonContext.getEmpName());
        try {
            Object result = pjp.proceed();
            if (result != null && !(result instanceof Resp) && !(result instanceof DeferredResult)) {
                log.error("return type wrong, only support void, Resp and DeferredResult<Resp>");
                return result;
            }
            if (result instanceof DeferredResult) {
                ((DeferredResult<?>) result).onCompletion(() -> {
                    Object realResult = ((DeferredResult<?>) result).getResult();
                    if (!(realResult instanceof Resp)) {
                        log.error("DeferredResult only support DeferredResult<Resp>");
                        return;
                    }
                    recordHistory(servletRequest, commonAop, req, realResult, start);
                });
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
            log.info("CommonAop url:{} ip:{} cost:{} req:{} result:{}",
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
