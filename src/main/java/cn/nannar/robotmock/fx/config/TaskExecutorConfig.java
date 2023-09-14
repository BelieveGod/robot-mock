package cn.nannar.robotmock.fx.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * springboot 全局的异步线程池配置
 * Created by pwf on 2019/2/16.
 */
@Configuration
@EnableAsync
public class TaskExecutorConfig implements AsyncConfigurer {
    @Autowired
    @Lazy
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        CustomizableThreadFactory customizableThreadFactory = new CustomizableThreadFactory("mockrobot-asyncpool-");
        customizableThreadFactory.setDaemon(true);
        executor.setThreadFactory(customizableThreadFactory);
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors()); //核心线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors()*2);  //最大线程数
        executor.setQueueCapacity(200); //队列大小
        //https://blog.csdn.net/medelia/article/details/89512726（there is no session with id 【xxxxx】）
        executor.setAllowCoreThreadTimeOut(true);//允许核心线程空闲时，过了一定的时间自动销毁
        executor.setKeepAliveSeconds(10); //设置线程空闲时间为1秒  1秒后便销毁
        // 设置线程池拒绝策略总共有4种 FIXME 这里今后可以设置为AbortPolicy(默认策略)，抛出并捕获异常或者自定义拒绝策略(自定义类并实现RejectedExecutionHandler接口)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//如果将拒绝策略改为：CallerRunsPolicy(即不用线程池中的线程执行，而是交给调用方来执行)
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return threadPoolTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }

}
