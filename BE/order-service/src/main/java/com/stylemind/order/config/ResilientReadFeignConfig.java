package com.stylemind.order.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

// Applied only to CartClient/ProductClient via @FeignClient(configuration = ...)
// - both are safe to retry (idempotent GETs). NOT applied to PaymentClient:
// checkout()/processPayment() are not idempotent (a retried checkout would
// create a second Transaction row), so a transient failure there must surface
// as an error rather than being retried silently.
//
// Deliberately NOT annotated @Configuration: Feign registers this class into a
// per-client child context regardless, but if it also carried @Configuration it
// would be picked up by the main app's component scan (com.stylemind.order.**)
// and become a global Retryer applied to every Feign client, including
// PaymentClient - defeating the whole point of scoping this to reads only.
public class ResilientReadFeignConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 500, 3);
    }
}
