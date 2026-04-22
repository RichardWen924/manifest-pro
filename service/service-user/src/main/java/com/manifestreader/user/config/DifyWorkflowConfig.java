package com.manifestreader.user.config;

import com.manifestreader.user.properties.DifyWorkflowProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DifyWorkflowProperties.class)
public class DifyWorkflowConfig {
}
