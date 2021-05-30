package cn.laifuzhi.template.filter;

import cn.laifuzhi.template.model.http.req.CommonContext;
import cn.laifuzhi.template.utils.CommonThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static cn.laifuzhi.template.utils.Const.CommonReqHeader.REFRESH_TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonReqHeader.TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonReqHeader.USERNAME;
import static cn.laifuzhi.template.utils.Const.MDC_TRACE_ID;

@Slf4j
// 不在FilterConfig中注册也可以生效，全部拦截
@Component
public class CommonFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            CommonContext commonContext = new CommonContext();
            commonContext.setServletReq(request);
            commonContext.setServletResp(response);
            commonContext.setUsername(request.getHeader(USERNAME));
            commonContext.setUri(request.getRequestURI());
            commonContext.setToken(request.getHeader(TOKEN));
            commonContext.setRefreshToken(request.getHeader(REFRESH_TOKEN));

            MDC.put(MDC_TRACE_ID, StringUtils.replace(UUID.randomUUID().toString(), "-", ""));
            CommonThreadLocal.put(commonContext);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            CommonThreadLocal.remove();
        }
    }
}
