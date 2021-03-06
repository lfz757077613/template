package cn.laifuzhi.template;

import cn.laifuzhi.template.grpc.GrpcServer;
import cn.laifuzhi.template.matrix.DirectMemReporter;
import cn.laifuzhi.template.netty.NettyServer;
import cn.laifuzhi.template.service.DynamicConfigDBService;
import com.alibaba.druid.support.http.ResourceServlet;
import com.alibaba.druid.support.http.StatViewFilter;
import com.alibaba.druid.support.http.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static cn.laifuzhi.template.utils.Const.FilterName.COMMON_FILTER;

/*
                   _oo0oo_
                  o8888888o
                  88" . "88
                  (| -_- |)
                  0\  =  /0
                ___/'---'\___
              .' \ |     | / '.
             / \||||  :  ||||/ \
            / _||||| -:- |||||- \
           |   | \ \  -  / / |   |
           | \_|  ''\---/''  |_/ |
           \  .-\__  '-'  __/-.  /
         ___'. .'  /--.--\  '. .'___
      ."" '<  '.___\_<|>_/___.'  >' "".
     | | : ' - \'.:'\ _ /':.'/ - ' : | |
     \  \ '-.   \_ __\ /__ _/   .-' /  /
  ====='-.___'-.___\_____/___.-'___.-'=====
                   '=---='
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
           ????????????         ??????BUG
*/

/**
 * springboot???????????????filter
 * Mapping filters: characterEncodingFilter urls=[/*] order=-2147483648, formContentFilter urls=[/*] order=-9900, requestContextFilter urls=[/*] order=-105
 * spring???dispatcherServlet???url-pattern???/??????/*??????????????????servlet???????????????servlet
 * Mapping servlets: dispatcherServlet urls=[/]
 *
 * @EnableWebSocket???@EnableScheduling ?????????????????????spring.task.schedule*????????????(TaskSchedulingAutoConfiguration???????????????ThreadPoolTaskScheduler)
 * ScheduledTaskRegistrar????????????localExecutor???boot?????????????????????ThreadPoolTaskScheduler
 * https://github.com/spring-projects/spring-boot/issues/28449
 *
 * @PostConstruct ????????????????????????????????????????????????(DefaultSingletonBeanRegistry)??????????????????????????????????????????bean
 * https://lotabout.me/books/Java-Concurrency/Thread-Safety-Home-Work/Spring-Bean-Initialization.html
 * https://stackoverflow.com/questions/49742762/spring-instance-variable-visibility-in-new-thread-started-from-postconstruct
 * classpath???classpath*????????????????????????classes???lib??????????????????classpath*???????????????????????????????????????????????????classpath????????????????????????????????????????????????
 */
@Slf4j
@EnableWebSocket
@EnableScheduling
@EnableTransactionManagement(proxyTargetClass = true)
@SpringBootApplication(exclude = {
//        TaskSchedulingAutoConfiguration.class,
//        TaskExecutionAutoConfiguration.class,
//        NettyAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
//        RestTemplateAutoConfiguration.class,
//        GsonAutoConfiguration.class,
//        PersistenceExceptionTranslationAutoConfiguration.class,
})
//@MapperScan("cn.laifuzhi.template.dao")
// ?????????yml
@PropertySource(value = {"classpath:conf.properties"}, encoding = "UTF-8")
public class Application implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, WebMvcConfigurer, WebSocketConfigurer {
    private static volatile String APPLICATION_PATH;
    private static volatile boolean STARTED;
    private static volatile ConfigurableApplicationContext CONTEXT;

    public static void main(String[] args) {
        try {
            if (TimeZone.getDefault().getRawOffset() != TimeUnit.HOURS.toMillis(8)) {
                System.err.println("default timeZone error, timeZone:" + TimeZone.getDefault());
                return;
            }
            long start = System.currentTimeMillis();
            APPLICATION_PATH = new ApplicationHome(Application.class).getDir().getCanonicalPath();
            // boot 2.6????????????????????????
            CONTEXT = SpringApplication.run(Application.class, args);
            getBean(DynamicConfigDBService.class).start();
            getBean(DirectMemReporter.class).start();
            getBean(GrpcServer.class).start();
            getBean(NettyServer.class).start();
            log.info("start success cost:{}", System.currentTimeMillis() - start);
            STARTED = true;
            /*
              ?????????jvm????????????spring??????????????????shutdownHook???????????????
              springboot2.5.1??????????????????????????????spring?????????????????????????????????
              SpringApplication.getShutdownHandlers().add(()->{});
              SpringApplicationShutdownHook?????????jvm????????????????????????spring?????????????????????shutdownHandler
             */
//            ??????main??????????????????spring-boot-starter-web??????????????????web?????????????????????
            ReentrantLock lock = new ReentrantLock();
            Condition stopCondition = lock.newCondition();
            lock.lock();
            SpringApplication.getShutdownHandlers().add(() -> {
//                LoggingApplicationListener.registerShutdownHookIfNecessary?????????????????????????????????shutdownHandler
//                ????????????????????????shutdownHandler??????spring????????????????????????????????????
                lock.lock();
                stopCondition.signal();
                lock.unlock();
            });
            stopCondition.awaitUninterruptibly();
            // ??????????????????spring?????????logback
            lock.unlock();
        } catch (Throwable t) {
            t.printStackTrace();
            // ?????????spring???jvm??????????????????
            System.exit(-1);
        }
    }
    public static String applicationPath() {
        return APPLICATION_PATH;
    }

    public static boolean isStarted() {
        return STARTED;
    }

    public static boolean isSpringActive() {
        return CONTEXT != null && CONTEXT.isActive();
    }

    public static <T> T getBean(Class<T> type) {
        return CONTEXT.getBean(type);
    }

    /**
     * https://github.com/spring-projects/spring-boot/issues/28449
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
        return builder.build();
    }

    /*********************************************************************************************************
     * WebServerFactoryCustomizer??????tomcat???springboot?????????????????????
     * ?????????springboot?????????TomcatWebServerFactoryCustomizer???????????????tomcat?????????????????????????????????
     * ???????????????tomcat?????????????????????????????????????????????springboot??????????????????
     * https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure
     * Spring Boot uses that infrastructure internally to auto-configure the server.
     * Auto-configured WebServerFactoryCustomizer beans have an order of 0 and will be processed before any user-defined customizers,
     * unless it has an explicit order that states otherwise.
     ********************************************************************************************************/
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
//            ????????????????????????????????????10000??????????????????????????????
            connector.setMaxParameterCount(-1);
//            ??????????????????cookie???????????????200?????????????????????400 Bad Request
            connector.setMaxCookieCount(-1);
//            https://kb.globalscape.com/Knowledgebase/10691/What-is-the-difference-between-basic-auth-and-formbased-auth
//            FORM BASED Authentication?????????????????????session???post???????????????????????????4k????????????????????????????????????????????????
//            connector.setMaxSavePostSize((int) DataSize.ofKilobytes(4L).toBytes());
//            ??????????????????spring.mvc.async.request-timeout????????????servlet?????????????????????????????????????????????tomcat??????30s?????????????????????
//            connector.setAsyncTimeout();
        });
    }

    /*********************************************************************************************************
     * WebMvcConfigurer???????????????????????????url????????????
     * https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-web-applications.spring-mvc.auto-configuration
     ********************************************************************************************************/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("/static/test.html");
//        ??????freemarker??????
//        registry.addViewController("/").setViewName("/api/index");
    }

    // knife4j??????
    @Bean
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("RocketMQ???????????? API????????????")
                        .description("????????????/api???/inner????????????<br>" +
                                "/api?????????pc?????????????????????buc?????????????????????????????????????????????<br>" +
                                "/inner????????????????????????????????????????????????????????????????????????????????????????????????????????????")
                        .termsOfServiceUrl("https://mq-ops.aliyun-inc.com")
                        .contact(new Contact("?????????", "https://work.alibaba-inc.com/nwpipe/u/208799", "fuzhi.lfz@alibaba-inc.com"))
                        .version("1.0")
                        .build())
                .groupName("default")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.alibaba.messaging.ops2.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    // knife4j??????????????????????????????
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        ?????????????????????????????????????????????????????????boot???????????????????????????????????????????????????
//        registry.addInterceptor().addPathPatterns().excludePathPatterns();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
//        ???????????????????????????????????????controller????????????????????????????????????????????????springboot??????BasicErrorController?????????
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackageClass(getClass()));
    }

//    @Override
//    public void addFormatters(FormatterRegistry registry) {
//        registry.addConverter((Converter<String, String>) source -> HtmlUtils.htmlEscape(source, StandardCharsets.UTF_8.name()));
//    }

//    ?????????????????????????????????controller?????????@CrossOrigin
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/api/**");
//    }

    /*********************************************************************************************************
     * WebSocketConfigurer??????websocket???????????????websocket????????????
     * https://docs.spring.io/spring-framework/docs/5.3.9/reference/html/web.html#websocket
     ********************************************************************************************************/
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setAsyncSendTimeout(2000L);
        container.setMaxSessionIdleTimeout(2000L);
        container.setMaxTextMessageBufferSize(1024 * 1024 * 10);
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 10);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TextWebSocketHandler(), "/ws/chat")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    /*********************************************************************************************************
     * ???????????????????????????spring????????????DelegatingFilterProxyRegistrationBean?????????spring????????????FilterRegistrationBean
     ********************************************************************************************************/
    /**
     * @see cn.laifuzhi.template.filter.CommonFilter
     */
    @Bean
    public DelegatingFilterProxyRegistrationBean registerCommonFilter() {
        DelegatingFilterProxyRegistrationBean proxyRegistrationBean = new DelegatingFilterProxyRegistrationBean(COMMON_FILTER);
//        ????????????????????????????????????/*
//        proxyRegistrationBean.addUrlPatterns("/api/*");
//        ??????????????????????????????????????????spring?????????characterEncodingFilter/formContentFilter/requestContextFilter????????????
        proxyRegistrationBean.setOrder(0);
//        ????????????????????????servlet?????????true
//        proxyRegistrationBean.setAsyncSupported();
        return proxyRegistrationBean;
    }

    /**
     * @see com.alibaba.druid.support.http.StatViewFilter
     * https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_StatViewServlet%E9%85%8D%E7%BD%AE
     * https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_StatViewFilter
     */
    @Bean
    public FilterRegistrationBean<StatViewFilter> registerStatViewFilter() {
        FilterRegistrationBean<StatViewFilter> filterRegistration = new FilterRegistrationBean<>(new StatViewFilter());
        filterRegistration.setOrder(1);
        filterRegistration.addUrlPatterns("/druid/*");
        filterRegistration.addInitParameter(ResourceServlet.PARAM_NAME_USERNAME, "lfz");
        filterRegistration.addInitParameter(ResourceServlet.PARAM_NAME_PASSWORD, "miao");
        return filterRegistration;
    }

    /**
     * @see com.alibaba.druid.support.http.WebStatFilter
     * https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_%E9%85%8D%E7%BD%AEWebStatFilter
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> registerWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> filterRegistration = new FilterRegistrationBean<>(new WebStatFilter());
        filterRegistration.setOrder(2);
//        filterRegistration.addInitParameter(WebStatFilter.PARAM_NAME_EXCLUSIONS, "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        return filterRegistration;
    }
}
