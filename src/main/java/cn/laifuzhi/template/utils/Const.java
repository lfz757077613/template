package cn.laifuzhi.template.utils;

public final class Const {
    public static final String IP_PORT_REGEXP = "^((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5]):(\\d|[1-9]\\d|[1-9]\\d{2}|[1-9]\\d{3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
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
