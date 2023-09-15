package cn.nannar.robotmock.fx.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author LTJ
 * @date 2023/9/15
 */

@Configuration
@MapperScan(basePackages = {"cn.nannar.robotmock.*.dao"})
public class MybatisPlusConfig {
}


