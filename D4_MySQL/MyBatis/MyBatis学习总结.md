# MyBatis 学习总结

> 基于 Spring Boot 3.3.0 + MyBatis 3.0.5 项目实战

---

## 一、MyBatis 是什么——为什么用它替代 JDBC？

### 1.1 原生 JDBC 的痛点

回顾 `D4_JDBC/jdbcTest.java`，一次简单的更新操作需要：

```java
// 1. 加载驱动
Class.forName("com.mysql.cj.jdbc.Driver");
// 2. 获取连接
Connection connection = DriverManager.getConnection(url, username, password);
// 3. 获取操作对象
Statement statement = connection.createStatement();
// 4. 执行 SQL（硬编码字符串）
int i = statement.executeUpdate("update user set age = 20 where id = 1");
// 5. 手动释放资源
statement.close();
connection.close();
```

**JDBC 的四大痛点：**

| 痛点 | 说明 |
|------|------|
| **硬编码** | SQL 语句写在 Java 代码字符串里，改 SQL 就要改代码、重新编译 |
| **重复代码** | 每次操作都要写加载驱动 → 获取连接 → 执行 → 释放资源 |
| **手动映射** | 查询结果 ResultSet 要手动逐列取出，再 set 到实体类中 |
| **资源管理** | 必须手动 close，忘了就造成连接泄漏 |

### 1.2 MyBatis 的解决方案

MyBatis 是一个**半自动的持久层框架**，它帮我们做了：

- **SQL 与 Java 代码分离**：SQL 写在 XML 文件或注解中，改动不需要重新编译 Java
- **自动封装连接管理**：获取连接、释放资源全部由框架处理
- **自动结果映射**：数据库列名 → 实体类属性，自动映射
- **参数绑定**：用 `#{xxx}` 占位符，自动防止 SQL 注入

**本质上是把 JDBC 的重复劳动封装掉，把控制权（SQL 编写）留给开发者。**

---

## 二、MyBatis 的环境搭建（辅助配置）

### 2.1 pom.xml 依赖

```xml
<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MyBatis Spring Boot Starter（核心依赖，自带 HikariCP 连接池） -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>

<!-- Lombok：简化实体类 getter/setter/构造方法 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.44</version>
    <optional>true</optional>
</dependency>
```

### 2.2 application.properties 配置

```properties
# ========== 数据库连接 ==========
spring.datasource.url=jdbc:mysql://localhost:3306/web01
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=你的密码

# ========== MyBatis 日志 ==========
# 在控制台打印 SQL 执行日志（开发调试用）
mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl

# ========== XML 映射文件位置 ==========
# classpath 指的是 target/classes/（编译后的 resources 目录）
# classpath:mapper/*.xml  →  找 target/classes/mapper/ 下所有 .xml 文件
mybatis.mapper-locations=classpath:mapper/*.xml
```

### 2.3 配置要点总结

| 配置项 | 作用 |
|--------|------|
| `spring.datasource.*` | 数据库连接信息（url、驱动、用户名、密码） |
| `mybatis.configuration.log-impl` | 开启 SQL 日志，方便调试 |
| `mybatis.mapper-locations` | 告诉 MyBatis 去哪找 XML 映射文件 |

---

## 三、数据库连接池

### 3.1 为什么需要连接池？

数据库连接是昂贵资源——每次建立 TCP 连接 + 握手认证耗时几十到几百毫秒。如果每次请求都新建、用完就关，高并发下性能和资源都是灾难。

### 3.2 HikariCP（Spring Boot 默认连接池）

引入 `mybatis-spring-boot-starter` 后，**Spring Boot 自动配置了 HikariCP 作为连接池**，无需额外配置。

```
请求 → 从连接池借一个连接 → 执行SQL → 归还连接（不关闭）
```

**连接池的核心优势：**
- 连接复用，避免频繁创建/销毁
- 限制最大连接数，防止数据库被压垮
- 空闲连接自动回收

### 3.3 可选配置（application.properties）

```properties
# 连接池大小（可选，有默认值）
spring.datasource.hikari.minimum-idle=5          # 最小空闲连接
spring.datasource.hikari.maximum-pool-size=20    # 最大连接数
spring.datasource.hikari.idle-timeout=300000     # 空闲超时（ms）
spring.datasource.hikari.connection-timeout=20000 # 获取连接超时（ms）
```

---

## 四、实体类与 Lombok

### 4.1 实体类定义（对应数据库表）

`com.cds.javaweb.MyBatis.Pojo.User`：

```java
@Data                    // 自动生成 getter/setter/toString/equals/hashCode
@AllArgsConstructor      // 全参构造方法
@NoArgsConstructor       // 无参构造方法
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer age;
}
```

### 4.2 Lombok 常用注解

| 注解 | 作用 |
|------|------|
| `@Data` | 组合：`@Getter + @Setter + @ToString + @EqualsAndHashCode` |
| `@AllArgsConstructor` | 生成包含全部字段的构造方法 |
| `@NoArgsConstructor` | 生成无参构造方法 |
| `@Builder` | 生成建造者模式的构建方法 |

> **注意**：实体类的属性名建议与数据库列名保持一致（或通过驼峰命名自动映射，如 `hire_date` → `hireDate`）。MyBatis 默认开启 `mapUnderscoreToCamelCase` 会自动转换。

---

## 五、Mapper 接口——增删改查操作

### 5.1 Mapper 接口定义

`com.cds.javaweb.MyBatis.Mapper.UserMapper`：

```java
@Mapper  // 告诉 MyBatis：这是 Mapper 接口，自动创建代理对象并加入 Spring IOC 容器
public interface UserMapper {

    // ==================== 查询 ====================
    public List<User> findAll();                    // XML 方式：查全部

    @Select("select * from user where username = #{username} and password = #{password}")
    public User findByUsernameAndPassword(          // 注解方式：条件查询
        @Param("username") String username,
        @Param("password") String password
    );

    // ==================== 删除 ====================
    @Delete("delete from user where id = #{id}")
    public void delete(int id);                     // #{id} 自动从方法参数取值

    // ==================== 新增 ====================
    @Insert("insert into user(username,password,name,age) " +
            "values(#{username},#{password},#{name},#{age})")
    public void insert(User user);                  // #{username} 从 user.getUsername() 取值

    // ==================== 更新 ====================
    @Update("update user set username=#{username}, password=#{password}, " +
            "name=#{name}, age=#{age} where id=#{id}")
    public void update(User user);
}
```

### 5.2 增删改查完整对比

| 操作 | 注解 | XML标签 | 说明 |
|------|------|---------|------|
| **查询** | `@Select` | `<select>` | 返回单条或多条结果 |
| **新增** | `@Insert` | `<insert>` | 参数是实体对象，用 `#{属性名}` 取值 |
| **更新** | `@Update` | `<update>` | 按 id 定位要修改的记录 |
| **删除** | `@Delete` | `<delete>` | 按 id 删除，返回影响行数 |

### 5.3 参数占位符：`#{}` vs `${}`

| 符号 | 处理方式 | SQL注入 | 适用场景 |
|------|----------|:------:|----------|
| `#{xxx}` | **预编译占位符** `?`，自动加引号 | ✅ 安全 | 参数值（多数情况） |
| `${xxx}` | **字符串拼接**，原样替换 | ❌ 危险 | 表名、列名（极少用，需白名单校验） |

```sql
-- #{username} 生成的 SQL：
select * from user where username = ?   -- 预编译，值安全传入

-- ${username} 生成的 SQL（危险！）：
select * from user where username = cds -- 直接拼接，没有引号
```

### 5.4 `@Param` 注解

当方法有多个参数时，用 `@Param` 给参数起别名：

```java
// ✅ 正确：用 @Param 指定 #{xxx} 中的名字
User findByUsernameAndPassword(@Param("username") String username,
                                @Param("password") String password);

// 如果只有一个实体对象参数，不需要 @Param
void insert(User user);  // #{username} 自动从 user.getUsername() 取
```

---

## 六、XML 映射文件——详细配置

### 6.1 XML 文件结构

`src/main/resources/com/cds/javaweb/MyBatis/Mapper/UserMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.cds.javaweb.MyBatis.Mapper.UserMapper">

    <select id="findAll" resultType="com.cds.javaweb.MyBatis.Pojo.User">
        select * from user
    </select>

</mapper>
```

### 6.2 核心规则

| 要素 | 说明 | 示例 |
|------|------|------|
| **namespace** | Mapper 接口的**全限定类名**（必须完全一致！） | `com.cds.javaweb.MyBatis.Mapper.UserMapper` |
| **id** | 方法名（必须一致） | `findAll` |
| **resultType** | 返回类型的**全限定类名** | `com.cds.javaweb.MyBatis.Pojo.User` |
| **parameterType** | 参数类型（可省略，MyBatis 自动推断） | — |

**配对关系：**
```
namespace = Mapper接口全限定名
id         = 接口方法名
→ 二者组合唯一确定一个 SQL 映射
```

### 6.3 XML 映射文件放在哪里？

**关键原则：XML 必须在 classpath 下（编译后在 `target/classes/` 中）。**

**方式一：放在 `resources/` 下（推荐）**

```
src/main/resources/
└── mapper/
    └── UserMapper.xml              ← 按功能分类存放
```

对应配置：
```properties
mybatis.mapper-locations=classpath:mapper/*.xml
```

**方式二：放在 `resources/` 下，与 Mapper 接口包路径一致**

```
src/main/resources/
└── com/cds/javaweb/MyBatis/Mapper/
    └── UserMapper.xml              ← 和接口同包路径
```

对应配置：
```properties
mybatis.mapper-locations=classpath:com/cds/javaweb/MyBatis/Mapper/*.xml
```

**方式三：放在 `src/main/java/` 下（不推荐，需额外配置）**

需要修改 `pom.xml`，让 Maven 也把 `src/main/java/` 下的 `.xml` 打包到 classpath：
```xml
<build>
    <resources>
        <resource>
            <directory>src/main/java</directory>
            <includes><include>**/*.xml</include></includes>
        </resource>
        <resource>
            <directory>src/main/resources</directory>
        </resource>
    </resources>
</build>
```

### 6.4 `mybatis.mapper-locations` 路径写法详解

路径是**相对于 classpath 根目录**（即 `target/classes/`）的通配符表达式：

| 写法 | 匹配范围 |
|------|----------|
| `classpath:mapper/*.xml` | `target/classes/mapper/` 下**一级**的所有 .xml |
| `classpath:mapper/**/*.xml` | `target/classes/mapper/` 下**所有子目录**的 .xml |
| `classpath:com/cds/**/Mapper/*.xml` | 按包路径通配 |
| `classpath*:mapper/**/*.xml` | 不止本项目，还搜依赖 JAR 包里的 |

多个路径用逗号分隔：
```properties
mybatis.mapper-locations=classpath:mapper/*.xml,classpath:mapper/**/*.xml
```

### 6.5 XML 四大操作标签

```xml
<mapper namespace="com.cds.javaweb.MyBatis.Mapper.UserMapper">

    <!-- 查询 -->
    <select id="findAll" resultType="com.cds.javaweb.MyBatis.Pojo.User">
        select * from user
    </select>

    <select id="findById" resultType="com.cds.javaweb.MyBatis.Pojo.User">
        select * from user where id = #{id}
    </select>

    <!-- 新增 -->
    <insert id="insert" parameterType="com.cds.javaweb.MyBatis.Pojo.User">
        insert into user(username, password, name, age)
        values(#{username}, #{password}, #{name}, #{age})
    </insert>

    <!-- 更新 -->
    <update id="update" parameterType="com.cds.javaweb.MyBatis.Pojo.User">
        update user set username=#{username}, password=#{password},
                        name=#{name}, age=#{age}
        where id = #{id}
    </update>

    <!-- 删除 -->
    <delete id="delete">
        delete from user where id = #{id}
    </delete>

</mapper>
```

---

## 七、注解 vs XML——如何选择？

| 维度 | 注解方式 | XML 方式 |
|------|----------|----------|
| **适用 SQL** | 简单 SQL | 复杂 SQL（多表关联、动态条件） |
| **可读性** | 直观，SQL 和代码在一起 | SQL 独立管理，结构清晰 |
| **维护性** | 改 SQL 需重新编译 | 改 SQL 只需重启应用 |
| **动态 SQL** | 不支持（或很弱） | 支持 `<if>`, `<where>`, `<foreach>` 等 |

**推荐策略：简单 SQL 用注解，复杂/动态 SQL 用 XML。两者可以混用。**

---

## 八、测试——验证 CRUD 操作

`src/test/java/.../JavaWebApplicationTests.java`：

```java
@SpringBootTest                           // 启动 Spring 容器，加载完整上下文
class JavaWebApplicationTests {

    @Autowired                            // 自动注入 Mapper 代理对象
    private UserMapper userMapper;

    @Test
    public void testFindAll() {           // 查全部
        List<User> userList = userMapper.findAll();
        userList.forEach(System.out::println);
    }

    @Test
    public void testDeleteById() {        // 按 id 删
        userMapper.delete(1);
    }

    @Test
    public void testInsert() {            // 新增
        User user = new User(5, "cds", "admin", "陈德圣", 18);
        userMapper.insert(user);
    }

    @Test
    public void testUpdate() {            // 更新
        User user = new User(1, "cds", "admin", "陈德圣", 18);
        userMapper.update(user);
    }

    @Test
    public void testFindByUsernameAndPassword() {  // 条件查询
        User user = userMapper.findByUsernameAndPassword("cds", "admin");
        System.out.println(user);
    }
}
```

---

## 九、整体架构回顾

```
┌─────────────────────────────────────────────────────┐
│                   浏览器 / Postman                    │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP 请求
                       ▼
┌─────────────────────────────────────────────────────┐
│                Controller 层（暂未涉及）               │
│              @RestController / @GetMapping            │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│               Mapper 接口（DAO 层）                    │
│   @Mapper  →  UserMapper  →  @Select/@Insert/...     │
│           接口          注解方式 或 XML 方式            │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              MyBatis 框架核心                          │
│    解析 XML/注解 → 生成 JDBC 代码 → 管理连接           │
│    → 执行 SQL → 结果映射为实体对象                      │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              HikariCP 连接池                           │
│        管理数据库连接 → 连接复用 → 连接回收              │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              MySQL 数据库 (web01)                      │
│                   user 表                              │
└─────────────────────────────────────────────────────┘
```

---

## 十、本项目当前配置一览

| 配置项 | 值 |
|--------|-----|
| Spring Boot 版本 | 3.3.0 |
| MyBatis Starter 版本 | 3.0.5 |
| MySQL 驱动 | mysql-connector-j |
| 连接池 | HikariCP（Spring Boot 默认） |
| 数据库 | web01 @ localhost:3306 |
| 实体类 | `com.cds.javaweb.MyBatis.Pojo.User` |
| Mapper 接口 | `com.cds.javaweb.MyBatis.Mapper.UserMapper` |
| XML 映射文件 | `resources/com/cds/javaweb/MyBatis/Mapper/UserMapper.xml` |
| XML 路径配置 | `mybatis.mapper-locations=classpath:com/cds/javaweb/MyBatis/Mapper/*.xml` |

---

## 十一、关键知识点速查

| 知识模块 | 核心要点 |
|----------|----------|
| **JDBC vs MyBatis** | MyBatis 封装了连接管理、结果映射、资源释放，SQL 与代码分离 |
| **@Mapper 注解** | 标识 Mapper 接口，框架自动生成代理对象并注入 Spring 容器 |
| **`#{}` 占位符** | 预编译占位 `?`，安全防注入，大多时候用它 |
| **`@Param`** | 多参数时指定 `#{}` 中使用的名字 |
| **XML namespace** | 必须等于 Mapper 接口全限定类名 |
| **XML id** | 必须等于接口方法名 |
| **resultType** | 返回实体类的全限定名 |
| **classpath** | 指 `target/classes/`，即编译后的 `src/main/resources/` |
| **HikariCP** | Spring Boot 默认连接池，自动配置，连接复用 |
| **日志输出** | `mybatis.configuration.log-impl=...StdOutImpl` 控制台打印 SQL |
| **注解 vs XML** | 简单 SQL 注解，复杂/动态 SQL 用 XML，可混用 |

---

> 📅 学习日期：2026-07-07  
> 📁 项目路径：`D4_MySQL/MyBatis/`
