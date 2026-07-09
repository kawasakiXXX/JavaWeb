package com.cds.javaweb.web_01.controller;

import com.cds.javaweb.web_01.pojo.Result;
import com.cds.javaweb.web_01.pojo.User;
import com.cds.javaweb.web_01.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserServices userServices;

    @GetMapping("/web01")
    public Result findAll() {
        System.out.println("查询全部部门数据");
        List<User> userList = userServices.findAll();
        return Result.success(userList);
    }

    @DeleteMapping("/web01/{id}")
    public Result delete(@PathVariable Integer id) {
        System.out.println("已删除id为" + id + "的用户");
        userServices.deleteById(id);
        return Result.success();
    }

    @PostMapping("/web01")
    public Result add(@RequestBody User user) {
        System.out.println("已插入用户" + user);
        userServices.insert(user);
        return Result.success();
    }

    @GetMapping("/web01/{id}")
    public Result findById(@PathVariable Integer id) {
        System.out.println("已查询id为" + id + "的用户");
        return Result.success(userServices.findById(id));
    }

    @PutMapping("/web01")
    public Result update(@RequestBody User user) {
        System.out.println("已更新用户" + user);
        userServices.update(user);
        return Result.success();
    }
}
