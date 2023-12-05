package cn.laifuzhi.template.utils;

import cn.laifuzhi.template.model.MyException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static cn.laifuzhi.template.utils.Const.IP_PORT_REGEXP;

@Slf4j
public final class Utils {
    /**
     * 参考PlatformDependent0中对Bits类的处理执行unaligned方法的过程
     */
    private int pageSize = AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
        try {
            Class<?> bitsClass = Class.forName("java.nio.Bits", false, PlatformDependent.getSystemClassLoader());
            Method pageSizeMethod = bitsClass.getDeclaredMethod("pageSize");
            pageSizeMethod.setAccessible(true);
            return (Integer) pageSizeMethod.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    /*
     * https://stackoverflow.com/questions/37560121/why-using-getfreespace-gettotalspace-getusablespace-gives-different-output-fr
     * |------------- free ----------|
     *           |-------usable------|----used-----|
     * |-reserve-|
     * |xxxxxxxxx|+++++++++++++++++++|=============|
     * |---------------- total --------------------|
     */
    public static double diskRadio(File file) {
        long totalSpace = file.getTotalSpace();
        long freeSpace = file.getFreeSpace();
        long usableSpace = file.getUsableSpace();
        return (0.0 + totalSpace - freeSpace) / (totalSpace - freeSpace + usableSpace);
    }

    private static final Joiner.MapJoiner MAP_JOINER_AND = Joiner.on("&").withKeyValueSeparator("=");
    private static final Joiner.MapJoiner MAP_JOINER_COMMA = Joiner.on(",").withKeyValueSeparator("=");
    private static final Joiner.MapJoiner MAP_JOINER_LINE = Joiner.on(System.lineSeparator()).withKeyValueSeparator("=");
    private static final Splitter.MapSplitter MAP_SPLITTER_LINE = Splitter.on(System.lineSeparator()).trimResults().omitEmptyStrings().withKeyValueSeparator("=");
    private static final Splitter SPLITTER_SEMICOLON = Splitter.on(';').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_DOT = Splitter.on('.').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_COLON = Splitter.on(':').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_TAB = Splitter.on('\t').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_HYPHEN = Splitter.on('-').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_AT = Splitter.on('@').trimResults().omitEmptyStrings();
    private static final Splitter SPLITTER_UNDERSCORE = Splitter.on('_').trimResults().omitEmptyStrings();

    public static String joinQueryString(TreeMap<String, String> paramMap) {
        Preconditions.checkArgument(MapUtils.isNotEmpty(paramMap));
        return MAP_JOINER_AND.join(paramMap);
    }

    public static String joinParamComma(TreeMap<String, String> paramMap) {
        Preconditions.checkArgument(MapUtils.isNotEmpty(paramMap));
        return MAP_JOINER_COMMA.join(paramMap);
    }

    public static String joinParamLine(TreeMap<String, String> paramMap) {
        if (MapUtils.isEmpty(paramMap)) {
            return StringUtils.EMPTY;
        }
        return MAP_JOINER_LINE.join(paramMap);
    }

    public static TreeMap<String, String> splitParamLine(String str) {
        if (StringUtils.isBlank(str)) {
            return Maps.newTreeMap();
        }
        return new TreeMap<>(MAP_SPLITTER_LINE.split(str));
    }

    public static List<String> splitSemicolon(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_SEMICOLON.splitToList(str);
    }

    public static List<String> splitDot(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_DOT.splitToList(str);
    }

    public static List<String> splitComma(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_COMMA.splitToList(str);
    }

    public static List<String> splitColon(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_COLON.splitToList(str);
    }

    public static List<String> splitTab(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_TAB.splitToList(str);
    }

    public static List<String> splitSpace(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_SPACE.splitToList(str);
    }

    public static List<String> splitHyphen(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_HYPHEN.splitToList(str);
    }

    public static List<String> splitAt(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_AT.splitToList(str);
    }

    public static List<String> splitUnderscore(String str) {
        Preconditions.checkArgument(str != null);
        return SPLITTER_UNDERSCORE.splitToList(str);
    }

    public static <T> int indexOf(T[] array, Predicate<T> predicate) {
        for (int i = 0; i < array.length; i++) {
            if (predicate.test(array[i])) {
                return i;
            }
        }
        return -1;
    }

    public static String getHost(String hostPort) {
        Preconditions.checkArgument(hostPort != null);
        return splitColon(hostPort).get(0);
    }

    public static String getIp(String ipPort) {
        Preconditions.checkArgument(Pattern.matches(IP_PORT_REGEXP, ipPort));
        return splitColon(ipPort).get(0);
    }

    public static int getPort(String ipPort) {
        Preconditions.checkArgument(Pattern.matches(IP_PORT_REGEXP, ipPort));
        return Integer.parseInt(splitColon(ipPort).get(1));
    }

    public static String buildSign(TreeMap<String, String> params, byte[] key) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey());
            sb.append(entry.getValue());
        }
        Mac hmac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_1, key);
        byte[] digest = hmac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(digest);
    }

    public static String generateKey(String algorithmName) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithmName);
            byte[] keyBytes = keyGen.generateKey().getEncoded();
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new MyException("generateKey error", e);
        }
    }
}
