package com.nexxserve.nexxclinic.service.invoice;

import com.nexxserve.nexxclinic.service.InvoicePdfGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Wires the Thymeleaf/Playwright renderer and view mapper into the static
 * {@link InvoicePdfGenerator} accessor so legacy callers don't need to change.
 */
@Component
public class InvoicePdfGeneratorConfig {

    private final InvoicePdfRenderer renderer;
    private final InvoiceViewMapper mapper;

    public InvoicePdfGeneratorConfig(InvoicePdfRenderer renderer, InvoiceViewMapper mapper) {
        this.renderer = renderer;
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        InvoicePdfGenerator.init(renderer, mapper);
    }
}
