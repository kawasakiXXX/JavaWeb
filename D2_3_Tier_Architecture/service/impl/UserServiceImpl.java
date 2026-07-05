package com.cds.javaweb.D2_3_Tier_Architecture.service.impl;

import com.cds.javaweb.D2_3_Tier_Architecture.dao.UserDao;
import com.cds.javaweb.D2_3_Tier_Architecture.model.User;
import com.cds.javaweb.D2_3_Tier_Architecture.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务逻辑层实现 — 处理用户相关的业务逻辑
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    // 构造器注入 DAO
    public UserServiceImpl(UserDao userDao) {

        this.userDao = userDao;
    }

    @Override
    public List<User> getAllUsers() {

        return userDao.findAll();
    }

    @Override
    public User getUserById(Integer id) {

        return userDao.findById(id);
    }

    @Override
    public User registerUser(User user) {
        // 业务校验：姓名和邮箱不能为空
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        // 调用 DAO 保存
        return userDao.save(user);
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("更新用户时ID不能为空");
        }
        User updated = userDao.update(user);
        if (updated == null) {
            throw new IllegalArgumentException("用户不存在: id=" + user.getId());
        }
        return updated;
    }

    @Override
    public boolean deleteUser(Integer id) {

        return userDao.deleteById(id);
    }
}
