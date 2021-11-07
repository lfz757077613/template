package cn.laifuzhi.plugin;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
class MyPluginTest {
    private static AnnotationConfigApplicationContext context;
    @BeforeAll
    public static void init() {
        context = new AnnotationConfigApplicationContext(){
            @Override
            public String getApplicationName() {
                return MyPluginTest.class.getSimpleName();
            }
        };
        context.setAllowCircularReferences(false);
        context.setAllowBeanDefinitionOverriding(false);
        context.scan(MyPluginTest.class.getPackage().getName());
        context.refresh();
    }

    @AfterAll
    public static void destroy() {
        context.close();
    }

    @Test
    public void test() {
        log.info("MyService:{}", context.getBean(MyService.class));
    }
}