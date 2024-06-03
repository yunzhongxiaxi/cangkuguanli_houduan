package com.proj.invoice.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.proj.invoice.bean.*;
import com.proj.invoice.mapper.*;
import com.proj.invoice.service.ItemService;
import com.proj.invoice.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class RepositoryItemController {
    @Autowired
    @Qualifier("ItemService")
    ItemService<RepositoryItem> itemService;
    @Autowired
    allocateItemsMapper allocateItemsMapper;
    @Autowired
    AllocateMapper allocateMapper;
    @Autowired
    RepositoryItemMapper repositoryItemMapper;
    @RequestMapping("/repositoryItem/deploy")
    public R deploy(@RequestParam long repAid,@RequestParam long repBid,@RequestParam long goodId,@RequestParam long num){
        RepositoryItem repositoryItem_A=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id",goodId).eq("repository_id",repAid));
        RepositoryItem repositoryItem_B=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id",goodId).eq("repository_id",repBid));
        boolean flag=repositoryItem_B==null;
        if(flag){
            repositoryItem_B=new RepositoryItem();
            repositoryItem_B.setRepositoryId(repBid);
            repositoryItem_B.setGoodId(goodId);
        }
        repositoryItem_A.setNum(repositoryItem_A.getNum()-num);
        if(repositoryItem_A.getNum()<0){
            return R.error("源库的库存不足");//.data("description","库存不足以调货"+String.valueOf(num));
        }
        repositoryItem_B.setNum(repositoryItem_B.getNum()+num);
        update(repositoryItem_A);
        if(!flag){
            update(repositoryItem_B);
        }
        else {
            add(repositoryItem_B);
        }
        return R.ok();
    }

    @Autowired
    GoodMapper goodMapper;
    @RequestMapping("/repositoryItem/input")
    public R input(@RequestParam long goodId,@RequestParam long num,@RequestParam long repId,@RequestParam String operator)
    {
        if(goodMapper.selectById(goodId)==null){
            return R.error("不存在该货品");
        }
        if(repositoryMapper.selectById(repId)==null){
            return R.error("仓库不存在");
        }
        RepositoryItem repositoryItem=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id",goodId).eq("repository_id",repId));
        if(repositoryItem==null){
            RepositoryItem repositoryItem_new=new RepositoryItem();
            repositoryItem_new.setRepositoryId(repId);
            repositoryItem_new.setNum(num);
            repositoryItem_new.setGoodId(goodId);
            repositoryItemMapper.insert(repositoryItem_new);
        }else{
            repositoryItem.setNum(repositoryItem.getNum()+num);
            update(repositoryItem);
        }
        allocate newallocate=new allocate();
        newallocate.setDstId((int) repId);
        newallocate.setTime(new Timestamp(System.currentTimeMillis()));
        newallocate.setOperator(operator);
        newallocate.setSrcId(-1);
        allocateMapper.insert(newallocate);
        allocateitems newallocateitems=new allocateitems();
        newallocateitems.setAllocate_id(newallocate.getId());
        newallocateitems.setGoodsId((int) goodId);
        newallocateitems.setNum((int) num);
        allocateItemsMapper.insert(newallocateitems);
        return R.ok();
    }

    @RequestMapping("/repositoryItem/getGoodsByRepId")
    public R getGoodsByRepId(@RequestParam long repId){
        List<RepositoryItem> list_items;
        Map<Long,Integer> isExist=new HashMap<>();
        list_items=repositoryItemMapper.selectList(new QueryWrapper<RepositoryItem>().eq("repository_id",repId));
        if(repId==-1){
            list_items=repositoryItemMapper.selectList(null);
        }
        List<GoodNew> list_goodNew=new ArrayList<>();
        for(RepositoryItem repositoryItem:list_items){
            Good good=goodMapper.selectById(repositoryItem.getGoodId());
            if(isExist.containsKey(good.getId())){
                int index=isExist.get(good.getId());
                long beforeNum=list_goodNew.get(index).getTotalNum();
                list_goodNew.set(index,new GoodNew(good.getId(),good.getName(),good.getDescription(),good.getInputPrice(),good.getRetailPrice(),good.getWholesalePrice(),repositoryItem.getNum()+beforeNum));
            }
            else {
                list_goodNew.add(new GoodNew(good.getId(),good.getName(),good.getDescription(),good.getInputPrice(),good.getRetailPrice(),good.getWholesalePrice(),repositoryItem.getNum()));
                isExist.put(good.getId(),list_goodNew.size()-1);
            }

        }
        return R.ok().data("items",list_goodNew);
    }
    
    @RequestMapping("/repositoryItem/add")
    public R add(@RequestBody RepositoryItem repositoryItem){
        return itemService.add(repositoryItem);
    }

    @RequestMapping("/repositoryItem/delById")
    public R del(@RequestParam long id){
        return itemService.del(id,new RepositoryItem());
    }

    @RequestMapping("/repositoryItem/delByRepositoryId")
    public R delByRepositoryId(@RequestParam long repositoryId){
        return itemService.del(repositoryId,"repository_id",new RepositoryItem());
    }

    @RequestMapping("/repositoryItem/update")
    public R update(@RequestBody RepositoryItem repositoryItem){
        return itemService.update(repositoryItem);
    }

    @RequestMapping("/repositoryItem/searchById")
    public R search(@RequestParam long id){
        return itemService.search(id,new RepositoryItem());
    }

    @RequestMapping("/repositoryItem/searchByRepositoryId")
    public R searchByRepositoryId(@RequestParam long repositoryId){
        return itemService.search(repositoryId,"repository_id",new RepositoryItem());
    }

    @RequestMapping("/repositoryItem/all")
    public R all(){
        return itemService.all(new RepositoryItem());
    }



    @RequestMapping("/repositoryItem/mdyStock")
    public R mdfStock(@RequestParam long repId,@RequestParam long goodId,@RequestParam long num)
    {
        RepositoryItem repositoryItem=repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("repository_id",repId).eq("good_id",goodId));
        if(repositoryItem!=null){
            repositoryItem.setNum(num);
            return update(repositoryItem);
        }
        return R.error();
    }

    @Autowired
    RepositoryMapper repositoryMapper;
    //TODO
    @RequestMapping("/repositoryItem/count")
    public R count(){
        List<Repository> list_repository=repositoryMapper.selectList(new QueryWrapper<Repository>());
        Map<Long,Long> map=new HashMap<>();
        for(Repository repository:list_repository){
            long repositoryId=repository.getRepositoryId();
            long count=0;
            List<RepositoryItem> list_repositoryItem=repositoryItemMapper.selectList(new QueryWrapper<RepositoryItem>().eq("repository_id",repositoryId));
            for(RepositoryItem repositoryItem:list_repositoryItem){
                count+=repositoryItem.getNum();
            }
            map.put(repositoryId,count);
        }
        return R.ok().data("items",map);
    }
}
