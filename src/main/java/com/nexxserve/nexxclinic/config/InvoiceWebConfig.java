package com.nexxserve.nexxclinic.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InvoiceWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path invoiceDir = Path.of("invoices").toAbsolutePath();
        registry.addResourceHandler("/invoices/**")
                .addResourceLocations(invoiceDir.toUri().toString());
    }
}
