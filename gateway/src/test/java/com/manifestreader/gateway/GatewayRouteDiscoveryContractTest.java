package com.manifestreader.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GatewayRouteDiscoveryContractTest {

    @Test
    void routesUseServiceDiscoveryUris() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml).contains("lb://manifest-reader-auth");
        assertThat(applicationYaml).contains("lb://manifest-reader-admin");
        assertThat(applicationYaml).contains("lb://manifest-reader-user");
    }
}
