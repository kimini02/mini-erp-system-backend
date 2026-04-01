package com.minierp.backend;

import com.minierp.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BackendApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }
}
