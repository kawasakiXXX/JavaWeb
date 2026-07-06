package com.cds.javaweb.D3_IOC_DI.controller;

import com.cds.javaweb.D3_IOC_DI.model.User;
import com.cds.javaweb.D3_IOC_DI.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 控制层 — 同屏展示三种注入方式
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    // ========== 引入 UserService 的两种方式（同屏对比） ==========

    // ① 构造器注入：final 不可变，启动即发现缺失，推荐
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /*
    // ② Setter 注入：非 final，可在 setter 中加额外逻辑
    private UserService userService;

    @Autowired
    public void setUserService(@Qualifier("userServiceImpl") UserService userService) {
        this.userService = userService;
    }
    */

    /*
    // ③ @Autowired 字段注入：代码最短，不能 final，⚠️ 不推荐
    @Autowired
    @Qualifier("userServiceImpl")//指定哪个 Bean
    private UserService userService;
    */

    // ③ @Autowired 字段注入：非 final，最简洁但不推荐生产使用
    @Autowired
    @Qualifier("serverStartTime")
    private String serverStartTime;

    // ============================================================

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.registerUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        return ResponseEntity.ok(userService.updateUser(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        boolean deleted = userService.deleteUser(id);
        return deleted
                ? ResponseEntity.ok("删除成功: id=" + id)
                : ResponseEntity.ok("用户不存在: id=" + id);
    }
}
