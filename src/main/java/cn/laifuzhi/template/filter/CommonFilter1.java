//package cn.laifuzhi.template.filter;
//
//import com.alibaba.buc.sso.client.util.SimpleUserUtil;
//import com.alibaba.buc.sso.client.vo.BucSSOUser;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.messaging.ops2.aop.CommonAopImpl;
//import com.alibaba.messaging.ops2.conf.StaticConfig;
//import com.alibaba.messaging.ops2.model.resp.Resp;
//import com.alibaba.messaging.ops2.utils.CommonContext;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.collect.Maps;
//import com.taobao.common.keycenter.security.Cryptograph;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.MapUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.slf4j.MDC;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import javax.annotation.Resource;
//import javax.servlet.FilterChain;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import java.util.Enumeration;
//import java.util.Map;
//import java.util.TreeMap;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import static com.alibaba.messaging.ops2.model.req.BaseReq.API_PREFIX;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.EMP_ID;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.EMP_NAME;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.INNER_PREFIX;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.SIGN;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.SOURCE;
//import static com.alibaba.messaging.ops2.model.req.BaseReq.TIMESTAMP;
//import static com.alibaba.messaging.ops2.utils.Const.FilterNames.COMMON_FILTER;
//import static com.alibaba.messaging.ops2.utils.Const.MDC_TRACE_ID;
//import static com.alibaba.messaging.ops2.utils.Utils.buildSign;
//
///**
// * @see CommonContext
// * @see CommonAopImpl
// */
//@Slf4j
//@Component(COMMON_FILTER)
//public class CommonFilter1 extends OncePerRequestFilter {
//    private final Map<String, byte[]> secretKeyMap = Maps.newHashMap();
//
//    @Resource
//    private ObjectMapper objectMapper;
//
//    public CommonFilter1(StaticConfig config, Cryptograph cryptograph) {
//        if (MapUtils.isEmpty(config.getInnerKeyMap())) {
//            return;
//        }
//        for (Map.Entry<String, String> entry : config.getInnerKeyMap().entrySet()) {
//            String source = entry.getKey();
//            String key = cryptograph.decrypt(entry.getValue(), config.getKeyCenterKeyName());
//            secretKeyMap.put(source, Base64.getDecoder().decode(key));
//        }
//    }
//
//    @SneakyThrows
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
//        try {
//            MDC.put(MDC_TRACE_ID, StringUtils.remove(UUID.randomUUID().toString(), '-'));
//            CommonContext commonContext = new CommonContext();
//            commonContext.setServletReq(request);
//            commonContext.setServletResp(response);
//
//            String servletPath = request.getServletPath();
//            if (!StringUtils.startsWithAny(servletPath, API_PREFIX, INNER_PREFIX)) {
//                filterChain.doFilter(request, response);
//                return;
//            }
//            if (StringUtils.startsWith(servletPath, API_PREFIX)) {
//                BucSSOUser bucSSOUser = SimpleUserUtil.getBucSSOUser(request);
//                if (bucSSOUser == null || StringUtils.isAnyBlank(bucSSOUser.getEmpId(), bucSSOUser.getLastName())) {
//                    log.error("buc error ip:{} url:{} bucSSOUser:{}", request.getRemoteHost(), request.getRequestURI(), JSON.toJSONString(bucSSOUser));
//                    writeResp(response, Resp.fail("buc error"));
//                    return;
//                }
//                commonContext.setBucSSOUser(bucSSOUser);
//                commonContext.setEmpId(bucSSOUser.getEmpId());
//                commonContext.setEmpName(bucSSOUser.getLastName());
//            }
//            if (StringUtils.startsWith(servletPath, INNER_PREFIX)) {
//                String sign = request.getParameter(SIGN);
//                String source = request.getParameter(SOURCE);
//                String timestamp = request.getParameter(TIMESTAMP);
//                if (StringUtils.isAnyBlank(sign, source, timestamp)
//                        || NumberUtils.toLong(timestamp) < System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)
//                        || !secretKeyMap.containsKey(source)) {
//                    log.error("inner call error source:{} timestamp:{} sign:{}", source, timestamp, sign);
//                    writeResp(response, Resp.fail("inner call error"));
//                    return;
//                }
//                TreeMap<String, String> paramMap = Maps.newTreeMap();
//                for (Enumeration<String> paramNames = request.getParameterNames(); paramNames.hasMoreElements(); ) {
//                    String paramName = paramNames.nextElement();
//                    if (StringUtils.equals(paramName, SIGN)) {
//                        continue;
//                    }
//                    paramMap.put(paramName, request.getParameter(paramName));
//                }
//                String checkSign = buildSign(paramMap, secretKeyMap.get(source));
//                if (!StringUtils.equals(sign, checkSign)) {
//                    log.error("inner call sign error paramMap:{} sign:{} checkSign:{}", JSON.toJSONString(paramMap), sign, checkSign);
//                    writeResp(response, Resp.fail("inner call sign error"));
//                    return;
//                }
//                commonContext.setEmpId(StringUtils.defaultString(request.getParameter(EMP_ID), source));
//                commonContext.setEmpName(StringUtils.defaultString(request.getParameter(EMP_NAME), source));
//            }
//            CommonContext.put(commonContext);
//            filterChain.doFilter(request, response);
//        } finally {
//            MDC.clear();
//            CommonContext.remove();
//        }
//    }
//
//    private void writeResp(HttpServletResponse response, Resp<?> resp) throws IOException {
//        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        PrintWriter out = response.getWriter();
//        out.write(objectMapper.writeValueAsString(resp));
//    }
//}
