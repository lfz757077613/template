//package com.alibaba.messaging.ops2.service;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.messaging.ops2.Application;
//import com.alibaba.messaging.ops2.conf.StaticConfig;
//import com.alibaba.messaging.ops2.dao.ops.AckInfoDao;
//import com.alibaba.messaging.ops2.model.OpsException;
//import com.alibaba.messaging.ops2.model.PO.ops.AckInfoPO;
//import com.alibaba.messaging.ops2.model.ack.HelmChart;
//import com.alibaba.messaging.ops2.model.ack.HelmHistory;
//import com.alibaba.messaging.ops2.model.ack.HelmInstallReq;
//import com.alibaba.messaging.ops2.model.ack.HelmRelease;
//import com.alibaba.messaging.ops2.model.ack.HelmUpgradeReq;
//import com.alibaba.messaging.ops2.model.ack.PodInfo;
//import com.alibaba.messaging.ops2.model.ack.StatefulSetInfo;
//import com.alibaba.messaging.ops2.utils.ExecUtils;
//import com.google.common.collect.Lists;
//import com.taobao.common.keycenter.security.Cryptograph;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.collections4.ListUtils;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.io.File;
//import java.io.IOException;
//import java.io.StringReader;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.TimeUnit;
//
//import static com.alibaba.messaging.ops2.utils.Const.HelmValues.IMAGE_MAIN_TAG;
//import static com.alibaba.messaging.ops2.utils.Const.HelmValues.REPLICA_COUNT;
//import static com.alibaba.messaging.ops2.utils.Utils.splitComma;
//import static com.alibaba.messaging.ops2.utils.Utils.splitHyphen;
//import static com.alibaba.messaging.ops2.utils.Utils.splitSpace;
//
//@Slf4j
//@Component
//public class AckService {
//    private static final String HELM_CMPUSH_PLUGIN_NAME = "cm-push";
//
//    private static final String HELM_PLUGIN_LIST = "%s plugin list";
//    private static final String HELM_INSTALL_CMPUSH_PLUGIN = "%s plugin install git://github.com/AliyunContainerService/helm-acr";
//    private static final String HELM_BROKER_REPO_ADD = "env HELM_REPO_USERNAME=%s HELM_REPO_PASSWORD=%s %s repo add broker acr://messaging-chart.cn-hangzhou.cr.aliyuncs.com/messaging/broker --username %s --password %s";
//    private static final String HELM_REPO_UPDATE = "%s repo update";
//    private static final String HELM_CHART_LIST = "%s search repo broker/broker -l -o json";
//    private static final String HELM_LIST = "%s list --kubeconfig %s -f 'vip|share' -d -r -o json";
//    private static final String HELM_INSTALL = "%s install --kubeconfig %s %s broker/broker --description %s --set broker.type=%s,broker.vipSpId=%s,image.main.tag=%s,region=%s,replicaCount=%s";
//    private static final String HELM_INSTALL_INTERNET = "%s install --kubeconfig %s %s broker/broker --description %s --set broker.isPublicTest=true,broker.type=%s,broker.vipSpId=%s,image.main.tag=%s,region=%s,replicaCount=%s,store.data.storageClass=alicloud-disk-topology-ssd,store.log.storageClass=alicloud-disk-topology-efficiency,vip.resourceGroupId=rg-aekz6s25qff646q,vip.type=internet";
//    private static final String HELM_UNINSTALL = "%s uninstall --kubeconfig %s %s";
//    private static final String HELM_GET_VALUES = "%s get values --kubeconfig %s %s";
//    private static final String HELM_UPGRADE = "%s upgrade --kubeconfig %s %s broker/broker --description %s --reuse-values %s --version %s";
//    private static final String HELM_HISTORY = "%s history --kubeconfig %s %s -o json";
//
//    private static final String KUBECTL_GET_STS_LIST = "%s get sts --kubeconfig %s -o wide --show-labels";
//    private static final String KUBECTL_GET_POD_LIST = "%s get pods --kubeconfig %s -l app.kubernetes.io/instance=%s,app.kubernetes.io/name=broker -o wide --show-labels";
//    private static final String KUBECTL_DESCRIBE_POD = "%s describe pod --kubeconfig %s %s";
//
//    @Resource
//    private Cryptograph cryptograph;
//    @Resource
//    private StaticConfig staticConfig;
//    @Resource
//    private AckInfoDao ackInfoDao;
//
//    private String helmPath;
//    private String kubectlPath;
//
//    @PostConstruct
//    private void init() throws IOException {
//        log.info("AckService init");
//        helmPath = Paths.get(Application.applicationPath(), "helm").toFile().getCanonicalPath();
//        kubectlPath = Paths.get(Application.applicationPath(), "kubectl").toFile().getCanonicalPath();
//        String helmPluginListResult = ExecUtils.exec(String.format(HELM_PLUGIN_LIST, helmPath));
//        if (!StringUtils.contains(helmPluginListResult, HELM_CMPUSH_PLUGIN_NAME)) {
//            log.info("install cm-push plugin {}", ExecUtils.exec(String.format(HELM_INSTALL_CMPUSH_PLUGIN, helmPath), TimeUnit.MINUTES.toMillis(10)));
//        }
//        log.info("add broker repo {}", ExecUtils.exec(String.format(HELM_BROKER_REPO_ADD,
//                staticConfig.getHelmUsername(),
//                cryptograph.decrypt(staticConfig.getHelmPassword(), staticConfig.getKeyCenterKeyName()),
//                helmPath,
//                staticConfig.getHelmUsername(),
//                cryptograph.decrypt(staticConfig.getHelmPassword(), staticConfig.getKeyCenterKeyName())), 10000));
//        log.info("AckService init success");
//    }
//
//    public List<HelmChart> helmChartVersionList() {
//        String helmChartResult = ExecUtils.exec(String.format(HELM_CHART_LIST, helmPath));
//        return ListUtils.emptyIfNull(JSON.parseArray(helmChartResult, HelmChart.class));
//    }
//
//    public List<HelmRelease> helmReleaseList(String region, String ackClusterName) {
//        String helmListResult = ExecUtils.exec(String.format(HELM_LIST, helmPath, getKubeconfigPath(region, ackClusterName)));
//        return ListUtils.emptyIfNull(JSON.parseArray(helmListResult, HelmRelease.class));
//    }
//
//    public String helmValues(String region, String ackClusterName, String brokerClusterName) {
//        return ExecUtils.exec(String.format(HELM_GET_VALUES, helmPath, getKubeconfigPath(region, ackClusterName), brokerClusterName));
//    }
//
//    public void install(HelmInstallReq req) {
//        ExecUtils.exec(String.format(HELM_REPO_UPDATE, helmPath));
//        String cmd = HELM_INSTALL;
//        if (req.isInternet()) {
//            cmd = HELM_INSTALL_INTERNET;
//        }
//        ExecUtils.exec(String.format(cmd, helmPath, getKubeconfigPath(req.getRegion(), req.getAckClusterName()),
//                req.getBrokerClusterName(), JSON.toJSONString(req), req.getBrokerType().getType(), req.getVipSpId(), req.getMainImageTag(), req.getRegion(), req.getReplicaCount()));
//    }
//
//    public void uninstall(String region, String ackClusterName, String brokerClusterName) {
//        ExecUtils.exec(String.format(HELM_UNINSTALL, helmPath, getKubeconfigPath(region, ackClusterName), brokerClusterName));
//    }
//
//    public void upgrade(HelmUpgradeReq req) {
//        ExecUtils.exec(String.format(HELM_REPO_UPDATE, helmPath));
//        List<String> settingList = Lists.newArrayList();
//        if (StringUtils.isNotBlank(req.getMainImageTag())) {
//            settingList.add(String.join("=", IMAGE_MAIN_TAG, req.getMainImageTag()));
//        }
//        if (req.getReplicaCount() != null) {
//            settingList.add(String.join("=", REPLICA_COUNT, String.valueOf(req.getReplicaCount())));
//        }
//        String settings = StringUtils.EMPTY;
//        if (CollectionUtils.isNotEmpty(settingList)) {
//            settings = String.join(" ", "--set", String.join(",", settingList));
//        }
//        ExecUtils.exec(String.format(HELM_UPGRADE, helmPath, getKubeconfigPath(req.getRegion(), req.getAckClusterName()),
//                req.getBrokerClusterName(), JSON.toJSONString(req), settings, req.getChartVersion()));
//    }
//
//    public List<HelmHistory> helmHistory(String region, String ackClusterName, String brokerClusterName) {
//        String helmHistoryResult = ExecUtils.exec(String.format(HELM_HISTORY, helmPath, getKubeconfigPath(region, ackClusterName), brokerClusterName));
//        List<HelmHistory> result = ListUtils.emptyIfNull(JSON.parseArray(helmHistoryResult, HelmHistory.class));
//        // 按照updated倒序
//        result.sort((history1, history2) -> history2.getUpdated().compareTo(history1.getUpdated()));
//        return result;
//    }
//
//    public List<StatefulSetInfo> statefulSetList(String region, String ackClusterName) {
//        try {
//            String statefulSetListResult = ExecUtils.exec(String.format(KUBECTL_GET_STS_LIST, kubectlPath, getKubeconfigPath(region, ackClusterName)));
//            List<String> lines = IOUtils.readLines(new StringReader(statefulSetListResult));
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<StatefulSetInfo> result = Lists.newArrayList();
//            if (CollectionUtils.size(lines) > 1) {
//                for (int i = 1; i < lines.size(); i++) {
//                    String line = lines.get(i);
//                    List<String> items = splitSpace(line);
//                    if (CollectionUtils.size(items) != 6) {
//                        log.error("statefulSetList format error region:{} ack:{} line:{}", region, ackClusterName, line);
//                        throw new OpsException("statefulSetList format error");
//                    }
//                    StatefulSetInfo statefulSetInfo = new StatefulSetInfo();
//                    statefulSetInfo.setName(items.get(0));
//                    statefulSetInfo.setReady(items.get(1));
//                    statefulSetInfo.setAge(items.get(2));
//                    statefulSetInfo.setContains(splitComma(items.get(3)));
//                    statefulSetInfo.setImages(splitComma(items.get(4)));
//                    statefulSetInfo.setLabels(splitComma(items.get(5)));
//                    result.add(statefulSetInfo);
//                }
//            }
//            return result;
//        } catch (OpsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("statefulSetList error region:{} ack:{}", region, ackClusterName, e);
//            throw new OpsException("statefulSetList error", e);
//        }
//    }
//
//    public List<PodInfo> podList(String region, String ackClusterName, String brokerClusterName) {
//        try {
//            String podListResult = ExecUtils.exec(String.format(KUBECTL_GET_POD_LIST, kubectlPath, getKubeconfigPath(region, ackClusterName), brokerClusterName));
//            List<String> lines = IOUtils.readLines(new StringReader(podListResult));
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<PodInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 10) {
//                    log.error("podList format error region:{} ack:{} mq:{} line:{}", region, ackClusterName, brokerClusterName, line);
//                    throw new OpsException("podList format error");
//                }
//                PodInfo podInfo = new PodInfo();
//                podInfo.setName(items.get(0));
//                podInfo.setReady(items.get(1));
//                podInfo.setStatus(items.get(2));
//                podInfo.setRestarts(items.get(3));
//                podInfo.setAge(items.get(4));
//                podInfo.setIp(items.get(5));
//                podInfo.setNode(items.get(6));
//                podInfo.setLabels(splitComma(items.get(9)));
//                result.add(podInfo);
//            }
//            result.sort((pod1, pod2) -> {
//                List<String> items1 = splitHyphen(pod1.getName());
//                List<String> items2 = splitHyphen(pod2.getName());
//                return Integer.parseInt(items1.get(items1.size() - 1)) - Integer.parseInt(items2.get(items2.size() - 1));
//            });
//            return result;
//        } catch (OpsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("podList error region:{} ack:{} mq:{}", region, ackClusterName, brokerClusterName, e);
//            throw new OpsException("podList error", e);
//        }
//    }
//
//    public String describePod(String region, String ackClusterName, String podName) {
//        return ExecUtils.exec(String.format(KUBECTL_DESCRIBE_POD, kubectlPath, getKubeconfigPath(region, ackClusterName), podName));
//    }
//
//    private String getKubeconfigPath(String region, String ackClusterName) {
//        try {
//            File kubeconfigFile = Paths.get(Application.applicationPath(), region, ackClusterName).toFile();
//            if (!kubeconfigFile.exists()) {
//                synchronized (this) {
//                    if (!kubeconfigFile.exists()) {
//                        Optional<AckInfoPO> ackInfoOptional = ackInfoDao.select(region, ackClusterName);
//                        if (!ackInfoOptional.isPresent()) {
//                            log.error("getKubeconfigPath not support region:{} ack:{}", region, ackClusterName);
//                            throw new OpsException(String.format("getKubeconfigPath not support region:%s ack:%s", region, ackClusterName));
//                        }
//                        Files.createDirectories(Paths.get(kubeconfigFile.getParent()));
//                        if (!kubeconfigFile.createNewFile()
//                                || !kubeconfigFile.setReadable(false, false)
//                                || !kubeconfigFile.setWritable(false, false)
//                                || !kubeconfigFile.setExecutable(false, false)
//                                || !kubeconfigFile.setReadable(true)
//                                || !kubeconfigFile.setWritable(true)) {
//                            log.error("getKubeconfigPath create file fail region:{} ack:{}", region, ackClusterName);
//                            throw new OpsException(String.format("getKubeconfigPath create file fail region:%s ack:%s", region, ackClusterName));
//                        }
//                        FileUtils.write(kubeconfigFile, ackInfoOptional.get().getKubeconfig(), StandardCharsets.UTF_8);
//                    }
//                }
//            }
//            return kubeconfigFile.getCanonicalPath();
//        } catch (OpsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("getKubeconfigPath error region:{} ack:{}", region, ackClusterName, e);
//            throw new OpsException("getKubeconfigPath error", e);
//        }
//    }
//}
