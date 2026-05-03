package com.manifestreader.user.feign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class UserFeignContractTest {

    @Test
    void authIdentityFeignClientTargetsAuthServiceByName() {
        FeignClient feignClient = AuthIdentityFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-auth");
        assertThat(hasHttpMapping(AuthIdentityFeignClient.class)).isTrue();
    }

    @Test
    void llmTaskFeignClientTargetsLlmTaskServiceByName() {
        FeignClient feignClient = LlmTaskFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-llm-task");
        assertThat(hasHttpMapping(LlmTaskFeignClient.class)).isTrue();
    }

    @Test
    void marketUserFeignClientTargetsMarketServiceByName() {
        FeignClient feignClient = MarketUserFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("manifest-reader-market");
        assertThat(hasHttpMapping(MarketUserFeignClient.class)).isTrue();
    }

    private boolean hasHttpMapping(Class<?> clientType) {
        return java.util.Arrays.stream(clientType.getMethods())
                .anyMatch(method -> method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class));
    }
}
