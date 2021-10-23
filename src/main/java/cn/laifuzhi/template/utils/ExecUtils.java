package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;

/**
 * common-exec的异步执行，看源码设计的并不好，每次都新开线程
 * 所以想异步执行还是自己做吧，不要依赖common-exec
 */
@Slf4j
public final class ExecUtils {

    public static Tuple<Integer, String> exec(String cmd, long timeout) {
        Integer exitStatus = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(output));
            // 毫秒
            executor.setWatchdog(new ExecuteWatchdog(timeout));
            exitStatus = executor.execute(CommandLine.parse(cmd));
            log.info("exec exitStatus:{} cmd:{} output:{}", exitStatus, cmd, System.lineSeparator() + output);
        } catch (Exception e) {
            log.error("exec error, cmd:{} output:{}", cmd, System.lineSeparator() + output, e);
        }
        return new Tuple<>(exitStatus, output.toString());
    }

    public static Tuple<Integer, String> exec(String cmd, long timeout, int[] expectExitCodes) {
        Integer exitStatus = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            DefaultExecutor executor = new DefaultExecutor();
            // 默认0
            executor.setExitValues(expectExitCodes);
            executor.setStreamHandler(new PumpStreamHandler(output));
            // 毫秒
            executor.setWatchdog(new ExecuteWatchdog(timeout));
            exitStatus = executor.execute(CommandLine.parse(cmd));
            log.info("exec exitStatus:{} cmd:{} output:{}", exitStatus, cmd, System.lineSeparator() + output);
        } catch (Exception e) {
            log.error("exec error, cmd:{} output:{}", cmd, System.lineSeparator() + output, e);
        }
        return new Tuple<>(exitStatus, output.toString());
    }
}
