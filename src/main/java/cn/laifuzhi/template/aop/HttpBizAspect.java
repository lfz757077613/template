package cn.laifuzhi.template.aop;


import cn.laifuzhi.template.utils.CommonThreadLocal;
import cn.laifuzhi.template.model.http.req.CommonContext;
import cn.laifuzhi.template.model.http.resp.Resp;
import cn.laifuzhi.template.model.http.resp.RespEnum;
import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Optional;

import static cn.laifuzhi.template.utils.Const.CommonRespHeader.SET_REFRESH_TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonRespHeader.SET_TOKEN;

@Slf4j
@Aspect
@Component
public class HttpBizAspect {
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("LK/nh6Mfkdgb1oxJux84BT2RKKzaXVXDUC4LhTG73Bs=".getBytes());
    private static final JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(SECRET_KEY).build();

    @Around("@annotation(httpBiz)")
    public Object process(ProceedingJoinPoint pjp, HttpBiz httpBiz) throws Throwable {
        long start = System.currentTimeMillis();
        Optional<CommonContext> contextOptional = CommonThreadLocal.get();
        if (!contextOptional.isPresent()) {
            log.error("unknown error no commonContext");
            return Resp.build(RespEnum.UNKNOWN_ERROR);
        }
        CommonContext commonContext = contextOptional.get();
        HttpServletResponse servletResp = commonContext.getServletResp();
        if (httpBiz.needLogin() || !StringUtils.isAllBlank(httpBiz.role(), httpBiz.perm())) {
            if (StringUtils.isAnyBlank(commonContext.getToken(), commonContext.getRefreshToken())) {
                log.info("user no login commonContext:{}", JSON.toJSONString(commonContext));
                return Resp.build(RespEnum.NO_LOGIN);
            }
            String username = parseJwt(commonContext.getToken());
            if (username == null) {
                username = parseJwt(commonContext.getRefreshToken());
                if (username == null) {
                    log.info("user expired commonContext:{}", JSON.toJSONString(commonContext));
                    return Resp.build(RespEnum.NO_LOGIN);
                }
                log.info("user refresh token commonContext:{}", JSON.toJSONString(commonContext));
                servletResp.addHeader(SET_TOKEN, createJWT(username, 7));
                servletResp.addHeader(SET_REFRESH_TOKEN, createJWT(username, 14));
            }
            if (!StringUtils.equals(username, commonContext.getUsername())) {
                log.info("username not same jwtUsername:{}, commonContext:{}", username, JSON.toJSONString(commonContext));
                return Resp.build(RespEnum.ILLEGAL_PARAM);
            }
            if (StringUtils.isNotBlank(httpBiz.role())) {
                log.info("user no role commonContext:{}", JSON.toJSONString(commonContext));
                return Resp.build(RespEnum.NO_PERM);
            }
            if (StringUtils.isNotBlank(httpBiz.perm())) {
                log.info("user no perm commonContext:{}", JSON.toJSONString(commonContext));
                return Resp.build(RespEnum.NO_PERM);
            }
        }
        Object obj = pjp.proceed();
        StringBuilder sb = new StringBuilder(commonContext.getUri());
        sb.append(" commonContext:").append(JSON.toJSONString(commonContext));
        if (httpBiz.recordReq()) {
            sb.append(" param:").append(JSON.toJSONString(pjp.getArgs()));
        }
        if (httpBiz.recordResp()) {
            sb.append(" result:").append(JSON.toJSONString(obj));
        }
        sb.append(" cost:").append(System.currentTimeMillis() - start);
        log.info(sb.toString());
        return obj;
    }


    private String createJWT(String username, int expireDay) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(DateUtils.addDays(new Date(), expireDay))
                .signWith(SECRET_KEY)
                .compact();
    }

    private String parseJwt(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
