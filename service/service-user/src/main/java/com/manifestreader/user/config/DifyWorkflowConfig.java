package com.manifestreader.user.config;

import com.manifestreader.user.properties.DifyWorkflowProperties;
import com.manifestreader.user.properties.ObjectStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({DifyWorkflowProperties.class, ObjectStorageProperties.class})
public class DifyWorkflowConfig {
}
