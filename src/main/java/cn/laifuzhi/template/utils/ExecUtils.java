package com.alibaba.messaging.ops2.utils;

import cn.laifuzhi.template.model.MyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;

@Slf4j
public final class ExecUtils {
    private static final long DEFAULT_TIMEOUT_MS = 10000;


    public static String exec(String cmd) {
        return exec(cmd, DEFAULT_TIMEOUT_MS);
    }

    public static String exec(String cmd, long timeoutMs) {
        return exec(cmd, timeoutMs, null);
    }

    public static String exec(String cmd, long timeoutMs, int[] expectExitCodes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            DefaultExecutor executor = new DefaultExecutor();
            if (expectExitCodes != null) {
                executor.setExitValues(expectExitCodes);
            }
            executor.setStreamHandler(new PumpStreamHandler(output));
            executor.setWatchdog(new ExecuteWatchdog(timeoutMs));
            executor.execute(CommandLine.parse(cmd));
            return StringUtils.trim(output.toString());
        } catch (Exception e) {
            log.error("exec error, cmd:{} output:{}", cmd, System.lineSeparator() + StringUtils.trim(output.toString()), e);
            throw new MyException(String.format("cmd exec error cmd:%s timeout:%s result:%s", cmd, timeoutMs, StringUtils.trim(output.toString())), e);
        }
    }
}
