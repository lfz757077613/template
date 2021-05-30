package cn.laifuzhi.template.client;

import org.asynchttpclient.AsyncHttpClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class HttpClient {
    @Resource
    private AsyncHttpClient asyncHttpClient;
}
