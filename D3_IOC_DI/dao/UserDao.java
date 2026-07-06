package com.cds.javaweb.D3_IOC_DI.dao;

import com.cds.javaweb.D3_IOC_DI.model.User;
import java.util.List;

/**
 * 数据访问层接口 — 定义 CRUD 操作
 * 上层只依赖接口而非实现，换存储只需换实现类，Service 层零改动
 */
public interface UserDao {

    List<User> findAll();

    User findById(Integer id);

    User save(User user);

    User update(User user);

    boolean deleteById(Integer id);
}
