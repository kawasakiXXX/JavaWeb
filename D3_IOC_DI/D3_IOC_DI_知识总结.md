# D3 分层解耦 — IoC / DI / Bean 知识总结

---

## 一、控制反转（IoC — Inversion of Control）

### 什么是 IoC？

**对象的创建权从"调用者"反转到"Spring 容器"**，这就是控制反转。

```
❌ 传统方式（自己控制）：
   UserDao dao = new UserDaoImpl();        // 你决定何时 new、new 哪个实现
   UserService service = new UserServiceImpl(dao);  // 你手动组装依赖链

✅ IoC 方式（容器控制）：
   @Service
   public class UserServiceImpl implements UserService {
       private final UserDao userDao;
       public UserServiceImpl(UserDao userDao) { ... }  // 只声明依赖，不 new
   }
   // Spring 容器负责：创建对象 → 组装依赖 → 管理生命周期 → 销毁对象
```

### IoC 容器的本质

Spring IoC 容器 = 一个巨大的 **Map<String, Object>**（BeanFactory），key 是 Bean 名称，value 是 Bean 实例。启动时扫描注解，把对象创建好放入这个 Map，需要时取出。

### Bean 生命周期

```
构造器 → 依赖注入 → @PostConstruct → 运行中 → @PreDestroy → 销毁
```

在项目中观察 `UserDaoImpl`：启动时控制台打印"正在创建 Bean"，关闭时打印"Bean 即将销毁"。

---

## 二、依赖注入（DI — Dependency Injection）

### 什么是 DI？

**依赖注入是 IoC 的一种实现方式**。对象不需要自己创建依赖，而是由容器"注入"进来。

```
IoC 是思想（控制权反转），DI 是手段（注入依赖）。
```

### DI 的三种方式

项目中三种方式都有展示，对照如下：

#### 方式一：构造器注入（✅ 推荐）

```java
// UserController.java / UserServiceImpl.java
private final UserService userService;         // ← 可以是 final

public UserController(UserService userService) {
    this.userService = userService;
}
```

| 优点 | 缺点 |
|------|------|
| 依赖可以是 `final`，不可变 | 依赖多时构造器参数列表长 |
| 启动时即发现依赖缺失 | |
| 单元测试可直接 `new` 传入 mock | |
| 单构造器时 `@Autowired` 可省略 | |

> **适用**：必需的依赖（没有这个依赖，类无法工作）

---

#### 方式二：Setter 注入（⚠️ 适合可选依赖）

```java
// UserServiceImpl.java
private String appInfo = "未注入";     // ← 有默认值，非 final

@Autowired(required = false)          // ← 依赖缺失也不报错
public void setAppInfo(@Qualifier("appInfo") String appInfo) {
    this.appInfo = appInfo;
}
```

| 优点 | 缺点 |
|------|------|
| 可选依赖（`required = false`），缺失不报错 | 不能 `final` |
| 可在 setter 内加校验/日志 | 依赖可能随时被修改 |

> **适用**：非必需的、有默认值的依赖

---

#### 方式三：字段注入（⚠️ 不推荐生产使用）

```java
// UserController.java
@Autowired
@Qualifier("serverStartTime")
private String serverStartTime;        // ← 非 final，无默认值
```

| 优点 | 缺点 |
|------|------|
| 代码最短 | 不能 `final`（反射在构造器后赋值，javac 不认） |
| | 隐藏依赖（看字段列表看不出依赖关系） |
| | 不便测试（必须启动 Spring 容器才能注入） |

> **为什么 `@Autowired private final` 编译报错？**
> Java 规定 `final` 字段必须在构造器内赋值。`@Autowired` 反射注入发生在构造器执行**之后**，javac 拒绝这种"事后赋值"。**只有构造器注入能让依赖成为 final。**

---

### 三种方式速查

| | 构造器注入 | Setter 注入 | 字段注入 |
|---|---|---|---|
| **字段能否 final** | ✅ | ❌ | ❌ |
| **启动时发现缺失** | ✅ 报错 | ✅ 可配置 | ✅ 报错 |
| **单元测试友好** | ✅ 直接 new | ✅ 调 setter | ❌ 需反射 |
| **代码简洁度** | 中等 | 较繁 | 最简 |
| **Spring 推荐** | ✅ | 可选依赖时 | ❌ |
| **本项目位置** | Controller + Service | ServiceImpl | Controller |

---

## 三、同类型多实例的解决方案

当接口有多个实现（或同类型有多个 Bean），Spring 不知道注入哪一个，需要明确指定。

### 方案一：`@Qualifier`（Spring 原生）

```java
// AppConfig 中注册了两个 String Bean："serverStartTime" 和 "appInfo"
// 注入时必须用 @Qualifier 指定名称

@Autowired
@Qualifier("serverStartTime")      // 按 Bean 名称精确指定
private String serverStartTime;
```

`@Qualifier` 可以配合构造器注入和 Setter 注入：

```java
// 构造器注入 + @Qualifier
public UserController(@Qualifier("serverStartTime") String startTime) { ... }

// Setter 注入 + @Qualifier
@Autowired
public void setAppInfo(@Qualifier("appInfo") String appInfo) { ... }
```

### 方案二：`@Resource`（JSR-250 标准，Jakarta 规范）

```java
import jakarta.annotation.Resource;

@Resource(name = "serverStartTime")   // 默认按名称匹配
private String serverStartTime;
```

### `@Qualifier` vs `@Resource` 对比

| | `@Autowired` + `@Qualifier` | `@Resource` |
|---|---|---|
| **来源** | Spring 框架 | JSR-250（Jakarta 标准） |
| **默认匹配** | 按类型 | 按名称 |
| **用法** | `@Qualifier("beanName")` | `@Resource(name = "beanName")` |
| **可标注位置** | 字段、构造器、方法参数 | 字段、setter 方法 |
| **找不到时** | 默认报错 | 默认报错 |
| **解耦程度** | 依赖 Spring 注解 | 标准注解，不依赖 Spring |

> **选择建议**：项目已用 Spring 就用 `@Qualifier`（一致性好）；想和 Spring 解耦就用 `@Resource`（标准注解，换框架不用改）。

---

## 四、`@Component` vs `@Configuration`

### 关系

```java
// @Configuration 源码
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component                           // ← @Configuration 本身就是 @Component
public @interface Configuration { }
```

**`@Configuration` 是 `@Component` 的子类型**，都会被组件扫描发现。

### 核心区别：CGLIB 代理

| | `@Configuration`（Full 模式） | `@Component`（Lite 模式） |
|---|---|---|
| **CGLIB 代理** | ✅ 有 | ❌ 无 |
| **@Bean 方法互调** | 代理拦截 → 返回容器中已存在的单例 | 直接执行 → 每次 new 新对象 |
| **用途** | 定义 Bean（配置类） | 实现业务逻辑 |
| **自身是 Bean** | 是 | 是 |

### 为什么必须有代理？

```java
@Configuration
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        return new DataSource("jdbc:mysql://...");    // 数据库连接池
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());         // ← 调用了上面的方法
    }
}
```

- **`@Configuration` + 代理**：`jdbcTemplate()` 里的 `dataSource()` 被代理拦截 → 返回容器中已存在的 DataSource **单例** → 整个应用共用一个连接池 ✅
- **`@Component` 无代理**：`jdbcTemplate()` 里的 `dataSource()` 就是普通方法调用 → **又 new 了一个新连接池** → 和容器里注入给其他地方的 DataSource 是**两个不同对象** ❌

**结论：这不是性能问题，是语义正确性问题。有 @Bean 方法必须用 @Configuration。**

### 选择规则

```
有 @Bean 方法？ ──→ @Configuration（需要代理保证单例语义）
纯业务逻辑？   ──→ @Service / @Repository / @Controller（都是 @Component 的子类）
第三方对象？   ──→ @Configuration + @Bean
```

---

## 五、项目中的 Bean 注册方式总结

| 类 | 注解 | 注册方式 | 说明 |
|---|---|---|---|
| `UserController` | `@RestController` | 隐式扫描 | `@RestController` 内含 `@Controller` → `@Component` |
| `UserServiceImpl` | `@Service` | 隐式扫描 | `@Service` 内含 `@Component` |
| `UserDaoImpl` | `@Repository` | 隐式扫描 | `@Repository` 内含 `@Component` |
| `AppConfig` | `@Configuration` | 隐式扫描 | 本身是 Bean，内部的 `@Bean` 方法也注册 Bean |
| `serverStartTime` | `@Bean`（在 AppConfig 中） | 显式声明 | `@Configuration` 保证单例语义 |
| `appInfo` | `@Bean` + `@Scope("prototype")` | 显式声明 | 每次获取都创建新实例 |
