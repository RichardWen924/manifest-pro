package com.manifest.auth.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.manifest.auth.mapper")
@ConditionalOnProperty(prefix = "manifest.auth.mybatis", name = "enabled", havingValue = "true")
public class MybatisMapperConfig {
}
