package com.proj.invoice.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.proj.invoice.bean.*;
import com.proj.invoice.mapper.*;
import com.proj.invoice.service.ItemService;
import com.proj.invoice.utils.R;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@CrossOrigin
public class OrderController {
    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderItemMapper orderItemMapper;

    @Autowired
    GoodMapper goodMapper;

    @Autowired
    RepositoryItemMapper repositoryItemMapper;

    @Autowired
    RepositoryMapper repositoryMapper;

    @Autowired
    CustomerMapper customerMapper;

    @RequestMapping("/order/cash")
    public R cash(@RequestBody Map<String, Object> map) {
        /*ObjectMapper mapper = new ObjectMapper();
        List<OrderItem> list = mapper.convertValue((List<OrderItem>)map.get("orderItems"), new TypeReference<List<OrderItem>>() { });*/
        String json = JSON.toJSONString((List<OrderItem>) map.get("orderItems"));
        List<OrderItem> list = JSONObject.parseArray(json, OrderItem.class);
        //long customerId=Integer.parseInt((String)map.get("customerId"));
        LocalDateTime time = LocalDateTime.now();
        double profit = 0;
        double total = 0;

        Orders Orders = new Orders();
        Orders.setState("待支付");
        //aOrder.setCustomerId(customerId);
        Orders.setTime(time);

        Iterator<OrderItem> it = list.iterator();
        while (it.hasNext()) {
            OrderItem orderItem = it.next();
            Good good = goodMapper.selectById(orderItem.getGoodId());
            total += good.getRetailPrice() * orderItem.getNum();
            profit += (good.getRetailPrice() - good.getInputPrice()) * orderItem.getNum();
        }
        Orders.setTotal(total);
        Orders.setProfit(profit);
        orderMapper.insert(Orders);
        long orderId = Orders.getOrderId();
        for (OrderItem orderItem : list) {
            orderItem.setOrderId(orderId);
            orderItemMapper.insert(orderItem);
        }
        return R.ok().data("item", Orders);
    }

    @RequestMapping("/order/pay")
    public R pay(@RequestParam long orderId) {
        if (orderMapper.selectById(orderId) == null) {
            return R.error();
        }
        updateState(orderId, "已完成");
        List<OrderItem> list_items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
        for (OrderItem orderItem : list_items) {
            RepositoryItem repositoryItem = repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("repository_id", 2).eq("good_id", orderItem.getGoodId()));
            repositoryItem.setNum(repositoryItem.getNum() - orderItem.getNum());
            repositoryItemMapper.updateById(repositoryItem);
        }
        return R.ok();
    }

    //获取所有销售单
    @GetMapping("/order/all")
    public R getAllOrder() {
        List<Orders> list = orderMapper.selectList(null);
        R r = R.ok().data("items", list);
        return r;
    }

    //更新销售单状态
    @GetMapping("/order/updateState")
    public R updateState(@RequestParam long order_id, @RequestParam String newState) {
        Orders orders = orderMapper.selectById(order_id);
        if (orders.getState().equals("待审核") && newState.equals("已保存")) {
            for (OrderItem orderItem : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order_id))) {
                RepositoryItem repositoryItem = repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id", orderItem.getGoodId()));
                repositoryItem.setNum(repositoryItem.getNum() + orderItem.getNum());
                repositoryItemMapper.updateById(repositoryItem);
            }
        }
        if (orders.getState().equals("已完成") && newState.equals("已退货")) {
            for (OrderItem orderItem : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order_id))) {
                RepositoryItem repositoryItem = repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id", orderItem.getGoodId()));
                repositoryItem.setNum(repositoryItem.getNum() + orderItem.getNum());
                repositoryItemMapper.updateById(repositoryItem);
            }
        }
        orders.setState(newState);
        int i = orderMapper.updateById(orders);
        if (i > 0) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    //
    @GetMapping("/order/getGoodsById")
    public R getGoodsById(@RequestParam long order_id) {
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", order_id);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        List<GoodNew> goods = new ArrayList<>();
        for (OrderItem orderItem : items) {
            Good good = goodMapper.selectById(orderItem.getGoodId());
            GoodNew goodNew = new GoodNew(good.getId(), good.getName(), good.getDescription(), good.getInputPrice(), good.getRetailPrice(), good.getWholesalePrice(), orderItem.getNum());
            goods.add(goodNew);
        }
        return R.ok().data("items", goods);
    }

    @PostMapping("/order/save")
    public R saveOrder(@RequestBody Map<String, Object> map) {
        long id = Integer.parseInt((String) map.get("id"));
        String json = JSON.toJSONString((List<OrderItem>) map.get("orderItems"));
        List<OrderItem> list = JSONObject.parseArray(json, OrderItem.class);
        long customerId = Integer.parseInt((String) map.get("customerId"));
        LocalDateTime time = LocalDateTime.now();
        double profit = 0;
        double total = 0;
        if (id == -1) {
            Orders Orders = new Orders();
            Orders.setState("已保存");
            Orders.setCustomerId(customerId);
            Orders.setTime(time);

            Iterator<OrderItem> it = list.iterator();
            while (it.hasNext()) {
                OrderItem orderItem = it.next();
                Good good = goodMapper.selectById(orderItem.getGoodId());
                total += good.getWholesalePrice() * orderItem.getNum();//批发价
                profit += (good.getWholesalePrice() - good.getInputPrice()) * orderItem.getNum();
            }
            Orders.setTotal(total);
            Orders.setProfit(profit);
            orderMapper.insert(Orders);
            long orderId = Orders.getOrderId();
            for (OrderItem orderItem : list) {
                orderItem.setOrderId(orderId);
                orderItemMapper.insert(orderItem);
            }

        } else {
            Orders Orders = orderMapper.selectById(id);
            orderItemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", id));
            Iterator<OrderItem> it = list.iterator();
            while (it.hasNext()) {
                OrderItem orderItem = it.next();
                orderItemMapper.insert(orderItem);
                Good good = goodMapper.selectById(orderItem.getGoodId());
                total += good.getRetailPrice() * orderItem.getNum();//零售价
                profit += (good.getRetailPrice() - good.getInputPrice()) * orderItem.getNum();
            }
            //aOrder.setTime(time);
            Orders.setTotal(total);
            Orders.setProfit(profit);
            orderMapper.updateById(Orders);
        }
        return R.ok();
    }

    @PostMapping("/order/makeOrder")
    public R makeOrder(@RequestBody Map<String, Object> map) {
        long id = Integer.parseInt((String) map.get("id"));
        /*ObjectMapper mapper = new ObjectMapper();
        List<OrderItem> list = mapper.convertValue((List<OrderItem>)map.get("orderItems"), new TypeReference<List<OrderItem>>() { });*/
        String json = JSON.toJSONString((List<OrderItem>) map.get("orderItems"));
        List<OrderItem> list = JSONObject.parseArray(json, OrderItem.class);
        long customerId = Integer.parseInt((String) map.get("customerId"));
        LocalDateTime time = LocalDateTime.now();
        double profit = 0;
        double total = 0;
        if (id == -1) {
            Orders Orders = new Orders();
            Orders.setState("待审核");
            Orders.setCustomerId(customerId);
            Orders.setTime(time);

            Iterator<OrderItem> it = list.iterator();
            while (it.hasNext()) {
                OrderItem orderItem = it.next();
                //orderItemMapper.insert(orderItem);
                Good good = goodMapper.selectById(orderItem.getGoodId());
                total += good.getWholesalePrice() * orderItem.getNum();
                profit += (good.getWholesalePrice() - good.getInputPrice()) * orderItem.getNum();
                RepositoryItem repositoryItem = repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("good_id", good.getId()));
                repositoryItem.setNum(repositoryItem.getNum() - orderItem.getNum());
                repositoryItemMapper.updateById(repositoryItem);
            }
            Orders.setTotal(total);
            Orders.setProfit(profit);
            int result = orderMapper.insert(Orders);
            Long orderId = Orders.getOrderId();
            for (OrderItem orderItem : list) {
                orderItem.setOrderId(orderId);
                orderItemMapper.insert(orderItem);
            }
        } else {
            Orders Orders = orderMapper.selectById(id);
            Orders.setState("待审核");
            orderItemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", id));
            Iterator<OrderItem> it = list.iterator();
            while (it.hasNext()) {
                OrderItem orderItem = it.next();
                orderItemMapper.insert(orderItem);
                Good good = goodMapper.selectById(orderItem.getGoodId());
                total += good.getRetailPrice() * orderItem.getNum();
                profit += (good.getRetailPrice() - good.getInputPrice()) * orderItem.getNum();
                RepositoryItem repositoryItem = repositoryItemMapper.selectOne(new QueryWrapper<RepositoryItem>().eq("repository_id", 1).eq("good_id", good.getId()));
                repositoryItem.setNum(repositoryItem.getNum() - orderItem.getNum());
                repositoryItemMapper.updateById(repositoryItem);
            }
            //aOrder.setTime(time);
            Orders.setTotal(total);
            Orders.setProfit(profit);
            orderMapper.updateById(Orders);
        }
        return R.ok();
    }


    @Autowired
    @Qualifier("ItemService")
    ItemService<Orders> itemService;

    @RequestMapping("/order/add")
    public R add(@RequestBody Orders Orders) {
        return itemService.add(Orders);
    }

    @RequestMapping("/order/delById")
    public R del(@RequestParam long id) {
        return itemService.del(id, new Orders());
    }

    @RequestMapping("/order/delByCustomerId")
    public R delByCustomerId(@RequestParam long id) {
        return itemService.del(id, "customer_id", new Orders());
    }

    @RequestMapping("/order/delByState")
    public R delByState(@RequestParam String state) {
        return itemService.del(state, "state", new Orders());
    }

    @RequestMapping("/order/update/{money}")
    public R update(@RequestBody Orders Orders, @PathVariable int money) {
        if (Orders.getState().equals("已完成")) {
            int count = 0;
            List<RepositoryItem> repositoryItemList = new ArrayList<>();
            for (OrderItem orderItem : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId()))) {
                List<RepositoryItem> repositoryItems = repositoryItemMapper.selectList(null);
                for (RepositoryItem repositoryItem : repositoryItems) {
                    if (isRepository(repositoryItem.getRepositoryId()) && repositoryItem.getGoodId() == orderItem.getGoodId() && repositoryItem.getNum() >= orderItem.getNum()) {
                        repositoryItem.setNum(repositoryItem.getNum() - orderItem.getNum());
                        repositoryItemList.add(repositoryItem);
                        count++;
                        break;
                    }
                }
            }
            if (count != orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId())).size()) {
                System.out.println(count);
                System.out.println(orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId())).size());
                return R.error("库存不足");
            }
            for (RepositoryItem repositoryItem : repositoryItemList) {
                repositoryItemMapper.updateById(repositoryItem);
            }
        } else if (Orders.getState().equals("待退款")) {
            int count = 0;
            List<RepositoryItem> repositoryItemList = new ArrayList<>();
            for (OrderItem orderItem : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId()))) {
                List<RepositoryItem> repositoryItems = repositoryItemMapper.selectList(null);
                for (RepositoryItem repositoryItem : repositoryItems) {
                    if (!isRepository(repositoryItem.getRepositoryId()) && repositoryItem.getGoodId() == orderItem.getGoodId()) {
                        repositoryItem.setNum(repositoryItem.getNum() + orderItem.getNum());
                        repositoryItemList.add(repositoryItem);
                        count++;
                        break;
                    }
                }
            }
            if (count != orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId())).size()) {
                System.out.println("1:" + count);
                System.out.println("2:" + orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", Orders.getOrderId())).size());
                return R.error("库存异常");
            }
            for (RepositoryItem repositoryItem : repositoryItemList) {
                repositoryItemMapper.updateById(repositoryItem);
            }
        } else if (Orders.getState().equals("已退款退货")) {
            if(Orders.getCustomerId()!=0){
                Customer customer = customerMapper.selectById(Orders.getCustomerId());
                customer.setMoney(customer.getMoney() - Orders.getPay() + Orders.getTotal());
                customerMapper.updateById(customer);
                Orders.setPay(0);
            }
        } else if (Orders.getState().equals("待发货")) {
            double beforeMoney = customerMapper.selectById(Orders.getCustomerId()).getMoney();
            Customer my = customerMapper.selectById(Orders.getCustomerId());
            my.setMoney(beforeMoney + money - Orders.getTotal());
            Orders.setPay(money);
            customerMapper.updateById(my);
        }
        return itemService.update(Orders);
    }

    @RequestMapping("/order/pay/{money}")
    public R pay(@RequestBody Orders order, @PathVariable int money) {
        order.setPay(order.getPay() + money);
        orderMapper.updateById(order);
        Customer customer = customerMapper.selectById(order.getCustomerId());
        customer.setMoney(customer.getMoney() + money);
        customerMapper.updateById(customer);
        return R.ok();
    }

    @RequestMapping("/order/searchById")
    public R search(@RequestParam long id) {
        return itemService.search(id, new Orders());
    }

    @RequestMapping("/order/searchByCustomerId")
    public R searchByCustomerId(@RequestParam long id) {
        return itemService.search(id, "customer_id", new Orders());
    }

    @RequestMapping("/order/searchByState")
    public R searchByState(@RequestParam String state) {
        return itemService.search(state, "state", new Orders());
    }

    @RequestMapping("/order/all")
    public R all() {
        return itemService.all(new Orders());
    }

    //TODO
    @RequestMapping("/order/totalSale")
    public R totalSale() {
        List<Orders> list = orderMapper.selectList(new QueryWrapper<Orders>().eq("state", "已完成"));
        double totalSale = 0;
        for (Orders Orders : list) {
            totalSale += Orders.getTotal();
        }
        return R.ok().data("item", totalSale);
    }

    //TODO
    @RequestMapping("/order/totalProfit")
    public R totalProfit() {
        List<Orders> list = orderMapper.selectList(new QueryWrapper<Orders>().eq("state", "已完成"));
        double totalProfit = 0;
        for (Orders Orders : list) {
            totalProfit += Orders.getProfit();
        }
        return R.ok().data("item", totalProfit);
    }

    @RequestMapping(value = "/order/retail")
    public R retail(@RequestBody Map<String, Object> map) {
        try {
            Orders order = new Orders();
            int profit = 0, total = 0;
            List<OrderItem> list = JSONObject.parseArray(JSON.toJSONString(map.get("data")), OrderItem.class);
            if (repositoryMapper.selectList(new QueryWrapper<Repository>().eq("classification", "门店")).get(0) == null) {
                return R.error("查无门店");
            }
            long storeRepositoryID = repositoryMapper.selectList(new QueryWrapper<Repository>().eq("classification", "门店")).get(0).getRepositoryId();
            List<RepositoryItem> storeItems = repositoryItemMapper.selectList(new QueryWrapper<RepositoryItem>().eq("repository_id", storeRepositoryID));
            if (storeItems == null) {
                return R.error("门店无存货");
            }
            List<RepositoryItem> waitWriteToRepository = new ArrayList<>();
            List<OrderItem> waitWriteToDataBase = new ArrayList<>();
            for (OrderItem orderItem : list) {
                for (RepositoryItem repositoryItem : storeItems) {//看门店库存够不够
                    if (repositoryItem.getGoodId() == orderItem.getGoodId()) {
                        long storeNum = repositoryItem.getNum();
                        if (storeNum >= orderItem.getNum()) {
                            repositoryItem.setNum(storeNum - orderItem.getNum());
                            waitWriteToRepository.add(repositoryItem);
                            break;
                        } else {//库存不足
                            return R.error("库存不足");
                        }
                    }
                }
                orderItem.setOrderId(order.getOrderId());
                total += goodMapper.selectById(orderItem.getGoodId()).getRetailPrice() * orderItem.getNum();
                profit += (goodMapper.selectById(orderItem.getGoodId()).getRetailPrice() - goodMapper.selectById(orderItem.getGoodId()).getInputPrice()) * orderItem.getNum();
                waitWriteToDataBase.add(orderItem);
            }
            for (RepositoryItem repositoryItem : waitWriteToRepository) {
                repositoryItemMapper.updateById(repositoryItem);//更新数据库
            }
            order.setTime(LocalDateTime.now());
            order.setProfit(profit);
            order.setTotal(total);
            order.setState("已完成");
            order.setPay(total);
            orderMapper.insert(order);
            for (OrderItem orderItem : waitWriteToDataBase) {
                orderItem.setOrderId(order.getOrderId());
                orderItemMapper.insert(orderItem);
            }
            return R.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return R.error();
        }

    }

    @RequestMapping(value = "/order/wholesale")
    public R wholesale(@RequestBody Map<String, Object> map) {
        long customerID = Long.parseLong((String) map.get("customerID"));
        if (customerMapper.selectById(customerID) == null) {
            return R.error("客户不存在!");
        }
        Orders order = new Orders();
        order.setCustomerId(customerID);
        int profit = 0, total = 0;
        List<OrderItem> list = JSONObject.parseArray(JSON.toJSONString(map.get("data")), OrderItem.class);
        for (OrderItem orderItem : list) {
            total += goodMapper.selectById(orderItem.getGoodId()).getWholesalePrice() * orderItem.getNum();
            profit += (goodMapper.selectById(orderItem.getGoodId()).getWholesalePrice() - goodMapper.selectById(orderItem.getGoodId()).getInputPrice()) * orderItem.getNum();
        }
        order.setTotal(total);
        order.setProfit(profit);
        order.setTime(LocalDateTime.now());
        order.setState("待审核");
        orderMapper.insert(order);
        long orderID = order.getOrderId();
        System.out.println(orderID);
        for (OrderItem orderItem : list) {
            orderItem.setOrderId(orderID);
            orderItemMapper.insert(orderItem);
        }

        return R.ok();
    }

    boolean isRepository(long RepositoryID) {
        return repositoryMapper.selectById(RepositoryID).getClassification().equals("仓库");
    }

    @GetMapping("/order/allRepository")
    public R allRepository() {
        List<Repository> repositories = repositoryMapper.selectList(null);
        List<Long> willSend = new ArrayList<>();
        for (Repository repository : repositories) {
            willSend.add(repository.getRepositoryId());
        }
        return R.ok().data("repository", willSend);
    }

    public static class data {
        public String name;
        public long id;
        public long soldNum;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getSoldNum() {
            return soldNum;
        }

        public void setSoldNum(long soldNum) {
            this.soldNum = soldNum;
        }
    }

    @GetMapping("/order/getHotGoods/{customerId}")
    public R getHotGoods(@PathVariable long customerId) {
        List<data> my = new ArrayList<>();
        List<Good> goods = goodMapper.selectList(null);
        for (Good tmp : goods) {
            data newData = new data();
            newData.id = tmp.getId();
            newData.name = tmp.getName();
            newData.soldNum = 0;
            my.add(newData);
        }
        List<OrderItem> needToV = new ArrayList<>();
        List<Orders> ordersList;
        if (customerId == -1) {//所有客户
            ordersList = orderMapper.selectList(null);
        } else {
            ordersList = orderMapper.selectList(new QueryWrapper<Orders>().eq("customer_id", customerId));
        }
        for (Orders tmp : ordersList) {
            needToV.addAll(orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", tmp.getOrderId())));
        }
        for (OrderItem item : needToV) {
            for (data tmp : my) {
                if (tmp.id == item.getGoodId()) {
                    tmp.soldNum += item.getNum();
                    break;
                }
            }
        }
        my.sort(Comparator.comparing(data::getSoldNum).reversed());
        return R.ok().data("items", my);
    }

    @GetMapping("/order/getByGoodsID/{id}/{customerId}")
    public R getByGoodsID(@PathVariable long id, @PathVariable long customerId) {
        List<OrderItem> my = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("good_id", id));
        Set<Long> orders = new HashSet<>();
        List<Orders> ordersReturn = new ArrayList<>();
        for (OrderItem tmp : my) {
            orders.add(tmp.getOrderId());
        }
        for (Long tmp : orders) {
            if (orderMapper.selectById(tmp) != null) {
                if (orderMapper.selectById(tmp).getCustomerId() == customerId || customerId == -1) {
                    ordersReturn.add(orderMapper.selectById(tmp));
                }
            }
        }
        return R.ok().data("items", ordersReturn);
    }

}
