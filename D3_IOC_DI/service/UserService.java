package com.cds.javaweb.D3_IOC_DI.service;

import com.cds.javaweb.D3_IOC_DI.model.User;
import java.util.List;

/**
 * 业务逻辑层接口 — 定义用户业务操作
 * Controller 只依赖接口而非实现，是 DI + 面向接口编程实现分层解耦的关键
 */
public interface UserService {

    List<User> getAllUsers();

    User getUserById(Integer id);

    User registerUser(User user);

    User updateUser(User user);

    boolean deleteUser(Integer id);
}
