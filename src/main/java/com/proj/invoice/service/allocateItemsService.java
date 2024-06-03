package com.proj.invoice.service;

import com.proj.invoice.bean.allocateitems;

import java.util.List;

public interface allocateItemsService {
    List<allocateitems> getByAllocateID(int allocateID);
    boolean save(List<allocateitems> my);
}
