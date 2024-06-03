package com.proj.invoice.service;

import com.proj.invoice.bean.allocate;
import com.proj.invoice.mapper.AllocateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class allocateServiceImpl implements allocateService{
    @Autowired
    AllocateMapper allocateMapper;

    @Override
    public List<allocate> getAll() {
        System.out.println(allocateMapper.selectList(null).get(0).getTime());
        return allocateMapper.selectList(null);
    }

    @Override
    public boolean save(allocate my) {
        if(allocateMapper.insert(my)>0){
            return true;
        }
        return false;
    }
}
