package cn.laifuzhi.template.utils;

import cn.laifuzhi.template.model.MyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.laifuzhi.template.utils.Utils.splitSpace;

@Slf4j
public final class ExecUtils {
    private static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);

    public static String execQuota(String cmd) {
        return execQuota(cmd, DEFAULT_TIMEOUT_MS);
    }

    public static String execQuota(String cmd, long timeoutMs) {
        return exec(cmd, timeoutMs, null, true);
    }

    public static String exec(String cmd) {
        return exec(cmd, DEFAULT_TIMEOUT_MS);
    }

    public static String exec(String cmd, long timeoutMs) {
        return exec(cmd, timeoutMs, null, false);
    }

    public static String exec(String cmd, long timeoutMs, int[] expectExitCodes, boolean hasQuota) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            DefaultExecutor executor = new DefaultExecutor();
            if (expectExitCodes != null) {
                executor.setExitValues(expectExitCodes);
            }
            PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(out, err);
            pumpStreamHandler.setStopTimeout(1000);
            executor.setStreamHandler(pumpStreamHandler);
            executor.setWatchdog(new ExecuteWatchdog(timeoutMs));
            CommandLine commandLine = CommandLine.parse(cmd);
            if (hasQuota) {
                List<String> cmdList = splitSpace(cmd);
                commandLine = new CommandLine(cmdList.get(0));
                for (int i = 1; i < cmdList.size(); i++) {
                    commandLine.addArgument(cmdList.get(i), false);
                }
            }
            executor.execute(commandLine);
            return StringUtils.trim(out.toString());
        } catch (Exception e) {
            log.error("exec error, cmd:{} timeout:{} quota:{} err:{} out:{}", cmd, timeoutMs, hasQuota, StringUtils.trim(err.toString()), System.lineSeparator() + StringUtils.trim(out.toString()), e);
            throw new MyException(String.format("cmd exec error cmd:%s timeout:%s err:%s result:%s", cmd, timeoutMs, StringUtils.trim(err.toString()), StringUtils.trim(out.toString())), e);
        }
    }
}
