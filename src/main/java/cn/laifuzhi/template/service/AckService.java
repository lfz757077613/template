//package com.alibaba.messaging.ops2.service.ack;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.messaging.ops2.Application;
//import com.alibaba.messaging.ops2.dao.AckInfoDao;
//import com.alibaba.messaging.ops2.model.OpsException;
//import com.alibaba.messaging.ops2.model.PO.AckInfoPO;
//import com.alibaba.messaging.ops2.model.ack.DeploymentInfo;
//import com.alibaba.messaging.ops2.model.ack.HelmChart;
//import com.alibaba.messaging.ops2.model.ack.HelmHistory;
//import com.alibaba.messaging.ops2.model.ack.HelmRelease;
//import com.alibaba.messaging.ops2.model.ack.PodInfo;
//import com.alibaba.messaging.ops2.model.ack.PvcInfo;
//import com.alibaba.messaging.ops2.model.ack.ServiceInfo;
//import com.alibaba.messaging.ops2.model.ack.StatefulSetInfo;
//import com.alibaba.messaging.ops2.utils.ExecUtils;
//import com.alibaba.messaging.ops2.utils.Tuple;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.collections4.ListUtils;
//import org.apache.commons.collections4.MapUtils;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.springframework.stereotype.Service;
//import org.yaml.snakeyaml.Yaml;
//
//import javax.annotation.Resource;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.StringReader;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.TreeMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReadWriteLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//import static com.alibaba.messaging.ops2.utils.Utils.joinParamComma;
//import static com.alibaba.messaging.ops2.utils.Utils.splitComma;
//import static com.alibaba.messaging.ops2.utils.Utils.splitHyphen;
//import static com.alibaba.messaging.ops2.utils.Utils.splitSpace;
//
//@Slf4j
//@Service
//public class AckService {
//    public static final String V5_CHART_NAMESPACE_NAME = "messaging-v5";
//    public static final String CHART_NAMESPACE_NAME = "messaging";
//
//    private static final String HELM_BROKER_REPO_ADD = "env HELM_REPO_USERNAME=%s HELM_REPO_PASSWORD=%s helm repo add %s acr://messaging-chart.cn-hangzhou.cr.aliyuncs.com/%s/%s --username %s --password %s";
//    private static final String HELM_REPO_UPDATE = "helm repo update %s";
//    private static final String HELM_CHART_LIST = "helm search repo %s/%s -l -o json";
//    private static final String HELM_LIST = "helm list --kubeconfig %s -d -r -o json -m 99999";
//    private static final String HELM_LIST_BY_FILTER = "helm list --kubeconfig %s -d -r -o json --filter ^%s$";
//    private static final String HELM_INSTALL = "helm install --kubeconfig %s %s %s/%s --description %s %s %s %s";
//    private static final String HELM_UNINSTALL = "helm uninstall --kubeconfig %s %s";
//    private static final String HELM_GET_VALUES = "helm get values --kubeconfig %s %s";
//    private static final String HELM_UPGRADE = "helm upgrade --kubeconfig %s %s %s/%s --description %s -f %s --version %s";
//    private static final String HELM_HISTORY = "helm history --kubeconfig %s %s -o json";
//
//    private static final String KUBECTL_GET_STS_LIST = "kubectl get sts --kubeconfig %s -l app.kubernetes.io/name=%s -o wide --show-labels";
//    private static final String KUBECTL_GET_STS = "kubectl get sts %s --kubeconfig %s -o wide --show-labels";
//    private static final String KUBECTL_GET_DEPLOY_LIST = "kubectl get deploy --kubeconfig %s -l app.kubernetes.io/name=%s -o wide --show-labels";
//    private static final String KUBECTL_GET_DEPLOY = "kubectl get deploy %s --kubeconfig %s -o wide --show-labels";
//    private static final String KUBECTL_GET_SVC_LIST = "kubectl get svc --kubeconfig %s -l app.kubernetes.io/instance=%s,app.kubernetes.io/name=%s -o wide --show-labels";
//    private static final String KUBECTL_GET_POD_LIST = "kubectl get pods --kubeconfig %s -l app.kubernetes.io/instance=%s,app.kubernetes.io/name=%s -o wide --show-labels";
//    private static final String KUBECTL_GET_PVC_LIST = "kubectl get pvc --kubeconfig %s -l app.kubernetes.io/instance=%s,app.kubernetes.io/name=%s -o wide --show-labels";
//    private static final String KUBECTL_DESCRIBE_POD = "kubectl describe pod --kubeconfig %s %s";
//    private static final String KUBECTL_DELETE_POD = "kubectl delete pod --kubeconfig %s %s --wait=false";
//    private static final String KUBECTL_DELETE_PVC = "kubectl delete pvc --kubeconfig %s -l app.kubernetes.io/instance=%s --wait=false";
//    private static final String KUBECTL_ROLLOUT_STATUS_DEPLOY = "kubectl rollout status deploy --kubeconfig %s %s";
//    private static final String KUBECTL_ROLLOUT_STATUS_STS = "kubectl rollout status sts --kubeconfig %s %s";
//    private static final String KUBECTL_PATCH_PVC = "kubectl patch pvc --kubeconfig %s %s -p {\"spec\":{\"resources\":{\"requests\":{\"storage\":\"%s\"}}}}";
//
//    @Resource
//    private AckInfoDao ackInfoDao;
//
//    private ConcurrentMap<String, ReentrantReadWriteLock> valuesLockMap = Maps.newConcurrentMap();
//    private ConcurrentMap<String, ReentrantReadWriteLock> kubeconfigLockMap = Maps.newConcurrentMap();
//
//    private Tuple<ReadWriteLock, AckInfoPO> kubeconfigTuple(String ackId) {
//        Optional<AckInfoPO> ackInfoOptional = ackInfoDao.select(ackId);
//        if (!ackInfoOptional.isPresent()) {
//            log.error("kubeconfigLock not support ack:{}", ackId);
//            throw new OpsException(String.format("kubeconfigLock not support ack:%s", ackId));
//        }
//        ReentrantReadWriteLock lock = kubeconfigLockMap.computeIfAbsent(ackId, ignore -> new ReentrantReadWriteLock());
//        return new Tuple<>(lock, ackInfoOptional.get());
//    }
//
//    /**
//     * 调用该方法的方法首先需要先获得读锁
//     * 通过锁升级锁降级保证使用kubeconfig文件执行helm或者kubectl命令的过程中，kubeconfig文件不会被写入
//     */
//    private String kubeconfigPathWithLock(ReadWriteLock lock, AckInfoPO ackInfoPO) {
//        String ackId = ackInfoPO.getAckId();
//        try {
//            File kubeconfigFile = Paths.get(Application.applicationPath(), "kubeconfig", ackId).toFile();
//            if (kubeconfigFile.exists()
//                    && StringUtils.equals(FileUtils.readFileToString(kubeconfigFile, StandardCharsets.UTF_8), ackInfoPO.getKubeconfig())) {
//                return kubeconfigFile.getCanonicalPath();
//            }
//            lock.readLock().unlock();
//            lock.writeLock().lock();
//            try {
//                if (kubeconfigFile.exists()
//                        && StringUtils.equals(FileUtils.readFileToString(kubeconfigFile, StandardCharsets.UTF_8), ackInfoPO.getKubeconfig())) {
//                    return kubeconfigFile.getCanonicalPath();
//                }
//                Files.createDirectories(Paths.get(kubeconfigFile.getParent()));
//                FileUtils.write(kubeconfigFile, ackInfoPO.getKubeconfig(), StandardCharsets.UTF_8);
//                if (!kubeconfigFile.setReadable(false, false)
//                        || !kubeconfigFile.setWritable(false, false)
//                        || !kubeconfigFile.setExecutable(false, false)
//                        || !kubeconfigFile.setReadable(true)
//                        || !kubeconfigFile.setWritable(true)) {
//                    log.error("kubeconfigPathWithLock file perm error ack:{}", ackId);
//                    throw new OpsException(String.format("kubeconfigPathWithLock file perm error ack:%s", ackId));
//                }
//                return kubeconfigFile.getCanonicalPath();
//            } finally {
//                lock.readLock().lock();
//                lock.writeLock().unlock();
//            }
//        } catch (OpsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("kubeconfigPathWithLock error ack:{}", ackId, e);
//            throw new OpsException("kubeconfigPathWithLock error", e);
//        }
//    }
//
//    protected synchronized String helmRepoAdd(String repoName, String chartNamespace, String helmUserName, String helmPassword) {
//        return ExecUtils.exec(String.format(HELM_BROKER_REPO_ADD,
//                helmUserName,
//                helmPassword,
//                repoName,
//                chartNamespace,
//                repoName,
//                helmUserName,
//                helmPassword));
//    }
//
//    protected void helmRepoUpdate(String repo) {
//        ExecUtils.exec(String.format(HELM_REPO_UPDATE, repo));
//    }
//
//    protected List<HelmChart> helmChartList(String repoName, String chartName) {
//        String helmChartResult = ExecUtils.exec(String.format(HELM_CHART_LIST, repoName, chartName));
//        return ListUtils.emptyIfNull(JSON.parseArray(helmChartResult, HelmChart.class));
//    }
//
//    protected List<HelmRelease> helmReleaseList(String ackId) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String helmListResult = ExecUtils.exec(String.format(HELM_LIST, kubeconfigPathWithLock(lock, ackInfoPO)));
//            return ListUtils.emptyIfNull(JSON.parseArray(helmListResult, HelmRelease.class));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public Optional<HelmRelease> helmRelease(String ackId, String releaseName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String helmListResult = ExecUtils.exec(String.format(HELM_LIST_BY_FILTER, kubeconfigPathWithLock(lock, ackInfoPO), releaseName));
//            List<HelmRelease> helmReleaseList = JSON.parseArray(helmListResult, HelmRelease.class);
//            if (CollectionUtils.size(helmReleaseList) > 1) {
//                throw new OpsException("helmRelease release gt 1 " + releaseName);
//            }
//            if (CollectionUtils.isEmpty(helmReleaseList)) {
//                return Optional.empty();
//            }
//            return Optional.of(helmReleaseList.get(0));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public String helmValues(String ackId, String releaseName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            return ExecUtils.exec(String.format(HELM_GET_VALUES, kubeconfigPathWithLock(lock, ackInfoPO), releaseName));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    protected void helmInstall(String ackId, String releaseName, String repoName, String chartName, String description, TreeMap<String, String> settingMap, TreeMap<String, String> stringSettingMap, String chartVersionParam) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String setting = StringUtils.EMPTY;
//            if (MapUtils.isNotEmpty(settingMap)) {
//                setting = String.join(StringUtils.SPACE, "--set", joinParamComma(settingMap));
//            }
//            String stringSetting = StringUtils.EMPTY;
//            if (MapUtils.isNotEmpty(stringSettingMap)) {
//                stringSetting = String.join(StringUtils.SPACE, "--set-string", joinParamComma(stringSettingMap));
//            }
//            String chartVersion = StringUtils.EMPTY;
//            if (StringUtils.isNotBlank(chartVersionParam)) {
//                chartVersion = String.join(StringUtils.SPACE, "--version", chartVersionParam);
//            }
//            ExecUtils.execQuota(
//                    String.format(HELM_INSTALL,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            releaseName,
//                            repoName,
//                            chartName,
//                            description,
//                            setting,
//                            stringSetting,
//                            chartVersion)
//            );
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public void helmUninstall(String ackId, String releaseName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.exec(String.format(HELM_UNINSTALL, kubeconfigPathWithLock(lock, ackInfoPO), releaseName));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    protected void helmUpgrade(String ackId, String releaseName, String repoName, String chartName, String description, String chartVersion, Consumer<Map<Object, Object>> valuesConsumer) {
//        String lockKey = String.join(":", ackId, releaseName);
//        ReentrantReadWriteLock releaseLock = valuesLockMap.computeIfAbsent(lockKey, ignore -> new ReentrantReadWriteLock());
//        releaseLock.writeLock().lock();
//        try {
//            if (StringUtils.isBlank(chartVersion)) {
//                Optional<HelmRelease> releaseOptional = helmRelease(ackId, releaseName);
//                if (!releaseOptional.isPresent()) {
//                    throw new OpsException("helmUpgrade release not exist " + releaseName);
//                }
//                chartVersion = StringUtils.remove(releaseOptional.get().getChart(), chartName + "-");
//            }
//            File valuesYaml = Paths.get(Application.applicationPath(), ackId, releaseName + ".yaml").toFile();
//            Yaml yaml = new Yaml();
//            String values = helmValues(ackId, releaseName);
//            Map<Object, Object> yamlMap = MapUtils.emptyIfNull(yaml.load(values));
//            valuesConsumer.accept(yamlMap);
//            Files.createDirectories(Paths.get(valuesYaml.getParent()));
//            FileUtils.write(valuesYaml, yaml.dumpAsMap(yamlMap), StandardCharsets.UTF_8);
//            Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//            ReadWriteLock kubeconfigLock = tuple.getT1();
//            AckInfoPO ackInfoPO = tuple.getT2();
//            kubeconfigLock.readLock().lock();
//            try {
//                ExecUtils.execQuota(
//                        String.format(HELM_UPGRADE,
//                                kubeconfigPathWithLock(kubeconfigLock, ackInfoPO),
//                                releaseName,
//                                repoName,
//                                chartName,
//                                description,
//                                valuesYaml.getCanonicalPath(),
//                                chartVersion)
//                );
//            } finally {
//                kubeconfigLock.readLock().unlock();
//            }
//        } catch (OpsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("helmUpgrade error release:{}", releaseName, e);
//            throw new OpsException("helmUpgrade error", e);
//        } finally {
//            releaseLock.writeLock().unlock();
//        }
//    }
//
//    public List<HelmHistory> helmHistory(String ackId, String releaseName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String helmHistoryResult = ExecUtils.exec(String.format(HELM_HISTORY, kubeconfigPathWithLock(lock, ackInfoPO), releaseName));
//            List<HelmHistory> result = ListUtils.emptyIfNull(JSON.parseArray(helmHistoryResult, HelmHistory.class));
//            // 按照updated倒序
//            result.sort(Comparator.comparing(HelmHistory::getUpdated).reversed());
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public List<StatefulSetInfo> ackStatefulSetList(String ackId, String nameLabel) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String statefulSetListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_STS_LIST,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            nameLabel)
//            );
//            List<String> lines = new BufferedReader(new StringReader(statefulSetListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<StatefulSetInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 6) {
//                    log.error("statefulSetList format error ack:{} line:{}", ackId, line);
//                    throw new OpsException("statefulSetList format error");
//                }
//                StatefulSetInfo statefulSetInfo = new StatefulSetInfo();
//                statefulSetInfo.setName(items.get(0));
//                statefulSetInfo.setReady(items.get(1));
//                statefulSetInfo.setAge(items.get(2));
//                statefulSetInfo.setContains(splitComma(items.get(3)));
//                statefulSetInfo.setImages(splitComma(items.get(4)));
//                statefulSetInfo.setLabels(splitComma(items.get(5)));
//                result.add(statefulSetInfo);
//            }
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    protected List<DeploymentInfo> ackDeploymentList(String ackId, String nameLabel) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String deployListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_DEPLOY_LIST,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            nameLabel)
//            );
//            List<String> lines = new BufferedReader(new StringReader(deployListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<DeploymentInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 9) {
//                    log.error("ackDeploymentList format error ack:{} line:{}", ackId, line);
//                    throw new OpsException("ackDeploymentList format error");
//                }
//                DeploymentInfo deploymentInfo = new DeploymentInfo();
//                deploymentInfo.setName(items.get(0));
//                deploymentInfo.setReady(items.get(1));
//                deploymentInfo.setUpToDate(items.get(2));
//                deploymentInfo.setAvailable(items.get(3));
//                deploymentInfo.setAge(items.get(4));
//                deploymentInfo.setContains(splitComma(items.get(5)));
//                deploymentInfo.setImages(splitComma(items.get(6)));
//                deploymentInfo.setSelector(splitComma(items.get(7)));
//                deploymentInfo.setLabels(splitComma(items.get(8)));
//                result.add(deploymentInfo);
//            }
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public Optional<StatefulSetInfo> ackStatefulSet(String ackId, String stsName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String statefulSetListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_STS,
//                            stsName,
//                            kubeconfigPathWithLock(lock, ackInfoPO))
//            );
//            List<String> lines = new BufferedReader(new StringReader(statefulSetListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Optional.empty();
//            }
//            StatefulSetInfo statefulSetInfo = new StatefulSetInfo();
//            String line = lines.get(1);
//            List<String> items = splitSpace(line);
//            if (CollectionUtils.size(items) != 6) {
//                throw new OpsException(String.format("statefulSetList format error ack:%s line:%s", ackId, line));
//            }
//            statefulSetInfo.setName(items.get(0));
//            statefulSetInfo.setReady(items.get(1));
//            statefulSetInfo.setAge(items.get(2));
//            statefulSetInfo.setContains(splitComma(items.get(3)));
//            statefulSetInfo.setImages(splitComma(items.get(4)));
//            statefulSetInfo.setLabels(splitComma(items.get(5)));
//            return Optional.of(statefulSetInfo);
//        } catch (Exception e) {
//            log.error("ackStatefulSet error", e);
//            return Optional.empty();
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public Optional<DeploymentInfo> ackDeployment(String ackId, String deployName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String deployListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_DEPLOY,
//                            deployName,
//                            kubeconfigPathWithLock(lock, ackInfoPO))
//            );
//            List<String> lines = new BufferedReader(new StringReader(deployListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Optional.empty();
//            }
//            DeploymentInfo deploymentInfo = new DeploymentInfo();
//            String line = lines.get(1);
//            List<String> items = splitSpace(line);
//            if (CollectionUtils.size(items) != 9) {
//                throw new OpsException(String.format("ackDeployment format error ack:%s line:%s", ackId, line));
//            }
//            deploymentInfo.setName(items.get(0));
//            deploymentInfo.setReady(items.get(1));
//            deploymentInfo.setUpToDate(items.get(2));
//            deploymentInfo.setAvailable(items.get(3));
//            deploymentInfo.setAge(items.get(4));
//            deploymentInfo.setContains(splitComma(items.get(5)));
//            deploymentInfo.setImages(splitComma(items.get(6)));
//            deploymentInfo.setSelector(splitComma(items.get(7)));
//            deploymentInfo.setLabels(splitComma(items.get(8)));
//            return Optional.of(deploymentInfo);
//        } catch (Exception e) {
//            log.error("ackDeployment error", e);
//            return Optional.empty();
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    protected List<ServiceInfo> ackServiceList(String ackId, String instanceLabel, String nameLabel) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String serviceListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_SVC_LIST,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            instanceLabel,
//                            nameLabel)
//            );
//            List<String> lines = new BufferedReader(new StringReader(serviceListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<ServiceInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 8) {
//                    log.error("ackServiceList format error ack:{} line:{}", ackId, line);
//                    throw new OpsException("ackServiceList format error");
//                }
//                ServiceInfo serviceInfo = new ServiceInfo();
//                serviceInfo.setName(items.get(0));
//                serviceInfo.setType(items.get(1));
//                serviceInfo.setClusterIp(items.get(2));
//                serviceInfo.setExternalIp(items.get(3));
//                serviceInfo.setPorts(splitComma(items.get(4)));
//                serviceInfo.setAge(items.get(5));
//                serviceInfo.setSelector(splitComma(items.get(6)));
//                serviceInfo.setLabels(splitComma(items.get(7)));
//                result.add(serviceInfo);
//            }
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    // us-west-1-share-01-1 2/2 Running 2 (2d23h ago) 3d 10.220.2.210 us-west-1.10.220.64.106 <none> <none> app.kubernetes.io/instance=us-west-1-share-01,app.kubernetes.io/name=broker,controller-revision-hash=us-west-1-share-01-b8964cf48,statefulset.kubernetes.io/pod-name=us-west-1-share-01-1
//    // 1.22以上ack restarts中可能有重启时间
//    protected List<PodInfo> ackPodList(String ackId, String instanceLabel, String nameLabel) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String podListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_POD_LIST,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            instanceLabel,
//                            nameLabel)
//            );
//            List<String> lines = new BufferedReader(new StringReader(podListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<PodInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 10 && CollectionUtils.size(items) != 12) {
//                    log.error("ackPodList format error ack:{} instanceLabel:{} line:{}", ackId, instanceLabel, line);
//                    throw new OpsException("ackPodList format error");
//                }
//                PodInfo podInfo = new PodInfo();
//                podInfo.setName(items.get(0));
//                podInfo.setReady(items.get(1));
//                podInfo.setStatus(items.get(2));
//                podInfo.setRestarts(items.get(3));
//                podInfo.setAge(items.get(items.size() - 6));
//                podInfo.setIp(items.get(items.size() - 5));
//                podInfo.setNode(items.get(items.size() - 4));
//                podInfo.setLabels(splitComma(items.get(items.size() - 1)));
//                result.add(podInfo);
//            }
//            result.sort(Comparator.comparing(podInfo -> {
//                List<String> items = splitHyphen(podInfo.getName());
//                return NumberUtils.toInt(items.get(items.size() - 1));
//            }));
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    protected List<PvcInfo> ackPvcList(String ackId, String instanceLabel, String nameLabel) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            String pvcListResult = ExecUtils.exec(
//                    String.format(KUBECTL_GET_PVC_LIST,
//                            kubeconfigPathWithLock(lock, ackInfoPO),
//                            instanceLabel,
//                            nameLabel)
//            );
//            List<String> lines = new BufferedReader(new StringReader(pvcListResult))
//                    .lines()
//                    .collect(Collectors.toList());
//            if (CollectionUtils.size(lines) <= 1) {
//                return Collections.emptyList();
//            }
//            List<PvcInfo> result = Lists.newArrayList();
//            for (int i = 1; i < lines.size(); i++) {
//                String line = lines.get(i);
//                List<String> items = splitSpace(line);
//                if (CollectionUtils.size(items) != 9) {
//                    log.error("ackPvcList format error ack:{} instanceLabel:{} line:{}", ackId, instanceLabel, line);
//                    throw new OpsException("ackPvcList format error");
//                }
//                PvcInfo pvcInfo = new PvcInfo();
//                pvcInfo.setName(items.get(0));
//                pvcInfo.setStatus(items.get(1));
//                pvcInfo.setVolume(items.get(2));
//                pvcInfo.setCapacity(items.get(3));
//                pvcInfo.setAccessModes(items.get(4));
//                pvcInfo.setStorageClass(items.get(5));
//                pvcInfo.setAge(items.get(6));
//                pvcInfo.setVolumeModes(items.get(7));
//                pvcInfo.setLabels(splitComma(items.get(8)));
//                result.add(pvcInfo);
//            }
//            return result;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public String describePod(String ackId, String podName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            return ExecUtils.exec(String.format(KUBECTL_DESCRIBE_POD, kubeconfigPathWithLock(lock, ackInfoPO), podName));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public void deletePod(String ackId, String podName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.exec(String.format(KUBECTL_DELETE_POD, kubeconfigPathWithLock(lock, ackInfoPO), podName));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public void deletePvc(String ackId, String instanceName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.exec(String.format(KUBECTL_DELETE_PVC, kubeconfigPathWithLock(lock, ackInfoPO), instanceName));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public void waitDeployFinish(String ackId, String deployName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.exec(
//                    String.format(KUBECTL_ROLLOUT_STATUS_DEPLOY, kubeconfigPathWithLock(lock, ackInfoPO), deployName),
//                    TimeUnit.MINUTES.toMillis(20)
//            );
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public void waitStsFinish(String ackId, String stsName) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.exec(
//                    String.format(KUBECTL_ROLLOUT_STATUS_STS, kubeconfigPathWithLock(lock, ackInfoPO), stsName),
//                    TimeUnit.MINUTES.toMillis(40)
//            );
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    // https://help.aliyun.com/document_detail/167551.html
//    public void pvcAddCapacity(String ackId, String pvcName, String capacity) {
//        Tuple<ReadWriteLock, AckInfoPO> tuple = kubeconfigTuple(ackId);
//        ReadWriteLock lock = tuple.getT1();
//        AckInfoPO ackInfoPO = tuple.getT2();
//        lock.readLock().lock();
//        try {
//            ExecUtils.execQuota(String.format(KUBECTL_PATCH_PVC, kubeconfigPathWithLock(lock, ackInfoPO), pvcName, capacity));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//}
