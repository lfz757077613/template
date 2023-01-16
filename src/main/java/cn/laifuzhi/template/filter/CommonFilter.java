package cn.laifuzhi.template.filter;

import cn.laifuzhi.template.utils.CommonContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static cn.laifuzhi.template.utils.Const.CommonReqHeader.X_REFRESH_TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonReqHeader.X_TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonRespHeader.X_SET_REFRESH_TOKEN;
import static cn.laifuzhi.template.utils.Const.CommonRespHeader.X_SET_TOKEN;
import static cn.laifuzhi.template.utils.Const.FilterName.COMMON_FILTER;
import static cn.laifuzhi.template.utils.Const.MDC_TRACE_ID;
import static cn.laifuzhi.template.utils.Const.MDC_UID;

/**
 * OncePerRequestFilter说明：https://www.baeldung.com/spring-onceperrequestfilter
 */
@Slf4j
@Component(COMMON_FILTER)
public class CommonFilter extends OncePerRequestFilter {
    // Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded())自动生成满足算法要求的秘钥key
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("LK/nh6Mfkdgb1oxJux84BT2RKKzaXVXDUC4LhTG73Bs=".getBytes());
    private static final JwtParser JWT_PARSER = Jwts.parserBuilder().setSigningKey(SECRET_KEY).build();

    @Resource
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            // 使用springboot中设置好的objectMapper直接返回结果，springmvc也是用这个objectMapper序列化的
//            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
//            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//            PrintWriter out = response.getWriter();
//            out.write(objectMapper.writeValueAsString(Resp.build(RespEnum.UNKNOWN_ERROR)));
            CommonContext commonContext = new CommonContext();
            commonContext.setServletReq(request);
            commonContext.setServletResp(response);
            String uid = parseJwt(request.getHeader(X_TOKEN));
            if (StringUtils.isBlank(uid)) {
                uid = parseJwt(request.getHeader(X_REFRESH_TOKEN));
                if (StringUtils.isNotBlank(uid)) {
                    log.info("refresh token uid:{}", uid);
                    response.setHeader(X_SET_TOKEN, createJWT(uid, 7));
                    response.setHeader(X_SET_REFRESH_TOKEN, createJWT(uid, 14));
                }
            }
            commonContext.setUid(uid);
            CommonContext.put(commonContext);
            if (StringUtils.isNotBlank(uid)) {
                MDC.put(MDC_UID, uid);
            }
            MDC.put(MDC_TRACE_ID, StringUtils.replace(UUID.randomUUID().toString(), "-", ""));
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            CommonContext.remove();
        }
    }

    private String createJWT(String uid, int expireDay) {
        return Jwts.builder()
                .setSubject(uid)
                .setExpiration(DateUtils.addDays(new Date(), expireDay))
                .signWith(SECRET_KEY)
                .compact();
    }

    private String parseJwt(String token) {
        try {
            if (StringUtils.isBlank(token)) {
                return StringUtils.EMPTY;
            }
            return JWT_PARSER.parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }
}
