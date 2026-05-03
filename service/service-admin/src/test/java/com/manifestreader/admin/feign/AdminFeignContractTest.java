package com.manifestreader.admin.feign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class AdminFeignContractTest {

    @Test
    void authFeignClientTargetsAuthServiceByName() {
        FeignClient feignClient = AuthFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-auth");
        assertThat(hasHttpMapping(AuthFeignClient.class)).isTrue();
    }

    @Test
    void userFeignClientTargetsUserServiceByName() {
        FeignClient feignClient = UserFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-user");
        assertThat(hasHttpMapping(UserFeignClient.class)).isTrue();
    }

    @Test
    void marketAdminFeignClientTargetsMarketServiceByName() {
        FeignClient feignClient = MarketAdminFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-market");
        assertThat(hasHttpMapping(MarketAdminFeignClient.class)).isTrue();
    }

    private boolean hasHttpMapping(Class<?> clientType) {
        return java.util.Arrays.stream(clientType.getMethods())
                .anyMatch(method -> method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class));
    }
}
