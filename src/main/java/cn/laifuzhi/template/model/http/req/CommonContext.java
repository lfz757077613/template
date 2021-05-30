package cn.laifuzhi.template.model.http.req;

import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Setter
@Getter
public class CommonContext {
    private transient HttpServletRequest servletReq;
    private transient HttpServletResponse servletResp;
    private String username;
    private String uri;
    private String token;
    private String refreshToken;
}
