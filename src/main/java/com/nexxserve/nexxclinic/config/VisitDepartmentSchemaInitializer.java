package com.nexxserve.nexxclinic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VisitDepartmentSchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(VisitDepartmentSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public VisitDepartmentSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureVisitDepartmentColumns() {
        jdbcTemplate.execute("ALTER TABLE visit_departments ADD COLUMN IF NOT EXISTS status VARCHAR(16)");
        jdbcTemplate.execute("ALTER TABLE visit_departments ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP");
        jdbcTemplate.execute("UPDATE visit_departments SET status = 'PENDING' WHERE status IS NULL");
        jdbcTemplate.execute("ALTER TABLE visit_departments ALTER COLUMN status SET NOT NULL");
        logger.debug("Ensured visit_departments.status and visit_departments.completed_at columns exist.");
    }
}