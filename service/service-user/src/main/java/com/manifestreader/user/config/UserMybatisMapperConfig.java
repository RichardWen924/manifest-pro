package com.manifestreader.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.manifestreader.user.mapper")
@ConditionalOnProperty(prefix = "manifest.user.mybatis", name = "enabled", havingValue = "true")
public class UserMybatisMapperConfig {
}
