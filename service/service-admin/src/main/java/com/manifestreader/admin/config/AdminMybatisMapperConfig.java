package com.manifestreader.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.manifestreader.admin.mapper")
@ConditionalOnProperty(prefix = "manifest.admin.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminMybatisMapperConfig {
}
