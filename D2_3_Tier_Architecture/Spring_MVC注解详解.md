# Spring MVC 控制层注解详解

> 以 `UserController` 为例，说明三层架构中控制层各注解的意义与用法。

---

## 一、类级别注解

### `@RestController`

```java
@RestController
public class UserController { ... }
```

`@Controller` + `@ResponseBody` 的合体：

| 对比 | `@Controller` | `@RestController` |
|------|:--:|:--:|
| 返回值 | 视图名（跳转页面） | **直接把对象序列化为 JSON** |
| 每个方法写 `@ResponseBody` | 需要 | 不需要，自动生效 |

用 `@RestController` 意味着所有方法的返回值都会被 Jackson 自动转成 JSON 写入响应体——这是 RESTful API 的标准行为。

---

### `@RequestMapping("/api/users")`

```java
@RequestMapping("/api/users")
public class UserController { ... }
```

给整个 Controller 的所有接口统一加路径前缀。最终请求路径 = **类上路径 + 方法上路径**：

| 方法上的映射 | 实际访问地址 |
|-------------|-------------|
| `@GetMapping`（无参数） | `GET /api/users` |
| `@GetMapping("/{id}")` | `GET /api/users/1` |
| `@PostMapping` | `POST /api/users` |
| `@PutMapping("/{id}")` | `PUT /api/users/1` |
| `@DeleteMapping("/{id}")` | `DELETE /api/users/1` |

---

## 二、方法级别注解 — HTTP 方法映射

这四个是 `@RequestMapping(method = ...)` 的快捷写法：

| 注解 | 等价写法 | 用途 |
|------|---------|------|
| `@GetMapping` | `@RequestMapping(method = GET)` | 查询 |
| `@PostMapping` | `@RequestMapping(method = POST)` | 新增 |
| `@PutMapping` | `@RequestMapping(method = PUT)` | 更新（全量替换） |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` | 删除 |

### RESTful 命名约定

```java
@GetMapping              // GET    /api/users     → 查询全部
@GetMapping("/{id}")     // GET    /api/users/1   → 查询单个
@PostMapping             // POST   /api/users     → 新增（请求体带数据）
@PutMapping("/{id}")     // PUT    /api/users/1   → 更新（请求体带新数据）
@DeleteMapping("/{id}")  // DELETE /api/users/1   → 删除
```

同一个 URL `/api/users`，靠 HTTP 方法区分操作——这就是 RESTful 风格的核心。

### `@*Mapping` 的常用参数

```java
@GetMapping(
    value    = "/{id}",                // 路径
    produces = "application/json",     // 限制响应的 Content-Type
    consumes = "application/json"      // 限制请求的 Content-Type
)
```

---

## 三、参数绑定注解

### `@PathVariable` — 取 URL 路径中的变量

```java
@GetMapping("/{id}")                    // {id} 是路径占位符
public ResponseEntity<User> getUserById(
    @PathVariable Integer id            // 自动取出 URL 中 id 的值，并转为 Integer
) { ... }
```

- 请求 `GET /api/users/5` → `id = 5`
- Spring 自动完成类型转换（URL 字符串 → `Integer`、`Long` 等）

---

### `@RequestBody` — 取请求体中的 JSON，反序列化为对象

```java
@PostMapping
public ResponseEntity<User> createUser(
    @RequestBody User user              // JSON 字符串 → User 对象
) { ... }
```

请求示例：
```http
POST /api/users
Content-Type: application/json

{"name": "张三", "email": "zhangsan@example.com"}
```

Spring 用 Jackson 自动把 JSON 转成 `User` 对象：
- `"name"` → `user.setName("张三")`
- `"email"` → `user.setEmail("zhangsan@example.com")`

> **前提**：`User` 类必须有**无参构造器** + 各字段的 **setter 方法**。

---

### `@RequestParam` — 取 URL 查询参数

```java
// GET /api/users/search?name=张三&age=20
@GetMapping("/search")
public List<User> search(
    @RequestParam String name,          // name = "张三"
    @RequestParam(defaultValue = "0") Integer age  // age = 20，不传则默认 0
) { ... }
```

| 参数 | 说明 |
|------|------|
| `value` / `name` | 参数名，默认与方法参数名一致 |
| `required` | 是否必传，默认 `true` |
| `defaultValue` | 不传时的默认值 |

---

### `@RequestHeader` — 取请求头

```java
@GetMapping("/me")
public String getCurrentUser(
    @RequestHeader("Authorization") String token
) { ... }
```

---

## 四、校验注解（配合 `@Valid`）

```java
@PostMapping
public ResponseEntity<User> createUser(
    @Valid @RequestBody User user       // 触发校验
) { ... }
```

在 `User` 实体类字段上配合使用：

| 注解 | 作用 |
|------|------|
| `@NotBlank` | 字符串不能为 null 且不能全是空白 |
| `@NotEmpty` | 集合/字符串不能为空 |
| `@NotNull` | 不能为 null |
| `@Email` | 必须是合法邮箱格式 |
| `@Size(min, max)` | 字符串/集合长度限制 |

---

## 五、异常处理注解

### `@ExceptionHandler` — 统一异常处理

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

把 Service 层抛出的 `IllegalArgumentException` 转成 HTTP 400 + 友好提示，而不是暴露 500 错误。

> 更规范的做法是配合 `@ControllerAdvice` 做全局异常处理。

---

## 六、Spring Bean 管理与生命周期注解

除了控制层，Service 层和 DAO 层也有两个关键的注解。

### `@Repository` — 数据访问层组件

```java
@Repository
public class UserDaoImpl implements UserDao { ... }
```

它是 `@Component` 的**特化版本**，专门标记 DAO（数据访问层）类：

| 注解 | 所在层 | 作用 |
|------|--------|------|
| `@Controller` / `@RestController` | 控制层 | 处理 HTTP 请求 |
| `@Service` | 业务层 | 处理业务逻辑 |
| `@Repository` | 数据访问层 | 操作数据库 |

它们本质都是 `@Component`，Spring 会自动扫描并创建单例 Bean 放入 IoC 容器。**特化的好处**：

- **语义更清晰**：一眼就知道这个类的职责
- **`@Repository` 独有能力**：自动把数据库异常（如 JDBC 异常）翻译成 Spring 的 `DataAccessException`，让异常体系统一

```java
// 三层各自的注解，一目了然：
@RestController  // ← UserController
@Service         // ← UserServiceImpl
@Repository      // ← UserDaoImpl
```

> 你项目中 `UserServiceImpl` 用的 `@Service` 也是同理，标记业务逻辑层组件。

---

### `@PostConstruct` — Bean 初始化完成后自动执行

```java
@Repository
public class UserDaoImpl implements UserDao {

    @PostConstruct
    public void init() {
        // Bean 创建完成后，Spring 自动调用这个方法
        // 在这里加载初始数据
    }
}
```

**执行时机**：Spring 容器完成依赖注入（构造器、setter 全部完成）之后，自动调用一次。

```text
Spring 启动流程：
  ① 扫描 → 发现 @Repository
  ② 实例化 → new UserDaoImpl()
  ③ 依赖注入 → 如果有 @Autowired/@Resource 就在这注入
  ④ @PostConstruct → 调用 init()   ← 我们在这里加载 JSON 数据
  ⑤ Bean 就绪 → 可以处理请求了
```

**典型使用场景**：

| 场景 | 示例 |
|------|------|
| 加载初始数据 | 从 `resources/data/users.json` 读数据到内存 |
| 初始化连接 | 建立数据库连接池、预热缓存 |
| 启动检查 | 验证必要的配置文件是否存在 |
| 注册/订阅 | 向消息队列注册消费者 |

和你项目代码的对应关系：

```java
@PostConstruct   // ← 告诉 Spring："构造完就调我"
public void init() {
    // 读取 resources/data/users.json
    // 把 3 条用户数据加载到 ConcurrentHashMap 中
    // 这样启动后 GET /api/users 直接就有数据返回
}

// init() 执行完 → userDB 里有数据了 → 其他方法才能正常工作
public List<User> findAll() {
    return new ArrayList<>(userDB.values());  // ← 返回已加载的数据
}
```

> `@PostConstruct` 来自 `jakarta.annotation` 包（Java EE 标准注解），不是 Spring 独有的，但在 Spring 中同样生效。

---

## 七、一个请求的完整流转

```
浏览器请求：GET http://localhost:8080/api/users/1

    ↓
① @RestController           ← 标记这是一个 REST 控制器
    ↓
② @RequestMapping("/api/users")  ← 匹配路径前缀 /api/users
    ↓
③ @GetMapping("/{id}")      ← 匹配 GET 方法 + /1 → id = 1
    ↓
④ @PathVariable Integer id  ← 把 URL 字符串 "1" 转为 Integer 类型
    ↓
⑤ userService.getUserById(1)  ← 调用 Service → DAO → 内存 Map
    ↓
⑥ return ResponseEntity.ok(user)  ← 把 User 对象序列化为 JSON
    ↓
浏览器收到：{"id":1,"name":"张三","email":"zhangsan@example.com"}
```

---

## 八、注解速查表

### 控制层

| 注解 | 作用位置 | 用途 |
|------|---------|------|
| `@RestController` | 类 | 声明 REST 控制器，返回值自动序列化为 JSON |
| `@RequestMapping` | 类/方法 | 映射请求路径 |
| `@GetMapping` | 方法 | 映射 GET 请求（查询） |
| `@PostMapping` | 方法 | 映射 POST 请求（新增） |
| `@PutMapping` | 方法 | 映射 PUT 请求（全量更新） |
| `@DeleteMapping` | 方法 | 映射 DELETE 请求（删除） |
| `@PatchMapping` | 方法 | 映射 PATCH 请求（部分更新） |
| `@PathVariable` | 参数 | 绑定 URL 路径变量 `/{id}` |
| `@RequestBody` | 参数 | 绑定请求体 JSON → 对象 |
| `@RequestParam` | 参数 | 绑定 URL 查询参数 `?key=value` |
| `@RequestHeader` | 参数 | 绑定请求头 |
| `@Valid` / `@Validated` | 参数 | 触发 Bean 校验 |
| `@ExceptionHandler` | 方法 | 处理指定异常 |
| `@ControllerAdvice` | 类 | 全局异常处理 / 全局数据绑定 |

### Bean 管理与生命周期

| 注解 | 作用位置 | 用途 |
|------|---------|------|
| `@Component` | 类 | 通用 Spring Bean（以下三个的父注解） |
| `@Controller` | 类 | 控制层组件（返回视图） |
| `@Service` | 类 | 业务逻辑层组件 |
| `@Repository` | 类 | 数据访问层组件（自动翻译数据库异常） |
| `@PostConstruct` | 方法 | Bean 初始化完成后自动调用一次 |
