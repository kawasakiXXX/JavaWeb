package com.cds;

import com.cds.mapper.UserMapper;
import com.cds.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class JavaWebApplicationTests {
    @Autowired
    private UserMapper userMapper;

    @Test
    public void testFindAll() {
        List<User> userList = userMapper.findAll();
        for (User user : userList) {
            System.out.println(user);
        }
    }
    @Test
    public void testDeleteById() {
        userMapper.deleteById(1);
    }




}
