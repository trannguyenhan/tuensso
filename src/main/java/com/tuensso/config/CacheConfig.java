package com.tuensso.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    // Branding rarely changes; a short TTL keeps admin edits visible quickly
    // while avoiding a DB round-trip on every login page view.
    @Bean
    CacheManagerCustomizer<CaffeineCacheManager> brandingCacheCustomizer() {
        return cacheManager -> cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1000));
    }
}
