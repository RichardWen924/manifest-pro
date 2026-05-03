package com.manifestreader.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GatewayMarketRouteContractTest {

    @Test
    void gatewayHasMarketRoute() throws Exception {
        Path projectRoot = locateProjectRoot();
        String yaml = Files.readString(projectRoot.resolve("gateway/src/main/resources/application.yml"));
        assertThat(yaml).contains("lb://manifest-reader-market");
        assertThat(yaml).contains("Path=/market/**");
    }

    private static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) && Files.exists(current.resolve("gateway/src/main/resources/application.yml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }
}
