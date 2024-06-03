package com.proj.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.proj.invoice.bean.Orders;
import org.springframework.stereotype.Component;

@Component
public interface OrderMapper extends BaseMapper<Orders> {
}
