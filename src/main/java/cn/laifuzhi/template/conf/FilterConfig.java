package cn.laifuzhi.template.conf;

import cn.laifuzhi.template.filter.CommonFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    private static FilterRegistrationBean<CommonFilter> registerCommonFilter(CommonFilter commonFilter) {
        FilterRegistrationBean<CommonFilter> registrationBean = new FilterRegistrationBean<>(commonFilter);
//        设置拦截路径，不设置默认/*
//        registrationBean.addUrlPatterns("/test");
//        设置执行顺序
//        registrationBean.setOrder();
        return registrationBean;
    }
}
