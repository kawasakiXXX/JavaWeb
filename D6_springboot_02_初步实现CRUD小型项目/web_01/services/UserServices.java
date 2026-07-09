package com.cds.javaweb.web_01.services;

import com.cds.javaweb.web_01.pojo.User;

import java.util.List;

public interface UserServices {
    public List<User> findAll();
    public void deleteById(int id);

    void insert(User user);

    User findById(int id);

    void update(User user);
}
