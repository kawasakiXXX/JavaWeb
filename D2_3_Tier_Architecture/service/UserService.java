package com.cds.javaweb.D2_3_Tier_Architecture.service;

import com.cds.javaweb.D2_3_Tier_Architecture.model.User;
import java.util.List;

/**
 * 业务逻辑层接口 — 定义用户业务操作
 */
public interface UserService {

    /** 获取所有用户 */
    List<User> getAllUsers();

    /** 根据ID获取用户 */
    User getUserById(Integer id);

    /** 注册新用户（含业务校验） */
    User registerUser(User user);

    /** 更新用户信息 */
    User updateUser(User user);

    /** 删除用户 */
    boolean deleteUser(Integer id);
}
