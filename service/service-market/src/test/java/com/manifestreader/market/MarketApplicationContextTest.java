package com.manifestreader.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MarketApplicationContextTest {

    @Test
    void serviceModuleHasNacosAndBootConfiguration() throws Exception {
        Path projectRoot = locateProjectRoot();
        String pom = Files.readString(projectRoot.resolve("service/service-market/pom.xml"));
        String yaml = Files.readString(projectRoot.resolve("service/service-market/src/main/resources/application.yml"));
        assertThat(pom).contains("spring-cloud-starter-openfeign");
        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery");
        assertThat(yaml).contains("manifest-reader-market");
    }

    private static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) && Files.exists(current.resolve("service/service-market/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }
}
