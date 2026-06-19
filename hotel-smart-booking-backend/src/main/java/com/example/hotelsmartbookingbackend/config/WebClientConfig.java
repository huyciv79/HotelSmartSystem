package com.example.hotelsmartbookingbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cấu hình WebClient dùng để gọi Python AI Service.
 *
 * <p>WebClient là HTTP client non-blocking của Spring WebFlux,
 * thay thế RestTemplate (deprecated từ Spring 5+).
 * Mặc dù chúng ta block() trong service (do @Transactional),
 * WebClient vẫn cho phép timeout và error handling tốt hơn RestTemplate.
 *
 * <p>Dependency cần thêm vào pom.xml:
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter-webflux&lt;/artifactId&gt;
 *   &lt;/dependency&gt;
 * </pre>
 */
@Configuration
public class WebClientConfig {

    /**
     * Bean WebClient dùng chung cho toàn ứng dụng.
     * Cấu hình Content-Type mặc định là application/json.
     *
     * @return WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
