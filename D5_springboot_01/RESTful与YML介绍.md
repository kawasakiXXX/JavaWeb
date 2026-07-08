# RESTful 与传统 URL 的区别 & Spring Boot 配置文件对比

---

## 一、RESTful 与传统 URL 的区别

### 1.1 传统 URL（面向操作）

传统 Web 应用中，URL 通常体现的是**对资源的操作行为**，动词直接出现在路径中：

```
GET  /getUserById?id=1          # 查询用户
GET  /getAllUsers               # 查询所有用户
POST /addUser                   # 新增用户
POST /updateUser                # 修改用户
POST /deleteUser?id=1           # 删除用户
GET  /exportUserExcel           # 导出用户 Excel
```

**特点：**
- URL 中包含动词（get、add、update、delete）
- 请求方式几乎只用 `GET` 和 `POST`
- 参数通过 `?key=value` 拼接在 URL 上
- 同一个资源的不同操作对应多个不同的 URL

### 1.2 RESTful URL（面向资源）

RESTful 将服务端的一切抽象为**资源**，通过 **HTTP 方法**（GET/POST/PUT/DELETE）来表达对该资源的操作：

```
GET     /users              # 查询所有用户
GET     /users/1            # 查询 ID 为 1 的用户
POST    /users              # 新增用户（请求体携带数据）
PUT     /users/1            # 修改 ID 为 1 的用户
DELETE  /users/1            # 删除 ID 为 1 的用户
```

**特点：**
- URL 中只有**名词**（资源名），没有动词
- 使用 HTTP 方法区分操作：`GET`=查、`POST`=增、`PUT`=改、`DELETE`=删
- 通过路径参数 `/users/{id}` 定位具体资源
- 同一个 URL 配合不同 HTTP 方法实现不同操作

### 1.3 对比总结

| 维度 | 传统 URL | RESTful URL |
|------|----------|-------------|
| **URL 语义** | 体现操作（动词+名词） | 体现资源（纯名词） |
| **HTTP 方法** | 主要使用 GET / POST | 使用 GET / POST / PUT / DELETE |
| **资源定位** | 通过 Query 参数 `?id=1` | 通过路径 `/users/1` |
| **增删改查** | 4 个不同 URL | 1~2 个 URL + 不同方法 |
| **可读性** | 功能导向，直观但冗余 | 资源导向，简洁统一 |
| **状态码运用** | 较少区分 | 充分利用 HTTP 状态码（200/201/204/400/404/500） |

### 1.4 Spring Boot 中的 RESTful 示例

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping                  // GET  /users
    public List<User> list() { ... }

    @GetMapping("/{id}")        // GET  /users/1
    public User getById(@PathVariable Long id) { ... }

    @PostMapping                // POST /users
    public User create(@RequestBody User user) { ... }

    @PutMapping("/{id}")        // PUT  /users/1
    public User update(@PathVariable Long id, @RequestBody User user) { ... }

    @DeleteMapping("/{id}")     // DELETE /users/1
    public void delete(@PathVariable Long id) { ... }
}
```

---

## 二、Spring Boot 配置文件：properties 与 yml 的区别

Spring Boot 支持两种主流配置文件格式：`application.properties` 和 `application.yml`（或 `.yaml`）。

### 2.1 application.properties（传统格式）

采用 `key=value` 的平铺结构，使用 `.` 分隔层级：

```properties
# 服务器配置
server.port=8080
server.servlet.context-path=/demo

# 数据库配置
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=123456

# MyBatis 配置
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.pojo

# 自定义配置
app.jwt.secret=abc123
app.jwt.expire=3600
```

### 2.2 application.yml（YAML 格式）

采用**缩进层级**表达嵌套关系，更接近结构化数据：

```yaml
# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /demo

# 数据库配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: 123456

# MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.pojo

# 自定义配置
app:
  jwt:
    secret: abc123
    expire: 3600
```

### 2.3 核心区别对比

| 维度 | application.properties | application.yml |
|------|----------------------|-----------------|
| **语法** | `key=value` 平铺结构 | 缩进层级结构（类似 Python） |
| **层级表达** | 用 `.` 分隔，逐行展开 | 用**缩进**（空格，通常 2 格）表达父子 |
| **冗余度** | 前缀重复，较冗余 | 层级聚合，更简洁 |
| **可读性** | 配置项少时清晰 | 配置层级深、项多时更直观 |
| **大小写** | 大小写不敏感（约定小写） | 大小写敏感 |
| **多环境支持** | 需要多个文件或 profile 块 | 可用 `---` 分隔符在单文件中定义多环境 |
| **列表/数组** | 逗号分隔：`list=a,b,c` | 支持原生列表语法（`-` 前缀） |
| **学习成本** | 极低，所见即所得 | 需注意缩进（缩进错误会导致解析失败） |
| **优先级** | 同时存在时 **properties 优先级更高** | yml 优先级低于 properties |

### 2.4 YAML 特殊特性

**① 多环境配置（单文件）：**
```yaml
# 公共配置
server:
  port: 8080

---
# 开发环境
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081

---
# 生产环境
spring:
  config:
    activate:
      on-profile: prod
server:
  port: 80
```

**② 原生列表语法：**
```yaml
# YAML 列表
servers:
  - host1.example.com
  - host2.example.com
  - host3.example.com
```

```properties
# properties 列表（逗号分隔）
servers=host1.example.com,host2.example.com,host3.example.com
```

**③ 引用与变量：**
```yaml
base-path: /api/v1
user-url: ${base-path}/users    # 引用上文定义的 base-path
```

### 2.5 如何选择？

- **小型项目 / 配置简单** → `application.properties` 足够，简单直接
- **配置层级深 / 配置项多** → `application.yml` 更清晰，避免大量重复前缀
- **团队协作** → 统一选用一种即可，避免两种混用造成混淆
- **注意优先级**：同时存在两种文件时，`application.properties` 的配置会覆盖 `application.yml` 中的同名配置

---

*2026-07-08 整理*
