package com.coffiness.calfit.core.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
    value = "classpath:google-calendar-fallback.properties",
    ignoreResourceNotFound = true)
public class GoogleCalendarFallbackPropertyConfig {}
