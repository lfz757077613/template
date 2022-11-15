package cn.laifuzhi.template;

import cn.laifuzhi.template.grpc.GrpcServer;
import cn.laifuzhi.template.matrix.DirectMemReporter;
import cn.laifuzhi.template.netty.NettyServer;
import cn.laifuzhi.template.service.DynamicConfigDBService;
import com.alibaba.druid.support.http.ResourceServlet;
import com.alibaba.druid.support.http.StatViewFilter;
import com.alibaba.druid.support.http.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
            / _||||| -:- |||||_ \
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
           佛祖保佑         永无BUG
*/

/**
 * springboot默认添加的filter
 * Mapping filters: characterEncodingFilter urls=[/*] order=-2147483648, formContentFilter urls=[/*] order=-9900, requestContextFilter urls=[/*] order=-105
 * spring的dispatcherServlet的url-pattern是/不是/*，仅仅替换了servlet容器的默认servlet
 * Mapping servlets: dispatcherServlet urls=[/]
 *
 * @EnableWebSocket和@EnableScheduling 同时使用会造成spring.task.schedule*设置失效(TaskSchedulingAutoConfiguration不自动生成ThreadPoolTaskScheduler)
 * ScheduledTaskRegistrar只会使用localExecutor，boot建议自己初始化ThreadPoolTaskScheduler
 * https://github.com/spring-projects/spring-boot/issues/28449
 *
 * @PostConstruct 初始化成员变量和线程可见性的关系(DefaultSingletonBeanRegistry)，因此最好任何时候都使用单例bean
 * https://lotabout.me/books/Java-Concurrency/Thread-Safety-Home-Work/Spring-Bean-Initialization.html
 * https://stackoverflow.com/questions/49742762/spring-instance-variable-visibility-in-new-thread-started-from-postconstruct
 * classpath和classpath*都会查找打包后的classes和lib目录，只不过classpath*会加载所有符合要求的文件或文件夹，classpath只会加载第一个匹配的文件或文件夹
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
// 不支持yml
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
            // boot 2.6默认禁止循环依赖
            CONTEXT = SpringApplication.run(Application.class, args);
            getBean(DynamicConfigDBService.class).start();
            getBean(DirectMemReporter.class).start();
            getBean(GrpcServer.class).start();
            getBean(NettyServer.class).start();
            log.info("start success cost:{}", System.currentTimeMillis() - start);
            STARTED = true;
            /*
              增加在jvm关闭前，spring容器关闭后的shutdownHook，串行执行
              springboot2.5.1为了解决日志系统先于spring容器关闭问题引入该功能
              SpringApplication.getShutdownHandlers().add(()->{});
              SpringApplicationShutdownHook中注册jvm关闭钩子，先关闭spring容器再按序执行shutdownHandler
             */
//            避免main线程退出，有spring-boot-starter-web时可以不用，web容器有常驻线程
            ReentrantLock lock = new ReentrantLock();
            Condition stopCondition = lock.newCondition();
            lock.lock();
            SpringApplication.getShutdownHandlers().add(() -> {
//                LoggingApplicationListener.registerShutdownHookIfNecessary中增加了关闭日志系统的shutdownHandler
//                所以执行到自定义shutdownHandler时，spring容器和日志系统已经关闭了
                lock.lock();
                stopCondition.signal();
                lock.unlock();
            });
            stopCondition.awaitUninterruptibly();
            // 之后无法使用spring容器和logback
            lock.unlock();
        } catch (Throwable t) {
            t.printStackTrace();
            // 会触发spring的jvm关闭回调钩子
            System.exit(-1);
        }

//        Options options = new Options();
//        // 支持完整配置使用Option.builder()，正常情况直接使用下面的两种方法
//        Option option = Option.builder("a")
//                .longOpt("argA")
//                .argName("showName")
//                // 该参数是否有value
//                .hasArg(true)
//                // 有value的情况下，是否可以不传value
//                .optionalArg(false)
//                // 是否毕传参数
//                .required(false)
//                .build();
//        options.addOption(option);
//        options.addOption("b", "argB", true, "desc");
//        options.addRequiredOption("c", "argC", true, "desc");
//        CommandLineParser commandLineParser = new DefaultParser();
//        CommandLine commandLine = commandLineParser.parse(options, args);
//        System.out.println(commandLine.hasOption("a"));
//        System.out.println(commandLine.getOptionValue("a"));
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
     * WebServerFactoryCustomizer设置tomcat除springboot提供的其他参数
     * 先执行springboot默认的TomcatWebServerFactoryCustomizer设置嵌入式tomcat，再执行用户自定义设置
     * 自定义设置tomcat参数可以覆盖默认配置，或者配置springboot没开放的设置
     * https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure
     * Spring Boot uses that infrastructure internally to auto-configure the server.
     * Auto-configured WebServerFactoryCustomizer beans have an order of 0 and will be processed before any user-defined customizers,
     * unless it has an explicit order that states otherwise.
     ********************************************************************************************************/
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
//            不限制参数个数限制，默认10000，超过的参数取不到值
            connector.setMaxParameterCount(-1);
//            不限制请求中cookie数量，默认200，超过的话返回400 Bad Request
            connector.setMaxCookieCount(-1);
//            https://kb.globalscape.com/Knowledgebase/10691/What-is-the-difference-between-basic-auth-and-formbased-auth
//            FORM BASED Authentication时使用，存储到session的post消息体最大值，默认4k，一般用不到，因为都自己设计登录
//            connector.setMaxSavePostSize((int) DataSize.ofKilobytes(4L).toBytes());
//            如果没有设置spring.mvc.async.request-timeout，则异步servlet的默认超时取各自容器的默认值，tomcat默认30s，负数代表无限
//            connector.setAsyncTimeout();
        });
    }

    /*********************************************************************************************************
     * WebMvcConfigurer设置拦截器、视图、url统一前缀
     * https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-web-applications.spring-mvc.auto-configuration
     ********************************************************************************************************/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("/static/test.html");
//        跳到freemarker模板
//        registry.addViewController("/").setViewName("/api/index");
    }

    // knife4j设置
    @Bean
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("RocketMQ运维平台 API文档说明")
                        .description("同时支持/api、/inner接口前缀<br>" +
                                "/api接口给pc页面调用，对接buc，登录的且有权限的员工可以访问<br>" +
                                "/inner接口提供系统间调用，通过给不同应用分配不同秘钥，进行请求参数验签保证安全")
                        .termsOfServiceUrl("https://mq-ops.aliyun-inc.com")
                        .contact(new Contact("赖福智", "https://work.alibaba-inc.com/nwpipe/u/208799", "fuzhi.lfz@alibaba-inc.com"))
                        .version("1.0")
                        .build())
                .groupName("default")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.alibaba.messaging.ops2.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    // knife4j的页面入口和静态资源
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        新加拦截器，指定拦截路径和不拦截路径，boot做好了静态资源映射，不用管静态资源
//        registry.addInterceptor().addPathPatterns().excludePathPatterns();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
//        设置接口统一前缀，避免每个controller都手动设置前缀。限定包路径，避免springboot自带BasicErrorController受影响
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackageClass(getClass()));
    }

//    @Override
//    public void addFormatters(FormatterRegistry registry) {
//        registry.addConverter((Converter<String, String>) source -> HtmlUtils.htmlEscape(source, StandardCharsets.UTF_8.name()));
//    }

//    统一跨域设置，避免每个controller都用过@CrossOrigin
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/api/**");
//    }

    /*********************************************************************************************************
     * WebSocketConfigurer配置websocket接口，设置websocket容器参数
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
     * 拦截器相关配置，被spring管理的用DelegatingFilterProxyRegistrationBean，不用spring管理的用FilterRegistrationBean
     ********************************************************************************************************/
    /**
     * @see cn.laifuzhi.template.filter.CommonFilter
     */
    @Bean
    public DelegatingFilterProxyRegistrationBean registerCommonFilter() {
        DelegatingFilterProxyRegistrationBean proxyRegistrationBean = new DelegatingFilterProxyRegistrationBean(COMMON_FILTER);
//        设置拦截路径，不设置默认/*
//        proxyRegistrationBean.addUrlPatterns("/api/*");
//        设置执行顺序，越小越先执行，spring自带的characterEncodingFilter/formContentFilter/requestContextFilter都是负的
        proxyRegistrationBean.setOrder(0);
//        设置是否支持异步servlet，默认true
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
