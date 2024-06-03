package com.proj.invoice.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.proj.invoice.bean.OrderItem;
import com.proj.invoice.bean.RepositoryItem;
import com.proj.invoice.bean.allocate;
import com.proj.invoice.bean.allocateitems;
import com.proj.invoice.mapper.AllocateMapper;
import com.proj.invoice.mapper.RepositoryItemMapper;
import com.proj.invoice.mapper.RepositoryMapper;
import com.proj.invoice.service.allocateServiceImpl;
import com.proj.invoice.service.allocateitemsServiceImp;
import com.proj.invoice.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class allocateController {
    @Autowired
    allocateServiceImpl allocateService;
    @Autowired
    allocateitemsServiceImp allocateitemsServiceImp;
    @Autowired
    RepositoryItemMapper repositoryItemMapper;

    @Autowired
    RepositoryMapper repositoryMapper;

    @Autowired
    AllocateMapper allocateMapper;

    @PostMapping("/addAllocate")
    R addAllocate(@RequestBody Map<String,Object> map){
        try {
            allocate newAllocate=new allocate();
            newAllocate.setOperator(map.get("operator").toString());
            int src_id=(int)map.get("src_id");
            int dst_id= (int)map.get("dst_id");
            newAllocate.setSrcId(src_id);
            newAllocate.setDstId(dst_id);
            newAllocate.setTime(new Timestamp(System.currentTimeMillis()));
            List<allocateitems> newItems= JSONObject.parseArray(JSON.toJSONString(map.get("data")) , allocateitems.class);
            ArrayList<RepositoryItem> dstWill=new ArrayList<>(),dstWillInsert=new ArrayList<>(),srcWill=new ArrayList<>();
            for (allocateitems tmp:newItems) {
                long goodsID=tmp.getGoodsId();
                int num=tmp.getNum();
                RepositoryItem src=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("repository_id",src_id).eq("good_id",goodsID));
                RepositoryItem dst=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("repository_id",dst_id).eq("good_id",goodsID));
                if(src==null){
                    return R.error("源仓库未找到调配货品");
                }
                if(src.getNum()<num){
                    return R.error("货品数量不足以调配");
                }
                if(dst==null){
                    dst=new RepositoryItem();
                    dst.setNum(num);
                    dst.setGoodId(goodsID);
                    dst.setRepositoryId(dst_id);
                    dstWillInsert.add(dst);
                }
                else {
                    dst.setNum(dst.getNum()+num);
                    dstWill.add(dst);
                }
                src.setNum(src.getNum()-num);
                srcWill.add(src);
            }
            for (RepositoryItem tmp: dstWill) {
                repositoryItemMapper.updateById(tmp);
            }
            for (RepositoryItem tmp: dstWillInsert) {
                repositoryItemMapper.updateById(tmp);
            }
            for (RepositoryItem tmp: srcWill) {
                repositoryItemMapper.updateById(tmp);
            }
            allocateMapper.insert(newAllocate);
            int allocate_id=newAllocate.getId();
            for (allocateitems tmp: newItems) {
                tmp.setAllocate_id(allocate_id);
            }
            allocateitemsServiceImp.save(newItems);
            return R.ok();
        }catch (Exception e){
            e.printStackTrace();
            return R.error();
        }
    }
    @GetMapping("/allocate/getAll")
    R getAll(){
        return R.ok().data("items",allocateService.getAll());
    }
    @GetMapping("/allocate/detail/{id}")
    R getDetail(@PathVariable int id){
        return R.ok().data("items",allocateitemsServiceImp.getByAllocateID(id));
    }
}
