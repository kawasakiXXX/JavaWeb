package com.cds.javaweb.web_01.controller;

import com.cds.javaweb.web_01.anno.Log;
import com.cds.javaweb.web_01.pojo.Result;
import com.cds.javaweb.web_01.pojo.User;
import com.cds.javaweb.web_01.services.UserServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RequestMapping("/web01")
@RestController
public class UserController {
    @Autowired
    private UserServices userServices;

    @GetMapping
    public Result findAll() {
        log.info("查询全部部门数据");
        List<User> userList = userServices.findAll();
        return Result.success(userList);
    }

    @Log("删除用户")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("已删除id为{}的用户", id );
        userServices.deleteById(id);
        return Result.success();
    }

    @Log("新增用户")
    @PostMapping
    public Result add(@RequestBody User user) {
        log.info("已插入用户{}", user);
        userServices.insert(user);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result findById(@PathVariable Integer id) {
        log.info("已查询id为{}的用户", id);
        return Result.success(userServices.findById(id));
    }

    @Log("更新用户")
    @PutMapping
    public Result update(@RequestBody User user) {
        log.info("已更新用户{}", user);
        userServices.update(user);
        return Result.success();
    }
}
