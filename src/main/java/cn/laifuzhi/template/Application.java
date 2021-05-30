package cn.laifuzhi.template;

import cn.laifuzhi.template.server.GrpcServer;
import cn.laifuzhi.template.server.NettyServer;
import cn.laifuzhi.template.service.DynamicConfigService;
import cn.laifuzhi.template.utils.DirectMemReporter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.GenericApplicationContext;

/*
                       _oo0oo_
                      o8888888o
                      88" . "88
                      (| -_- |)
                      0\  =  /0
                    ___/`---'\___
                  .' \\|     |// '.
                 / \\|||  :  |||// \
                / _||||| -:- |||||- \
               |   | \\\  -  /// |   |
               | \_|  ''\---/''  |_/ |
               \  .-\__  '-'  ___/-. /
             ___'. .'  /--.--\  `. .'___
          ."" '<  `.___\_<|>_/___.' >' "".
         | | :  `- \`.;`\ _ /`;.`/ - ` : | |
         \  \ `_.   \_ __\ /__ _/   .-` /  /
     =====`-.____`.___ \_____/___.-`___.-'=====
                       `=---='
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

               佛祖保佑         永无BUG
*/
@Slf4j
@SpringBootApplication
@MapperScan("cn.laifuzhi.template.dao")
@PropertySource(value = {"classpath:conf.properties"}, encoding = "UTF-8")
public class Application {
    @Getter
    private static boolean started;
    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        try {
            long start = System.currentTimeMillis();
            // 手动禁止循环依赖，springboot默认已经禁止了同名bean并提供了配置项，但是没提供循环依赖的配置项
            applicationContext = new SpringApplicationBuilder(Application.class)
                    .initializers((ApplicationContextInitializer<GenericApplicationContext>) applicationContext -> {
                        applicationContext.setAllowCircularReferences(false);
                    }).run(args);
            applicationContext.getBean(DynamicConfigService.class).start();
            applicationContext.getBean(DirectMemReporter.class).start();
            applicationContext.getBean(GrpcServer.class).start();
            applicationContext.getBean(NettyServer.class).start();
            started = true;
            log.info("start success cost:{}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("start error", e);
            System.exit(-1);
        }
    }

    public static boolean isSpringActive() {
        return applicationContext != null && applicationContext.isActive();
    }
}
