package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.model.MyException;
import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import cn.laifuzhi.template.model.http.resp.Resp;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 全局异常处理器，未捕获的异常统一返回未知错误
 * 包括spring校验http请求类型，参数转换错误都会被捕获
 */
@Slf4j
@RestControllerAdvice
public class ErrorController {

//    @ResponseStatus默认返回500状态码，并且设置响应头Connection: close
//    DispatcherServlet.doDispatch会捕获Throwable包装成NestedServletException
    @ExceptionHandler(Throwable.class)
    private Resp<Void> handlerThrowable(Throwable t) {
        log.error("unknown error", t);
        return Resp.build(BizCodeEnum.UNKNOWN_ERROR);
    }

    // Controller中方法抛出spring自己定义的BindException，其他类的方法里抛出jsr303标准的ConstraintViolationException
    // 因为spring在controller中需要把错误信息帮到view里，所以和标准处理不同
    // 默认支持国际化，国际化资源文件在hibernate-validator.jar
    @ExceptionHandler(BindException.class)
    private Resp<Void> handlerBindException(BindException e) {
        log.error("param error", e);
        List<String> errorMessageList = e.getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ":" + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        return Resp.build(BizCodeEnum.ILLEGAL_PARAM, JSON.toJSONString(errorMessageList));
    }

    // 异步请求超时，又没有设置超时结果时抛出该异常
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    private Resp<Void> handlerAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        return Resp.build(BizCodeEnum.TIMEOUT);
    }

    @ExceptionHandler(MyException.class)
    private Resp<Void> handlerMyException(MyException e) {
        return Resp.build(e.getBizCodeEnum());
    }
}