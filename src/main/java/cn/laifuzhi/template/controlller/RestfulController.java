package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.aop.APIEntrance;
import cn.laifuzhi.template.model.MyException;
import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import cn.laifuzhi.template.model.http.req.BaseReq;
import cn.laifuzhi.template.model.http.resp.Resp;
import cn.laifuzhi.template.service.LockService;
import cn.laifuzhi.template.utils.CommonRunnable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.Duration;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通过DeferredResult一样可以实现全链路异步，和webflux一样的效果
 * 早期没有webflux的时候已经有人通过servlet3+的异步servlet能力实现了全链路异步
 * 实际上在webflux中如果底层容器使用的是servlet容器，也是通过的异步servlet实现的
 * DeferredResult不设置超时时间，以spring.mvc.async.request-timeout或容器自身默认超时为准
 * DeferredResult不设置超时结果，抛出AsyncRequestTimeoutException
 */
@Slf4j
@Validated
// @CrossOrigin
@RestController
@RequestMapping(path = "/rest")
public class RestfulController extends AsyncBaseController{
    @Resource
    private LockService lockService;

    // @Validated是@Valid的增强，支持分组功能，支持在类上使用(此时可以直接对方法参数使用@NotBlank的校验注解)
    // 最佳实践是controller中方法参数用@Valid(http请求大多会有一个请求参数的基类)，其他类用@Validated标注，方法中用@Valid等注解
    // Controller中方法抛出spring自己定义的BindException，其他类的方法里抛出jsr303标准的ConstraintViolationException
    // 因为spring在controller中需要把错误信息帮到view里，所以和标准处理不同
    // 默认支持国际化，国际化资源文件在hibernate-validator.jar
    @APIEntrance
    @GetMapping("/get")
    public DeferredResult<Resp<Void>> get(@Valid @RequestBody BaseReq req) throws MyException {
        DeferredResult<Resp<Void>> deferredResult = buildDeferredResp();
        new Thread(new CommonRunnable(() -> {
            try {
                TimeUnit.SECONDS.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            deferredResult.setResult(Resp.build(BizCodeEnum.UNKNOWN_ERROR));
        })).start();
        return deferredResult;
    }

    @APIEntrance
    @GetMapping("get1")
    public Resp<Map> get1(HttpServletRequest request) throws JsonProcessingException {
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
            String headName = headerNames.nextElement();
            System.out.println(headName + ":" + request.getHeader(headName));
        }
        System.out.println(request.getRemotePort());
        System.out.println(request.getProtocol());
        HashMap<String, Object> map = Maps.newHashMap();
        map.put("dd", new Date());
        map.put("aa", Duration.ofDays(2));
        throw new RuntimeException("123");
//        return Resp.build(BizCodeEnum.OK, map);
    }

}
