package cn.laifuzhi.template.utils;

public final class Const {
    public static final String MDC_TRACE_ID = "trace";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
    public static final String DEFAULT_DYNAMIC_CONFIG_KEY = "default";


    public static final class LockKey {
    }

    public static final class CommonReqHeader {
        public static final String USERNAME = "X-Username";
        public static final String TOKEN = "X-Token";
        public static final String REFRESH_TOKEN = "X-Refresh-Token";
    }

    public static final class CommonRespHeader {
        public static final String SET_TOKEN = "Set-Token";
        public static final String SET_REFRESH_TOKEN = "Set-Refresh-Token";
    }
}
