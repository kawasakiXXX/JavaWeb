package com.cds.services;

import com.cds.mapper.UserMapper;
import com.cds.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserServicesImpl implements UserServices{
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public void deleteById(int id) {
        userMapper.deleteById(id);
    }

    @Override
    public void insert(User user) {
        userMapper.insert(user);
    }

    @Override
    public User findById(int id) {
        return userMapper.findById(id);
    }

    @Override
    public void update(User user) {
        userMapper.update(user);
    }

    @Override
    public User login(String name, String password) {
        User user = userMapper.findByName(name);
        if (user != null && user.getPassword() != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

}
