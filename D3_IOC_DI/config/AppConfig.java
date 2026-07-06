package com.cds.javaweb.D3_IOC_DI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring 配置类 — 演示显式 Bean 注册
 *
 * @Configuration vs @Component：
 * - 关系：@Configuration 源码上有 @Component，是它的子类型
 * - 区别：@Configuration 有 CGLIB 代理，@Bean 方法互调时会拦截 → 返回容器中的单例
 *          @Component 无代理，@Bean 方法互调 = 普通方法调用 → 每次 new 新对象
 * - 结论：有 @Bean 方法必须用 @Configuration（保证单例语义，不是性能问题）
 *
 * 两种注册 Bean 的方式：
 * ① 隐式扫描：@Component / @Service / @Repository / @Controller
 * ② 显式声明：@Configuration + @Bean（适合第三方对象，如 RestTemplate）
 */
@Configuration
public class AppConfig {

    /** 注册 singleton Bean，方法名 = Bean 名称 */
    @Bean
    public String serverStartTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** 注册 prototype Bean，每次获取都创建新实例 */
    @Bean
    @Scope("prototype")
    public String appInfo() {
        return "D3_分层解耦项目 — IoC/DI/Bean 演示应用";
    }
}
