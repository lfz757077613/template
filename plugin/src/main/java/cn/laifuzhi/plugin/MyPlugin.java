package cn.laifuzhi.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyPlugin extends Plugin {
    private static AnnotationConfigApplicationContext context;
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public MyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        long start = System.currentTimeMillis();
        log.info("MyPlugin starting");
//        默认applicationName为""，多个spring context在LiveBeansView.registerApplicationContext中会冲突
        context = new AnnotationConfigApplicationContext(){
            @Override
            public String getApplicationName() {
                return MyPlugin.class.getSimpleName();
            }
        };
//        setClassLoader一定要在scan或register前
        context.setClassLoader(getWrapper().getPluginClassLoader());
        context.setAllowCircularReferences(false);
        context.setAllowBeanDefinitionOverriding(false);
        context.scan(MyPlugin.class.getPackage().getName());
        context.refresh();
        log.info("MyPlugin started cost:{}", System.currentTimeMillis() - start);
    }

    @Override
    public void stop() {
        long start = System.currentTimeMillis();
        log.info("MyPlugin stopping");
        context.close();
        log.info("MyPlugin stopped cost:{}", System.currentTimeMillis() - start);
    }

    public static <T> T getBean(Class<T> tClass) {
        return context.getBean(tClass);
    }
}
