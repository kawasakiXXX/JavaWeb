package com.cds.mapper;

import com.cds.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper// 告诉MyBatis这个接口是一个Mapper,程序运行时自动创建这个接口的实现类对象（代理对象），并且会自动存入到Spring（IOC）容器中--bean
public interface UserMapper {
    // 查询所有用户
    @Select("select id, username, password from user order by id desc ")
    public List<User> findAll();


    @Delete("delete from user where id = #{id}")// #{id}是占位符, #{id}的值会从方法参数中获取
    public void deleteById(int id);


    @Insert("insert into user (username, password) values (#{username},#{password})")
    public void insert(User user);

    @Select("select id, username from user where id = #{id}")
    public User findById(int id);

    @Update("update user set username = #{username} where id = #{id}")
    public void update(User user);

    // 根据用户名查询用户（含密码，用于登录验证）
    @Select("select id, username, password from user where username = #{username}")
    public User findByName(String username);

}
