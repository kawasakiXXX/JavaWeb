package com.cds.javaweb.web_01.mapper;

import com.cds.javaweb.web_01.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper// 告诉MyBatis这个接口是一个Mapper,程序运行时自动创建这个接口的实现类对象（代理对象），并且会自动存入到Spring（IOC）容器中--bean
public interface UserMapper {
    // 查询所有用户
    @Select("select id, name, age, create_time, update_time from user order by id desc ")
    public List<User> findAll();


    @Delete("delete from user where id = #{id}")// #{id}是占位符, #{id}的值会从方法参数中获取
    public void deleteById(int id);


    @Insert("insert into user (name, age, create_time, update_time) values (#{name},#{age},#{createTime},#{updateTime})")
    public void insert(User user);

    @Select("select id, name, age, create_time, update_time from user where id = #{id}")
    public User findById(int id);

    @Update("update user set name = #{name}, age = #{age}, update_time = #{updateTime} where id = #{id}")
    public void update(User user);

}
