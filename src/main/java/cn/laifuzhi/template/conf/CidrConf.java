//package cn.laifuzhi.template.conf;
//
//import com.alibaba.messaging.ops2.model.enumeration.BrokerEnvEnum;
//import com.google.common.collect.Sets;
//import org.apache.commons.net.util.SubnetUtils;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.util.Set;
//
//@Component
//public class CidrConf {
//    @Resource
//    private StaticConfig config;
//
//    private Set<SubnetUtils.SubnetInfo> kubeOneCidrSet = Sets.newHashSet();
//    private Set<SubnetUtils.SubnetInfo> ackCidrSet = Sets.newHashSet();
//
//    @PostConstruct
//    private void init() {
//        for (String kubeOneCidr : config.getKubeOneCidr()) {
//            kubeOneCidrSet.add(new SubnetUtils(kubeOneCidr).getInfo());
//        }
//        for (String ackCidr : config.getAckCidr()) {
//            ackCidrSet.add(new SubnetUtils(ackCidr).getInfo());
//        }
//    }
//
//    public BrokerEnvEnum bizType(Set<String> clusterIpSet) {
//        if (clusterIpSet.stream().allMatch(ip -> kubeOneCidrSet.stream().anyMatch(subnetInfo -> subnetInfo.isInRange(ip)))) {
//            return BrokerEnvEnum.ONS_KUBE_ONE;
//        }
//        if (clusterIpSet.stream().allMatch(ip -> ackCidrSet.stream().anyMatch(subnetInfo -> subnetInfo.isInRange(ip)))) {
//            return BrokerEnvEnum.ONS_ACK;
//        }
//        return BrokerEnvEnum.ONS_OXS;
//    }
//}
