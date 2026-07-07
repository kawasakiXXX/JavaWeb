package JDBC;

import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class jdbcTest {
    @Test
    public void testUpdate() throws Exception {
        //1.加载驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        //2.获取连接
        String url = "jdbc:mysql://localhost:3306/web01";
        String username = "root";
        String password = "@Likekawasaki6";
        Connection connection = DriverManager.getConnection(url,username,password);
        //3.获取数据库操作对象
        Statement statement = connection.createStatement();
        //4.执行sql
        int i = statement.executeUpdate("update user set age = 20 where id = 1");
        System.out.println(i);
        //5.释放资源
        statement.close();
        connection.close();
    }
}


//需要的依赖
//    <dependencies>
//        <!-- MySQL JDBC Driver -->
//        <dependency>
//            <groupId>com.mysql</groupId>
//            <artifactId>mysql-connector-j</artifactId>
//            <version>8.0.33</version>
//        </dependency>
//        <!-- TestNG -->
//        <dependency>
//            <groupId>org.testng</groupId>
//            <artifactId>testng</artifactId>
//            <version>RELEASE</version>
//            <scope>test</scope>
//        </dependency>
//    </dependencies>
