package com.cds.javaweb.web_01.services;

import com.cds.javaweb.web_01.pojo.User;

import java.util.List;

public interface UserServices {
    public List<User> findAll();
    public void deleteById(int id);

    void insert(User user);

    User findById(int id);

    void update(User user);

    /**
     * 根据用户名和密码验证用户登录
     * @param name     用户名
     * @param password 密码
     * @return 验证成功返回 User 对象，失败返回 null
     */
    User login(String name, String password);
}
