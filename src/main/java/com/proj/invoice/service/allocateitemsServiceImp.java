package com.proj.invoice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.proj.invoice.bean.allocate;
import com.proj.invoice.bean.allocateitems;
import com.proj.invoice.mapper.allocateItemsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class allocateitemsServiceImp implements allocateItemsService{
    @Autowired
    allocateItemsMapper allocateItemsMapper;

    @Override
    public List<allocateitems> getByAllocateID(int allocateID) {
        return allocateItemsMapper.selectList(new QueryWrapper<allocateitems>().eq("allocate_id",allocateID));
    }

    @Override
    public boolean save(List<allocateitems> my) {
        int count=0;
        for (allocateitems tmp:my) {
            if(allocateItemsMapper.insert(tmp)>0){
                count++;
            }
        }
        return count > 0;
    }
}
