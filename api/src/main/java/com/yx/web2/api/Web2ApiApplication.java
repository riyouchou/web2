package com.yx.web2.api;


import io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.yx.lib.job.annotation.EnableYxJob;

import java.util.TimeZone;

@Slf4j
@EnableDiscoveryClient
@SpringBootApplication(exclude = { OpenTelemetryAutoConfiguration.class })
@ServletComponentScan
@EnableYxJob
@EnableAsync
public class Web2ApiApplication {
    public static void main(String[] args) {
        log.info("Web2ApiApplicationMainStart");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(Web2ApiApplication.class, args);
    }
}