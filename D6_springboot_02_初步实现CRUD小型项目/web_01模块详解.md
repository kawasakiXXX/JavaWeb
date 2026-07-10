# web_01 模块详解

> Spring Boot 3 + MyBatis 实现的用户管理 RESTful API 模块，采用经典三层架构（Controller → Service → Mapper），提供用户的增删改查接口。

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 技术栈](#2-技术栈)
- [3. 项目结构](#3-项目结构)
- [4. 配置文件详解](#4-配置文件详解)
- [5. 分层架构详解](#5-分层架构详解)
  - [5.1 POJO 层 —— 实体类](#51-pojo-层--实体类)
  - [5.2 Mapper 层 —— 数据访问层](#52-mapper-层--数据访问层)
  - [5.3 Service 层 —— 业务逻辑层](#53-service-层--业务逻辑层)
  - [5.4 Controller 层 —— 控制层](#54-controller-层--控制层)
- [6. 数据流向图](#6-数据流向图)
- [7. API 接口文档](#7-api-接口文档)
- [8. 数据库设计](#8-数据库设计)
- [9. 运行指南](#9-运行指南)
- [10. 设计亮点与注意事项](#10-设计亮点与注意事项)

---

## 1. 项目概览

`web_01` 是 `JavaWeb_demo` 项目中的一个子模块，包路径为 `com.cds.javaweb.web_01`。它是一个标准的 **Spring Boot + MyBatis** 单体后端应用模块，围绕 `user` 表实现了完整的 CRUD（增删改查）RESTful API。

- **基础路径**：`/web01`
- **请求/响应格式**：统一使用 JSON，通过 `Result` 类进行封装
- **数据库**：MySQL，数据库名为 `web01`

---

## 2. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.0 | 应用框架 |
| MyBatis | 3.0.5（starter） | ORM / 数据库访问 |
| MySQL Connector | —（runtime） | MySQL JDBC 驱动 |
| Lombok | 1.18.44 | 简化 POJO 代码 |
| Maven | — | 项目构建与依赖管理 |

---

## 3. 项目结构

```
JavaWeb_demo/
├── pom.xml                                    # Maven 父 POM，管理依赖
├── src/main/
│   ├── java/com/cds/javaweb/
│   │   ├── JavaWebApplication.java            # Spring Boot 启动类
│   │   └── web_01/                            # ← 本模块
│   │       ├── controller/
│   │       │   └── UserController.java        # 控制层：处理 HTTP 请求
│   │       ├── mapper/
│   │       │   └── UserMapper.java            # 数据访问层：MyBatis 注解式 SQL
│   │       ├── pojo/
│   │       │   ├── User.java                  # 实体类：用户模型
│   │       │   └── Result.java                # 统一响应结果封装
│   │       └── services/
│   │           ├── UserServices.java           # 服务层接口
│   │           └── UserServicesImpl.java       # 服务层实现
│   └── resources/
│       └── application.yml                    # Spring Boot 全局配置
├── 端口进程查询和关闭命令.txt                   # 辅助命令速查
├── HELP.md                                    # Spring Initializr 生成的项目帮助
├── mvnw / mvnw.cmd                            # Maven Wrapper（跨平台构建脚本）
└── .gitignore / .gitattributes                # Git 配置
```

模块内部按 **三层架构** 组织：

```
web_01/
├── controller/   ← 表现层（接收请求、返回响应）
├── services/     ← 业务逻辑层（接口 + 实现）
├── mapper/       ← 数据访问层（MyBatis Mapper）
└── pojo/         ← 数据模型（实体类 + 通用封装）
```

---

## 4. 配置文件详解

**文件位置**：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: JavaWeb
  datasource:
    url: jdbc:mysql://localhost:3306/web01          # 数据库连接 URL
    driver-class-name: com.mysql.cj.jdbc.Driver      # MySQL JDBC 驱动
    username: root                                   # 数据库用户名
    password: '@Likekawasaki6'                       # 数据库密码

mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志输出到控制台
    map-underscore-to-camel-case: true                      # 开启驼峰命名自动映射
  mapper-locations: classpath:com/cds/javaweb/web_01/mapper/*.xml  # XML Mapper（预留）
```

### 关键配置说明

| 配置项 | 说明 |
|--------|------|
| `datasource.url` | 连接本地 MySQL 的 `web01` 数据库 |
| `map-underscore-to-camel-case: true` | **重要**：自动将数据库下划线字段（如 `create_time`）映射为 Java 驼峰属性（`createTime`），无需手动 `@Results` 注解 |
| `log-impl: StdOutImpl` | 开发阶段便于调试，每个 SQL 语句及参数都会打印到控制台 |
| `mapper-locations` | 预留 XML 映射文件路径，当前模块主要使用注解方式，未实际放置 XML 文件 |

---

## 5. 分层架构详解

### 5.1 POJO 层 —— 实体类

#### User.java —— 用户实体

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer id;
    private String name;
    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Integer` | 主键，自增 |
| `name` | `String` | 用户名 |
| `age` | `Integer` | 年龄 |
| `createTime` | `LocalDateTime` | 创建时间（Java 驼峰 ↔ 数据库 `create_time`） |
| `updateTime` | `LocalDateTime` | 更新时间（Java 驼峰 ↔ 数据库 `update_time`） |

> **Lombok 注解**：`@Data` 自动生成 getter/setter/toString/equals/hashCode；`@AllArgsConstructor` / `@NoArgsConstructor` 生成全参/无参构造方法。

#### Result.java —— 统一响应结果

```java
@Data
public class Result {
    private Integer code;   // 1 = 成功，0 = 失败
    private String msg;     // 提示消息
    private Object data;    // 响应数据（泛型擦除为 Object）

    public static Result success() { ... }         // 无数据成功
    public static Result success(Object object) { ... }  // 带数据成功
    public static Result error(String msg) { ... }        // 失败
}
```

**设计意图**：所有 API 返回统一的 JSON 结构，前端无需判断不同接口的返回格式：

```json
// 成功示例
{ "code": 1, "msg": "操作成功", "data": [...] }

// 失败示例
{ "code": 0, "msg": "用户不存在", "data": null }
```

---

### 5.2 Mapper 层 —— 数据访问层

**文件**：`UserMapper.java`

使用 **MyBatis 注解方式** 直接编写 SQL，无需 XML 映射文件。`@Mapper` 注解使 MyBatis 在运行时自动生成该接口的代理实现类并注入 Spring 容器。

| 方法 | SQL 注解 | 说明 |
|------|----------|------|
| `findAll()` | `@Select("select ... from user order by id desc")` | 查询全部用户，按 `id` 倒序 |
| `deleteById(int id)` | `@Delete("delete from user where id = #{id}")` | 按主键删除用户 |
| `insert(User user)` | `@Insert("insert into user (name, age, create_time, update_time) values (...)")` | 新增用户（id 自增，无需传入） |
| `findById(int id)` | `@Select("select ... from user where id = #{id}")` | 按主键查询单个用户 |
| `update(User user)` | `@Update("update user set name=..., age=..., update_time=... where id=...")` | 按主键更新用户信息 |

> **`#{}` 占位符**：MyBatis 的参数占位符，会自动从方法参数或对象属性中取值，并使用预编译（PreparedStatement）防止 SQL 注入。

---

### 5.3 Service 层 —— 业务逻辑层

#### 接口：`UserServices.java`

定义了 5 个业务方法签名，与 Mapper 层一一对应：

```java
public interface UserServices {
    List<User> findAll();
    void deleteById(int id);
    void insert(User user);
    User findById(int id);
    void update(User user);
}
```

#### 实现：`UserServicesImpl.java`

- 通过 `@Service` 注解注册为 Spring Bean
- 通过 `@Autowired` 注入 `UserMapper`
- **关键业务逻辑**：
  - `insert()`：在插入前自动填充 `createTime` 和 `updateTime` 为当前时间
  - `update()`：在更新前自动刷新 `updateTime` 为当前时间

```java
@Override
public void insert(User user) {
    user.setCreateTime(LocalDateTime.now());
    user.setUpdateTime(LocalDateTime.now());
    userMapper.insert(user);
}

@Override
public void update(User user) {
    user.setUpdateTime(LocalDateTime.now());
    userMapper.update(user);
}
```

> 这种设计将时间戳管理放在 Service 层而非数据库默认值，保持了业务逻辑的集中可控性。

---

### 5.4 Controller 层 —— 控制层

**文件**：`UserController.java`

| 注解 | 值 | 说明 |
|------|-----|------|
| `@RestController` | — | 标识为 REST 控制器，方法返回值自动序列化为 JSON |
| `@RequestMapping` | `/web01` | 该 Controller 下所有接口的基础路径 |

#### 接口与 HTTP 方法映射

| HTTP 方法 | 路径 | 方法 | 说明 |
|-----------|------|------|------|
| `GET` | `/web01` | `findAll()` | 查询全部用户 |
| `GET` | `/web01/{id}` | `findById(id)` | 按 ID 查询用户 |
| `POST` | `/web01` | `add(user)` | 新增用户（请求体 JSON） |
| `PUT` | `/web01` | `update(user)` | 更新用户（请求体 JSON） |
| `DELETE` | `/web01/{id}` | `delete(id)` | 按 ID 删除用户 |

**注解解析**：

| 注解 | 作用 | 示例 |
|------|------|------|
| `@GetMapping` | 映射 GET 请求 | `@GetMapping("/{id}")` → `/web01/5` |
| `@PostMapping` | 映射 POST 请求 | 请求体自动绑定到 `@RequestBody User user` |
| `@PutMapping` | 映射 PUT 请求 | 同上 |
| `@DeleteMapping` | 映射 DELETE 请求 | `@DeleteMapping("/{id}")` → `/web01/5` |
| `@PathVariable` | 提取 URL 路径变量 | `/{id}` → 方法参数 `Integer id` |
| `@RequestBody` | 将请求体 JSON 反序列化为 Java 对象 | POST/PUT 请求体 → `User` 对象 |

#### 特点

- 每个方法都会在控制台 `System.out.println` 输出操作日志，便于开发调试
- 所有返回值都通过 `Result.success()` 封装，保证前端解析一致性
- 严格遵循 RESTful 风格：同一 URL（`/web01`）通过不同 HTTP 方法区分操作

---

## 6. 数据流向图

```
HTTP 请求（JSON）
    │
    ▼
┌─────────────────────────────────────────────┐
│  UserController  (@RestController)          │
│  - 接收 HTTP 请求                           │
│  - 解析路径参数 / 请求体                     │
│  - 调用 Service 层方法                      │
│  - 将结果封装为 Result 并序列化为 JSON 返回  │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│  UserServicesImpl  (@Service)               │
│  - 处理业务逻辑（时间戳填充等）               │
│  - 调用 Mapper 层方法                       │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│  UserMapper  (@Mapper)                      │
│  - 注解式 SQL：@Select / @Insert / @Update  │
│  - MyBatis 自动生成代理实现                  │
└──────────────┬──────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────┐
│  MySQL 数据库  (web01.user)                 │
└─────────────────────────────────────────────┘
```

这是一条典型的 **自上而下、单向依赖** 的调用链：Controller → Service → Mapper → DB。上层依赖下层，下层不感知上层。

---

## 7. API 接口文档

### 基础信息

- **Base URL**：`http://localhost:8080/web01`
- **Content-Type**：`application/json`

### 接口列表

#### 7.1 查询全部用户

```
GET /web01
```

**响应示例**：
```json
{
  "code": 1,
  "msg": "操作成功",
  "data": [
    {
      "id": 3,
      "name": "张三",
      "age": 25,
      "createTime": "2025-01-01T10:00:00",
      "updateTime": "2025-01-01T10:00:00"
    }
  ]
}
```

#### 7.2 按 ID 查询用户

```
GET /web01/{id}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | Path | Integer | 是 | 用户主键 ID |

#### 7.3 新增用户

```
POST /web01
```

**请求体**：
```json
{
  "name": "李四",
  "age": 30
}
```

> `id`、`createTime`、`updateTime` 由后端自动填充，无需传递。

#### 7.4 更新用户

```
PUT /web01
```

**请求体**：
```json
{
  "id": 3,
  "name": "张三（已修改）",
  "age": 26
}
```

> 前端需要回传 `id` 以定位记录；`updateTime` 由 Service 层自动刷新。

#### 7.5 删除用户

```
DELETE /web01/{id}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `id` | Path | Integer | 是 | 要删除的用户主键 ID |

---

## 8. 数据库设计

### 数据库信息

- **数据库名**：`web01`
- **主机**：`localhost:3306`
- **字符集**：推荐 `utf8mb4`

### 表结构（推断）

根据 Mapper 中的 SQL 语句，`user` 表结构如下：

```sql
CREATE TABLE `user` (
    `id`          INT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `name`        VARCHAR(50)  DEFAULT NULL             COMMENT '用户名',
    `age`         INT          DEFAULT NULL             COMMENT '年龄',
    `create_time` DATETIME     DEFAULT NULL             COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 字段映射关系

| 数据库字段（下划线） | Java 属性（驼峰） | 类型 | 说明 |
|---------------------|-------------------|------|------|
| `id` | `id` | `INT` → `Integer` | 主键自增 |
| `name` | `name` | `VARCHAR` → `String` | 用户名 |
| `age` | `age` | `INT` → `Integer` | 年龄 |
| `create_time` | `createTime` | `DATETIME` → `LocalDateTime` | 自动驼峰映射 |
| `update_time` | `updateTime` | `DATETIME` → `LocalDateTime` | 自动驼峰映射 |

---

## 9. 运行指南

### 前置条件

1. **JDK 17+** 已安装并配置环境变量
2. **MySQL** 已启动，创建了 `web01` 数据库和 `user` 表
3. 根据本地环境修改 `application.yml` 中的数据库连接信息

### 启动步骤

```bash
# 1. 进入项目根目录
cd JavaWeb_demo

# 2. 使用 Maven Wrapper 编译并启动（Windows）
mvnw.cmd spring-boot:run

# 或 Linux / macOS
./mvnw spring-boot:run

# 3. 应用默认运行在 http://localhost:8080
```

### 端口冲突处理

如果 8080 端口被占用，可参考项目根目录下的 `端口进程查询和关闭命令.txt`：

```bash
# 查看占用 8080 端口的进程
netstat -ano | findstr :8080

# 根据 PID 查看进程名
tasklist | findstr <PID>

# 强制终止进程
taskkill /PID <PID> /F
```

### 验证接口

启动后可使用 `curl` 或 Postman 测试：

```bash
# 查询全部用户
curl http://localhost:8080/web01

# 按 ID 查询
curl http://localhost:8080/web01/1

# 新增用户
curl -X POST http://localhost:8080/web01 \
  -H "Content-Type: application/json" \
  -d '{"name":"测试用户","age":20}'

# 更新用户
curl -X PUT http://localhost:8080/web01 \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"新名字","age":25}'

# 删除用户
curl -X DELETE http://localhost:8080/web01/1
```

---

## 10. 设计亮点与注意事项

### 设计亮点

1. **RESTful API 规范**：同一资源路径通过 HTTP 方法区分操作，语义清晰
2. **统一响应格式**：`Result` 类封装 `code` + `msg` + `data`，前后端约定明确
3. **驼峰自动映射**：通过 `map-underscore-to-camel-case: true` 避免了繁琐的 `@Results` 手动映射
4. **注解式 MyBatis**：简单 CRUD 使用注解 SQL，无需维护 XML 文件，代码紧凑
5. **时间戳自动管理**：`createTime` / `updateTime` 由 Service 层统一控制，而非依赖数据库默认值或前端传入
6. **Lombok 精简代码**：实体类无需手写 getter/setter/构造方法

### 可改进方向

| 改进点 | 建议 |
|--------|------|
| **日志** | 用 SLF4J/Logback 替代 `System.out.println`，支持日志级别控制和文件输出 |
| **异常处理** | 添加全局异常处理器（`@RestControllerAdvice`），统一处理 `404`/`500` 等错误 |
| **参数校验** | 引入 `spring-boot-starter-validation`，对请求参数进行非空/格式校验 |
| **分页查询** | `findAll` 在数据量大时应支持分页（MyBatis PageHelper 或手动 LIMIT） |
| **事务管理** | 对写操作添加 `@Transactional` 注解，保证数据一致性 |
| **DTO 分离** | 区分 Entity（DO）和请求/响应 DTO，避免直接暴露数据库实体 |
| **接口文档** | 引入 Swagger / SpringDoc，自动生成 API 文档页面 |

---

> **总结**：`web_01` 是一个典型的 Spring Boot 入门级 CRUD 模块，完整展示了 Controller → Service → Mapper → DB 的标准三层架构，适合作为 Spring Boot + MyBatis 初学者的参考项目。
