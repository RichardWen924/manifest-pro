package com.manifest.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuthDiscoveryContractTest {

    @Test
    void authServiceRegistersWithNacosDiscovery() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery");
        assertThat(pom).contains("spring-cloud-starter-loadbalancer");
        assertThat(applicationYaml).contains("discovery:");
        assertThat(applicationYaml).contains("register-enabled: ${NACOS_REGISTER_ENABLED:true}");
    }
}
