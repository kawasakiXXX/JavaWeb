package com.cds.javaweb.D3_IOC_DI.dao.impl;

import com.cds.javaweb.D3_IOC_DI.dao.UserDao;
import com.cds.javaweb.D3_IOC_DI.model.User;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据访问层实现 — 内存 Map 模拟数据库
 *
 * @Repository 将此类注册为 Spring Bean，由 IoC 容器管理生命周期
 * Bean 生命周期：构造器 → @PostConstruct → 运行中 → @PreDestroy
 */
@Repository
public class UserDaoImpl implements UserDao {

    private final Map<Integer, User> userDB = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public UserDaoImpl() {
        System.out.println("🟢 [IoC容器] 正在创建 UserDaoImpl Bean");
    }

    /** 启动时加载初始数据 — Bean 初始化回调 */
    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("D2_data/users.json");
            User[] users = mapper.readValue(resource.getInputStream(), User[].class);
            for (User user : users) {
                if (user.getId() != null) {
                    userDB.put(user.getId(), user);
                    if (user.getId() >= idGenerator.get()) {
                        idGenerator.set(user.getId() + 1);
                    }
                }
            }
            System.out.println("  └─ [@PostConstruct] 已加载 " + users.length + " 条用户数据");
        } catch (Exception e) {
            System.out.println("  └─ 未找到数据文件，空库启动");
        }
    }

    /** Bean 销毁回调 */
    @PreDestroy
    public void destroy() {
        System.out.println("🔴 [IoC容器] UserDaoImpl Bean 即将销毁，用户数: " + userDB.size());
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
        Integer id = idGenerator.getAndIncrement();
        user.setId(id);
        userDB.put(id, user);
        return user;
    }

    @Override
    public User update(User user) {
        Integer id = user.getId();
        if (id == null || !userDB.containsKey(id)) {
            return null;
        }
        userDB.put(id, user);
        return user;
    }

    @Override
    public boolean deleteById(Integer id) {
        return userDB.remove(id) != null;
    }
}
