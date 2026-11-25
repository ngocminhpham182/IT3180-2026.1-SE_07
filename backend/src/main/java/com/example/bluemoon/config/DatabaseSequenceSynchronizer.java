package com.example.bluemoon.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSequenceSynchronizer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Synchronizing database identity sequences...");
            String sql = """
                DO $$
                DECLARE
                    tbl text;
                    seq text;
                BEGIN
                    FOR tbl IN 
                        SELECT table_name 
                        FROM information_schema.tables 
                        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                    LOOP
                        seq := pg_get_serial_sequence(tbl, 'id');
                        IF seq IS NOT NULL THEN
                            EXECUTE format('SELECT setval(%L, COALESCE((SELECT MAX(id) FROM %I), 1))', seq, tbl);
                        END IF;
                    END LOOP;
                END $$;
                """;
            jdbcTemplate.execute(sql);
            log.info("Database identity sequences synchronized successfully.");
        } catch (Exception e) {
            log.warn("Could not synchronize database sequences: {}", e.getMessage());
        }
    }
}
