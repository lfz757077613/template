package cn.laifuzhi.template.client;

import cn.laifuzhi.template.model.MyException;
import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkHttp {
    private static final MediaType JSON_TYPE = MediaType.get(HttpHeaderValues.APPLICATION_JSON.toString());
    private OkHttpClient client;

    @PostConstruct
    private void init() {
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .writeTimeout(Duration.ofSeconds(2))
                .callTimeout(Duration.ofSeconds(5))
                .followRedirects(false)
                .build();
        client.dispatcher().setMaxRequests(1024);
        client.dispatcher().setMaxRequestsPerHost(512);
    }

    // https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/ 说明了okhttp的shutdown
    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("OkHttp shutdown ...");
        client.dispatcher().executorService().shutdown();
        while (!client.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("OkHttp executorService shutdown ...");
        }
        client.connectionPool().evictAll();
        log.info("OkHttp shutdown finished");
    }

    public String get(String url, Map<String, String> param) {
        HttpUrl.Builder builder = HttpUrl.get(url).newBuilder();
        if (MapUtils.isNotEmpty(param)) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        Request request = new Request.Builder()
                .url(builder.build())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                log.error("get body null code:{} url:{} param:{}", response.code(), url, JSON.toJSONString(param));
                throw new MyException("okhttp get body null");
            }
            String result = response.body().string();
            if (!response.isSuccessful()) {
                log.error("get not success code:{} url:{} param:{} result:{}", response.code(), url, JSON.toJSONString(param), result);
                throw new MyException("okhttp get not success code:" + response.code());
            }
            return response.body().string();
        } catch (MyException e) {
            throw e;
        } catch (Exception e) {
            log.error("get error url:{} param:{}", url, JSON.toJSONString(param), e);
            throw new MyException("okhttp get error", e);
        }
    }

    public String postJson(String url, String body) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, JSON_TYPE))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                log.error("postJson body null code:{} url:{} param:{}", response.code(), url, JSON.toJSONString(body));
                throw new MyException("okhttp postJson body null");
            }
            String result = response.body().string();
            if (!response.isSuccessful()) {
                log.error("postJson not success code:{} url:{} param:{} result:{}", response.code(), url, body, result);
                throw new MyException("okhttp postJson not success code:" + response.code());
            }
            return result;
        } catch (MyException e) {
            throw e;
        } catch (Exception e) {
            log.error("postJson error url:{} body:{}", url, body, e);
            throw new MyException("okhttp postJson error", e);
        }
    }

    public String postForm(String url, Map<String, String> param) {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (MapUtils.isNotEmpty(param)) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                bodyBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        Request request = new Request.Builder()
                .url(url)
                .post(bodyBuilder.build())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                log.error("postForm body null code:{} url:{} param:{}", response.code(), url, JSON.toJSONString(param));
                throw new MyException("okhttp postForm body null");
            }
            String result = response.body().string();
            if (!response.isSuccessful()) {
                log.error("postForm not success code:{} url:{} param:{} result:{}", response.code(), url, JSON.toJSONString(param), result);
                throw new MyException("okhttp postForm not success code:" + response.code());
            }
            return result;
        } catch (MyException e) {
            throw e;
        } catch (Exception e) {
            log.error("postForm error url:{} body:{}", url, JSON.toJSONString(param), e);
            throw new MyException("okhttp postForm error", e);
        }
    }
}
