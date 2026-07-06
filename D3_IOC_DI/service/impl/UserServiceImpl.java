package com.cds.javaweb.D3_IOC_DI.service.impl;

import com.cds.javaweb.D3_IOC_DI.dao.UserDao;
import com.cds.javaweb.D3_IOC_DI.model.User;
import com.cds.javaweb.D3_IOC_DI.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务逻辑层 — 同屏展示三种注入方式
 */
@Service
public class UserServiceImpl implements UserService {

    // ========== 引入 UserDao 的三种方式（同屏对比） ==========

    // ① 构造器注入：final 不可变，启动即发现缺失，✅ 推荐
    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    /*
    // ② Setter 注入：非 final，可在 setter 中加校验/日志
    private UserDao userDao;

    @Autowired
    public void setUserDao(@Qualifier("userDaoImpl") UserDao userDao) {
        this.userDao = userDao;
    }
    */

    /*
    // ③ @Autowired 字段注入：代码最短，不能 final，⚠️ 不推荐
    @Autowired
    @Qualifier("userDaoImpl")//指定哪个 Bean
    private UserDao userDao;
    */

    // ============================================================

    // 可选依赖 — Setter 注入演示
    private String appInfo = "未注入";

    @Autowired(required = false)
    public void setAppInfo(@Qualifier("appInfo") String appInfo) {
        this.appInfo = appInfo;
    }

    // ============================================================

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
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
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
