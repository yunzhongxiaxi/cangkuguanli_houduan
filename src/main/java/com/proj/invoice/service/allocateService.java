package com.proj.invoice.service;
import com.proj.invoice.bean.allocate;
import com.proj.invoice.mapper.AllocateMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface allocateService {
    List<allocate> getAll();
    boolean save(allocate my);
}
