package cn.laifuzhi.template.utils;

public final class Const {
    public static final String MDC_TRACE_ID = "trace";
    public static final String MDC_UID = "uid";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
    public static final String DEFAULT_DYNAMIC_CONFIG_KEY = "default";


    public static final class LockKey {
    }

    public static final class FilterName {
        public static final String COMMON_FILTER = "commonFilter";
    }

    public static final class CommonReqHeader {
        public static final String X_TOKEN = "X-Token";
        public static final String X_REFRESH_TOKEN = "X-Refresh-Token";
    }

    public static final class CommonRespHeader {
        public static final String X_SET_TOKEN = "X-Set-Token";
        public static final String X_SET_REFRESH_TOKEN = "X-Set-Refresh-Token";
    }
}
