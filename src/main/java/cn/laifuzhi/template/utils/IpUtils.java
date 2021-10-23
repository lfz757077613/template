package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Slf4j
public final class IpUtils {
    // refer to RFC 1918
    // 10/8 prefix
    // 172.16/12 prefix
    // 192.168/16 prefix
    private static String innerIp = "";
    private static int pid = -1;

    static {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface anInterface = interfaces.nextElement();
                for (Enumeration<InetAddress> inetAddresses = anInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isSiteLocalAddress() && inetAddress instanceof Inet4Address) {
                        innerIp = inetAddress.getHostAddress();
                        log.info("get innerIp:{}", innerIp);
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("get innerIp error", e);
        }
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // format: "pid@hostname"
        String name = runtime.getName();
        try {
            pid = Integer.parseInt(name.substring(0, name.indexOf('@')));
            log.info("get pid:{}", pid);
        } catch (Exception e) {
            log.error("get pid error name:{}", name, e);
        }
    }

    public static String getInnerIp() {
        return innerIp;
    }

    public static int getPid() {
        return pid;
    }

    public static String getFrom() {
        return getInnerIp() + "@" + getPid();
    }
}
