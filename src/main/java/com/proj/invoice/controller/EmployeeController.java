package com.proj.invoice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.proj.invoice.bean.Employee;
import com.proj.invoice.bean.Good;
import com.proj.invoice.bean.Users;
import com.proj.invoice.mapper.EmployeeMapper;
import com.proj.invoice.mapper.UserMapper;
import com.proj.invoice.service.ItemService;
import com.proj.invoice.utils.R;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RestController
@CrossOrigin
public class EmployeeController {

    String defaultPassword;
    @Autowired
    @Qualifier("ItemService")
    ItemService<Employee> itemService;

    @Autowired
    UserMapper userMapper;
    @Autowired
    EmployeeMapper employeeMapper;

//    public EmployeeController() {
//        try {
//            final CountDownLatch countDownLatch=new CountDownLatch(1);
//            ZooKeeper zooKeeper=
//                    new ZooKeeper("8.130.130.118:2181" ,
//                            4000, event -> {
//                        if(Watcher.Event.KeeperState.SyncConnected==event.getState()){
//                            //如果收到了服务端的响应事件，连接成功
//                            countDownLatch.countDown();
//                        }
//                    });
//            countDownLatch.await();
//            byte [] tmp= zooKeeper.getData("/defaultpassword", false, null);
//            defaultPassword=new String(tmp);
//            System.out.println(defaultPassword);
//        } catch (IOException | InterruptedException | KeeperException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @RequestMapping("/employee/add")
    public R add(@RequestBody Employee employee){
        Users Users =new Users();

        Users.setPwd(defaultPassword);
        Users.setPosition(employee.getPosition());
        userMapper.insert(Users);
        employee.setAccount(Users.getAccount());
        List<Employee> List= employeeMapper.selectList(null);
        long maxId=-1;
        for (Employee e: List) {
            if(e.getId()>maxId){
                maxId=e.getId();
            }
        }
        employee.setId(maxId+1);
        itemService.add(employee);
        return R.ok().data("item", Users);
    }

    @RequestMapping("/employee/delById/{id}")
    public R del(@PathVariable long id){
        userMapper.deleteById(employeeMapper.selectById(id).getAccount());
        return itemService.del(id,new Employee());
    }

    @RequestMapping("/employee/delByName")
    public R del(@RequestParam String name){
        return itemService.del(name,"name",new Employee());
    }


    @RequestMapping("/employee/updatePos")
    public R updatePos(@RequestBody Employee employee){
        Employee employee_exit=employeeMapper.selectById(employee.getId());
        if(employee_exit!=null){
            employee_exit.setPosition(employee.getPosition());
            update(employee_exit);
            return R.ok();
        }
        return R.error();
    }

    @RequestMapping("/employee/update")
    public R update(@RequestBody Employee employee){
        return itemService.update(employee);
    }

    @RequestMapping("/employee/searchById")
    public R search(@RequestParam long id){
        return itemService.search(id,new Employee());
    }

    @RequestMapping("/employee/searchByName")
    public R search(@RequestParam String name){
        return itemService.search(name,"name",new Employee());
    }

    @RequestMapping("/employee/all")
    public R all(){
        return itemService.all(new Employee());
    }


    @RequestMapping("/employee/count")
    public R count(){
        int count=employeeMapper.selectCount(new QueryWrapper<Employee>());
        return R.ok().data("item",count);
    }
}
