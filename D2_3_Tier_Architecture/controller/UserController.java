package com.cds.javaweb.D2_3_Tier_Architecture.controller;

import com.cds.javaweb.D2_3_Tier_Architecture.model.User;
import com.cds.javaweb.D2_3_Tier_Architecture.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 控制层 — 接收 HTTP 请求，调用 Service 层，返回响应
 *
 * 完整的三层调用链：Controller → Service → DAO → 内存数据库(Map)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 构造器注入 Service
    public UserController(UserService userService) {

        this.userService = userService;
    }

    /**
     * GET /api/users — 查询所有用户
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id} — 根据ID查询用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users — 新增用户
     * 请求体示例: {"name": "张三", "email": "zhangsan@example.com"}
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.registerUser(user);
        return ResponseEntity.ok(savedUser);
    }

    /**
     * PUT /api/users/{id} — 更新用户
     * 请求体示例: {"id": 1, "name": "李四", "email": "lisi@example.com"}
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * DELETE /api/users/{id} — 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok("删除成功: id=" + id);
        } else {
            return ResponseEntity.ok("用户不存在: id=" + id);
        }
    }
}
