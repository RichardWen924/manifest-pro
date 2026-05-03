package com.manifestreader.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MarketSchemaContractTest {

    @Test
    void sqlMigrationCreatesMarketplaceTables() throws Exception {
        Path projectRoot = locateProjectRoot();
        String sql = Files.readString(projectRoot.resolve("zfile/sql/V6__freight_market_init.sql"));
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_demand");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_quote");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_order");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS freight_order_timeline");
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
