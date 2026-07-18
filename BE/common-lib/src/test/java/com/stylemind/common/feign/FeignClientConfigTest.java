package com.stylemind.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FeignClientConfigTest {

    @Test
    void internalRequestsUseTheXInternalTokenHeader() {
        FeignClientConfig config = new FeignClientConfig();
        ReflectionTestUtils.setField(config, "internalToken", "configured-token");

        RequestInterceptor interceptor = config.internalRequestInterceptor();
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("X-Internal-Token"))
                .containsExactly("configured-token");
    }
}
