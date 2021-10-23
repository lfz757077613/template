package cn.laifuzhi.template.RocketHttp.reactive;

import cn.laifuzhi.template.RocketHttp.model.RocketResponse;

public interface RocketDIYHandler {
    void onCompleted(RocketResponse response) throws Exception;

    void onThrowable(Throwable t);
}
