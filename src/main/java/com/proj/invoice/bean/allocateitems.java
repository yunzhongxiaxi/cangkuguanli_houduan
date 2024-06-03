package com.proj.invoice.bean;

import com.baomidou.mybatisplus.annotation.TableId;

public class allocateitems {
    @TableId
    private int id;
    private int allocate_id;

    private int goodsId;
    private int num;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAllocate_id() {
        return allocate_id;
    }

    public void setAllocate_id(int allocate_id) {
        this.allocate_id = allocate_id;
    }
    public int getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(int goodsId) {
        this.goodsId = goodsId;
    }


    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
