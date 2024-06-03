package com.proj.invoice;

import com.proj.invoice.mapper.AllocateMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InvoiceApplicationTests {

    @Autowired
    AllocateMapper allocateMapper;
    @Test
    void contextLoads() {
    }

}
