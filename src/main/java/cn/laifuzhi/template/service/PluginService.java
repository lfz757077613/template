package cn.laifuzhi.template.service;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.pf4j.AbstractPluginManager.DEFAULT_PLUGINS_DIR;

@Service
public class PluginService {
//    PluginManager不是线程安全的
    private PluginManager pluginManager;
    private List<Object> extensions;
    @PostConstruct
    private void init() throws IOException {
        pluginManager = new DefaultPluginManager(Paths.get(new ApplicationHome(getClass()).getDir().getCanonicalPath(), DEFAULT_PLUGINS_DIR));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
//        每次执行getExtensions获取的都是新的extension对象，可以使用SingletonExtensionFactory，不过没必要
        extensions = pluginManager.getExtensions(Object.class);
    }

    @PreDestroy
    private void destroy() {
        pluginManager.stopPlugins();
        pluginManager.unloadPlugins();
    }
}
