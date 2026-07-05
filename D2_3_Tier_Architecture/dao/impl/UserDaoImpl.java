package com.cds.javaweb.D2_3_Tier_Architecture.dao.impl;

import com.cds.javaweb.D2_3_Tier_Architecture.dao.UserDao;
import com.cds.javaweb.D2_3_Tier_Architecture.model.User;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据访问层实现 — 使用内存 Map 模拟数据库，启动时从 resources/data/users.json 读取初始数据
 */
@Repository
public class UserDaoImpl implements UserDao {

    // 模拟数据库表，线程安全的 Map
    private final Map<Integer, User> userDB = new ConcurrentHashMap<>();
    // 自增ID生成器
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    /**
     * 启动时从 resources/data/users.json 加载初始数据到内存
     */
    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("data/users.json");
            User[] users = mapper.readValue(resource.getInputStream(), User[].class);
            for (User user : users) {
                if (user.getId() != null) {
                    userDB.put(user.getId(), user);
                    if (user.getId() >= idGenerator.get()) {
                        idGenerator.set(user.getId() + 1);
                    }
                }
            }
            System.out.println("已从 data/users.json 加载 " + users.length + " 条用户数据");
        } catch (Exception e) {
            System.out.println("未找到 data/users.json，使用空数据库启动: " + e.getMessage());
        }
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userDB.values());
    }

    @Override
    public User findById(Integer id) {
        return userDB.get(id);
    }

    @Override
    public User save(User user) {
        // 生成新ID
        Integer id = idGenerator.getAndIncrement();
        user.setId(id);
        userDB.put(id, user);
        return user;
    }

    @Override
    public User update(User user) {
        Integer id = user.getId();
        if (id == null || !userDB.containsKey(id)) {
            return null; // 用户不存在
        }
        userDB.put(id, user);
        return user;
    }

    @Override
    public boolean deleteById(Integer id) {
        return userDB.remove(id) != null;
    }
}
