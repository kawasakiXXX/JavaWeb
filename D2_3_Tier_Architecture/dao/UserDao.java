package com.cds.javaweb.D2_3_Tier_Architecture.dao;

import com.cds.javaweb.D2_3_Tier_Architecture.model.User;
import java.util.List;

/**
 * 数据访问层接口 — 定义用户数据的增删改查操作
 */
public interface UserDao {

    /** 查询所有用户 */
    List<User> findAll();

    /** 根据ID查询用户 */
    User findById(Integer id);

    /** 新增用户，返回新增后的用户（含ID） */
    User save(User user);

    /** 更新用户 */
    User update(User user);

    /** 根据ID删除用户，返回是否删除成功 */
    boolean deleteById(Integer id);
}
