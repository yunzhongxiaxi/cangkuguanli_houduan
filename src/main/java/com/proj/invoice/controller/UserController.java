package com.proj.invoice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.proj.invoice.bean.Employee;
import com.proj.invoice.bean.Users;
import com.proj.invoice.mapper.EmployeeMapper;
import com.proj.invoice.mapper.UserMapper;
import com.proj.invoice.service.ItemService;
import com.proj.invoice.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
//@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    @Qualifier("ItemService")
    ItemService<Users> itemService;

    @RequestMapping("/user/add")
    public R add(@RequestBody Users Users){
        return itemService.add(Users);
    }

    @RequestMapping("/user/delById")
    public R del(@RequestParam long id){
        return itemService.del(id,new Users());
    }

    @RequestMapping("/user/delByPosition")
    public R del(@RequestParam String position){
        return itemService.del(position,"position",new Users());
    }

    @RequestMapping("/user/update")
    public R update(@RequestBody Users Users){
        return itemService.update(Users);
    }

    @RequestMapping("/user/searchById")
    public R search(@RequestParam long id){
        return itemService.search(id,new Users());
    }

    @RequestMapping("/user/searchByPosition")
    public R search(@RequestParam String position){
        return itemService.search(position,"position",new Users());
    }

    @RequestMapping("/user/all")
    public R all(){
        return itemService.all(new Users());
    }



    @Autowired
    UserMapper userMapper;
    @Autowired
    EmployeeMapper employeeMapper;

    @PostMapping("/user/login")
    public R login(@RequestBody Users user){
        Users Users =userMapper.selectById(user.getAccount());
        List<Boolean> rights=new ArrayList<>();
        for(int i=0;i<6;i++){
            rights.add(true);
        }
        Employee employee= employeeMapper.selectList(new QueryWrapper<Employee>().eq("account",user.getAccount())).get(0);
        if(employee.getExtraposition()==null){
            rights.add(false);
        }
        else if(employee.getExtraposition().equals("首席店长")){
            rights.add(true);//查看统计的权限
        }
        else{
            rights.add(false);
        }
        if (Users !=null&&user.getPwd().equals(Users.getPwd())){
            /*Employee employee= employeeMapper.selectList(new QueryWrapper<Employee>().eq("account",user.getAccount())).get(0);*/
            switch (Users.getPosition()){
                case "店长":
                    break;
                case "销售员":
                    rights.set(1,false);//雇员资料
                    rights.set(2,false);//客户资料
                    rights.set(4,false);//库存管理
                    rights.set(5,false);//仓库管理
                    break;
                case "仓库管理员":
                    rights.set(1,false);//雇员资料
                    rights.set(2,false);//客户资料
                    rights.set(3,false);//销售单
                    break;
            }
            return R.ok().data("rights",rights);
        }
        return R.error("账号或密码错误");
    }

    @GetMapping("/user/info")
    public R info(@RequestParam String token){
        Employee employee=employeeMapper.selectOne(new QueryWrapper<Employee>().eq("account",token));
        System.out.println(employeeMapper.selectList(null).get(0).getAccount());
        if(employee!=null){
            String[] roles = new String[1];
            roles[0] = employee.getPosition();
            return R.ok().data("name",employee.getName()).data("avatar","https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif")
                    .data("introduction","I am "+roles[0]).data("roles",roles);
        }
        return R.error();
    }

    @PostMapping("setPos")
    public R setPos(@RequestBody Users u)
    {
        //set Pos in DataBase
        System.out.println(u);
        return R.ok();
    }

    @GetMapping("/user/mdfPwd")
    public R mdfPwd(@RequestParam long username,@RequestParam String pwd,@RequestParam String newPwd)
    {
        Users Users =userMapper.selectById(username);
        if(Users !=null&& Users.getPwd().equals(pwd)){
            Users.setPwd(newPwd);
            return update(Users);
        }
        return R.ok().success(Boolean.FALSE);
    }

}
