package com.example.bluemoon.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSequenceSynchronizerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSequenceSynchronizer synchronizer;

    @Test
    @DisplayName("Chạy đồng bộ sequence thành công khi ứng dụng khởi động")
    void run_executesSyncScript() {
        synchronizer.run(new DefaultApplicationArguments(new String[]{}));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    @DisplayName("Không gây crash ứng dụng nếu gặp lỗi kết nối database lúc đồng bộ")
    void run_catchesExceptionGracefully() {
        doThrow(new RuntimeException("DB Connection failed")).when(jdbcTemplate).execute(anyString());
        // Should not throw
        synchronizer.run(new DefaultApplicationArguments(new String[]{}));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}
