# Spring Boot AOP（面向切面编程）学习总结

> 基于项目 `JavaWeb_demo` 中 `web_01` 模块的 `RecordTimeAspect` 实际代码总结

---

## 一、什么是 AOP？一句话理解

**AOP（Aspect Oriented Programming）**：在不修改原有业务代码的前提下，**横向切入**额外的功能（如日志、事务、性能统计）。

```
传统做法（纵向，修改源码）：               AOP 做法（横向，不修改源码）：

┌──────────────────────┐                   ┌──────────────────┐
│   UserServices       │                   │  [ 计时切面 ]     │  ← 横切进去
│  ┌───────────────┐   │                   │  记录每个方法耗时  │
│  │ 开始计时       │   │ ← 手动加          └────────┬─────────┘
│  │ findAll()     │   │                             │ 自动织入
│  │ 结束计时       │   │                   ┌────────▼─────────┐
│  │ 开始计时       │   │                   │   UserServices   │
│  │ deleteById()  │   │                   │  findAll()       │  ← 代码干干净净
│  │ 结束计时       │   │                   │  deleteById()    │
│  └───────────────┘   │                   └──────────────────┘
└──────────────────────┘
```

---

## 二、核心概念速览

```
                            ┌─────────────────────┐
                            │       Aspect         │  ← 切面类 = 切入点 + 通知
                            │   (@Aspect 注解)      │
                            │   RecordTimeAspect   │
                            └──────────┬──────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                                     │
          ┌─────────▼─────────┐               ┌───────────▼───────────┐
          │    Pointcut       │               │       Advice          │
          │    切入点表达式    │               │       通知            │
          │  "在哪些方法上切入"│               │  "切入后做什么事"      │
          │                   │               │  @Before / @After /   │
          │  execution(...)   │               │  @Around 等           │
          └─────────┬─────────┘               └─────────────┬─────────┘
                    │                                       │
                    │           ┌──────────────────────┐    │
                    └──────────►│     JoinPoint        │◄───┘
                                │      连接点           │
                                │  "被拦截到的具体方法"  │
                                │                       │
                                │  提供 getSignature()  │
                                │  getArgs() 等反射信息  │
                                └───────────────────────┘
```

| 概念 | 一句话 | 对应代码中的什么 |
|------|--------|----------------|
| **JoinPoint（连接点）** | 可能被拦截的**所有**方法 | `UserServicesImpl` 中的 `findAll()`、`deleteById()` 等 |
| **Pointcut（切入点）** | 实际被拦截的**匹配规则** | `execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))` |
| **Advice（通知）** | 拦截后**做什么事** | `recordTime()` 方法：记录开始时间 → 执行原方法 → 记录结束时间 |
| **Aspect（切面）** | 切入点 + 通知 = 切面 | `RecordTimeAspect` 整个类 |
| **Weaving（织入）** | 把切面"织"进目标对象的过程 | Spring 启动时自动创建代理对象 |

> **记忆口诀**：JoinPoint 是"可能被截的点"，Pointcut 是"你选中要截的点"，Advice 是"截住之后干嘛"，Aspect 是"截点 + 动作"的合体。

---

## 三、五大通知类型

Spring AOP 提供了 **5 种通知（Advice）**，围绕目标方法的执行生命周期：

```
                     ┌───────────────────────────────┐
                     │        目标方法执行过程           │
                     └───────────────────────────────┘

    @Before          @Around 前                       @After          @AfterReturning
       │                 │                               │                 │
       ▼                 ▼                               ▼                 ▼
    ┌──────┐   ┌─────────────────────┐   ┌──────┐   ┌─────────┐
    │ 前置  │  │    try {            │   │ 后置  │   │ 返回通知│
    │ 通知  │  │      原方法执行()    │   │ 通知  │   │ (成功时)│
    └──────┘   │    }                │   └──────┘   └─────────┘
               │    @Around 后       │        ▲
               └─────────────────────┘        │
                                         ┌─────────┐
                                         │ 异常通知 │
                                         │ (抛异常) │
                                         └─────────┘
                                           @AfterThrowing
```

### 3.1 各通知详解

#### ① `@Before` — 前置通知

在目标方法**执行之前**运行。不能阻止方法执行（除非抛异常）。

```java
@Before("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
public void before(JoinPoint joinPoint) {
    log.info("方法 {} 开始执行，参数：{}", 
             joinPoint.getSignature().getName(), 
             Arrays.toString(joinPoint.getArgs()));
}
```

#### ② `@After` — 后置通知（最终通知）

在目标方法**执行之后**运行，**无论成功还是异常都会执行**（类似 `finally`）。

```java
@After("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
public void after(JoinPoint joinPoint) {
    log.info("方法 {} 执行结束", joinPoint.getSignature().getName());
}
```

#### ③ `@AfterReturning` — 返回通知

在目标方法**正常返回后**执行。可以拿到返回值。**异常时不执行**。

```java
@AfterReturning(
    pointcut = "execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))",
    returning = "result"   // ← 指定接收返回值的参数名
)
public void afterReturning(JoinPoint joinPoint, Object result) {
    log.info("方法 {} 正常返回，返回值：{}", 
             joinPoint.getSignature().getName(), result);
}
```

#### ④ `@AfterThrowing` — 异常通知

在目标方法**抛出异常后**执行。可以拿到异常信息。**正常时不执行**。

```java
@AfterThrowing(
    pointcut = "execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))",
    throwing = "ex"       // ← 指定接收异常的参数名
)
public void afterThrowing(JoinPoint joinPoint, Exception ex) {
    log.error("方法 {} 抛出异常：{}", joinPoint.getSignature().getName(), ex.getMessage());
}
```

#### ⑤ `@Around` — 环绕通知（最强）

**包裹**目标方法，可以在方法前后都加逻辑，**必须手动调用 `pjp.proceed()`** 才会执行原方法，**必须返回原方法的返回值**。

```java
@Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    // === 前置逻辑 ===
    log.info("开始执行...");

    // === 执行原方法（必须调！）===
    Object result = pjp.proceed();

    // === 后置逻辑 ===
    log.info("执行完成...");

    // === 必须返回原方法的返回值！===
    return result;
}
```

### 3.2 五种通知对比速查表

| 通知类型 | 执行时机 | 能拿返回值 | 能拿异常 | 能否阻止执行 | 需要 `proceed()` |
|---------|---------|----------|---------|------------|-----------------|
| `@Before` | 方法前 | ❌ | ❌ | ❌（除非抛异常） | ❌ |
| `@After` | 方法后（finally 式） | ❌ | ❌ | ❌ | ❌ |
| `@AfterReturning` | 正常返回后 | ✅ `returning` | ❌ | ❌ | ❌ |
| `@AfterThrowing` | 抛异常后 | ❌ | ✅ `throwing` | ❌ | ❌ |
| `@Around` | 包裹整个方法 | ✅ | ✅ | ✅（不调 proceed） | ✅ 必须调 |

> **选型建议**：只要不是非用 `@Around` 不可（比如需要同时拿返回值 + 记录耗时），优先用更具体的通知类型，代码意图更清晰。计时场景必须用 `@Around`，因为只有它能同时包裹前后。

---

## 四、通知执行顺序（重要！）

### 4.1 正常返回时

```
  ┌──────────────────────────────────────────────────────┐
  │                    执行顺序（正常情况）                  │
  │                                                        │
  │  ① @Around  进入（前置部分）                             │
  │  ② @Before                                           │
  │  ③ 目标方法执行                                        │
  │  ④ @AfterReturning                                   │
  │  ⑤ @After                                            │
  │  ⑥ @Around  退出（后置部分）                             │
  └──────────────────────────────────────────────────────┘
```

### 4.2 抛出异常时

```
  ┌──────────────────────────────────────────────────────┐
  │                    执行顺序（异常情况）                  │
  │                                                        │
  │  ① @Around  进入（前置部分）                             │
  │  ② @Before                                           │
  │  ③ 目标方法执行 → 抛异常 ✘                              │
  │  ④ @AfterThrowing                                    │
  │  ⑤ @After                                            │
  │  ⑥ @Around 不再执行后置部分 ← 注意！                     │
  └──────────────────────────────────────────────────────┘
```

> ⚠️ **关键记忆**：`@After` 无论成败都执行（像 `finally`）；`@AfterReturning` 和 `@AfterThrowing` 互斥，只会执行其中一个。

### 4.3 多个切面时的排序

用 `@Order(数字)` 控制多个切面的执行顺序——**数字越小越先执行**（类似 Filter 的 order）：

```java
@Order(1)
@Aspect
@Component
public class LogAspect { ... }    // 先执行

@Order(2)
@Aspect
@Component
public class TimeAspect { ... }   // 后执行
```

```
正常返回时：                 异常时：
  LogAspect 前 │→              LogAspect 前 │→
  TimeAspect 前│→ 外           TimeAspect 前│→ 外
  目标方法     │→ 内           异常 ✘       │→ 内
  TimeAspect 后│←             TimeAspect 后│←
  LogAspect 后 │←              LogAspect 后 │←
```

---

## 五、两种切入点表达式

### 5.1 `execution` — 方法匹配表达式

最常用，按**方法签名**匹配。

```
execution(修饰符? 返回值 包.类.方法(参数) throws-异常?)
            ↑     ↑     ↑     ↑     ↑         ↑
         可省略  必填  必填  必填  必填      可省略
```

#### 通配符规则

| 通配符 | 含义 | 示例 |
|--------|------|------|
| `*` | 匹配**一个**任意字符串（单个层级） | `*.services.*` → 匹配 `com.xxx.services.UserServices` |
| `..` | 匹配**零个或多个**任意字符串（多层） | `com..services.*` → 匹配 `com.a.b.services.X` |
| `*`（返回值位） | 匹配任意返回类型 | `* com.xxx.*.*(..)` |
| `(..)` | 匹配任意参数 | 无参/有参都行 |
| `(*)` | 匹配恰好一个参数 | 只匹配单参方法 |

#### 常用表达式速查

```java
// ① 匹配某个类的所有方法（最常用）
execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))

// ② 匹配某个包下所有类的所有方法
execution(* com.cds.javaweb.web_01.services.*.*(..))

// ③ 匹配所有 Service 结尾的类
execution(* com.cds.javaweb.web_01..*Service.*(..))

// ④ 匹配所有 public 方法
execution(public * com.cds.javaweb.web_01..*.*(..))

// ⑤ 匹配指定方法名
execution(* com.cds.javaweb.web_01.services.*.find*(..))
//   → 匹配 findAll、findById 等

// ⑥ 匹配指定参数类型
execution(* com.cds.javaweb.web_01.services.*.*(String, ..))
//   → 第一个参数是 String，后面任意
```

### 5.2 `@annotation` — 注解匹配表达式

按**方法上的注解**匹配，更灵活、更语义化。

#### 两步走：

**第一步**：自定义一个注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordTime {
}
```

**第二步**：在切面中用 `@annotation` 匹配

```java
@Around("@annotation(com.cds.javaweb.web_01.aop.RecordTime)")
public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {
    // ...
}
```

**使用**：在需要计时的方法上加 `@RecordTime`

```java
@RecordTime
public List<User> findAll() {
    return userMapper.findAll();
}
```

### 5.3 两种表达式对比

| 维度 | `execution` | `@annotation` |
|------|------------|---------------|
| **匹配方式** | 按包名+类名+方法名 | 按方法上的注解 |
| **粒度** | 可以批量匹配整个包 | 精确到方法，需手动加注解 |
| **优点** | 一处写，全局生效 | 语义清晰，看注解就知道被 AOP 了 |
| **缺点** | 不够直观，要翻切面才知道哪些被拦截 | 每个需要 AOP 的方法都得加注解 |
| **适用场景** | 通用横切（日志、事务） | 按需切入（权限校验、缓存） |

---

## 六、连接点（JoinPoint）

### 6.1 什么是 JoinPoint

连接点就是**被拦截到的那个方法的信息封装**。在通知方法中声明这个参数，Spring 会自动注入。

```java
@Before("execution(* com..services.*.*(..))")
public void before(JoinPoint joinPoint) {
    // joinPoint 封装了被拦截方法的一切信息
}
```

### 6.2 `JoinPoint` vs `ProceedingJoinPoint`

| | `JoinPoint` | `ProceedingJoinPoint` |
|---|-----------|----------------------|
| **用于哪些通知** | `@Before`、`@After`、`@AfterReturning`、`@AfterThrowing` | **仅** `@Around` |
| **能调用 `proceed()`** | ❌ 不能 | ✅ 必须调，不调原方法不执行 |
| **继承关系** | 父接口 | 子接口，多了 `proceed()` |

### 6.3 常用 API

```java
@Around("execution(* com..services.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {

    // ① 获取目标方法签名
    Signature signature = pjp.getSignature();
    String methodName = signature.getName();          // 方法名："findAll"
    String fullName = signature.toString();           // 全限定名： "UserServices findAll()"
    Class declaringType = signature.getDeclaringType(); // 声明该方法的类

    // ② 获取方法参数
    Object[] args = pjp.getArgs();                    // 实参数组

    // ③ 获取目标对象
    Object target = pjp.getTarget();                  // 被代理的原始对象

    // ④ 获取代理对象
    Object proxy = pjp.getThis();                     // 当前代理对象

    // ⑤ 执行原方法（只有 ProceedingJoinPoint 有）
    Object result = pjp.proceed();                    // 无参：原样传参
    // Object result = pjp.proceed(args);              // 有参：可以修改参数！

    return result;
}
```

---

## 七、本项目实际代码分析

### 7.1 文件结构

```
src/main/java/com/cds/javaweb/web_01/aop/
└── RecordTimeAspect.java    ← 计时切面

src/main/java/com/cds/javaweb/web_01/services/
├── UserServices.java        ← 接口（切入点目标）
└── UserServicesImpl.java    ← 实现类（切入点目标）

pom.xml                       ← 依赖：spring-boot-starter-aop
```

### 7.2 切面代码逐行解读

```java
@Slf4j          // ① Lombok 注解 → 生成 log 对象，可直接 log.info()
@Aspect         // ② 告诉 Spring：这是个切面类
@Component      // ③ 交给 Spring 容器管理（必须！）
public class RecordTimeAspect {

    @Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
    //       ↑ ④ 环绕通知 + 切入点表达式
    public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {
    //     ↑ ⑤ 返回值必须为 Object          ↑ ⑥ 接收连接点信息

        Long begin = System.currentTimeMillis();   // ⑦ 前置：记录开始时间

        Object result = pjp.proceed();            // ⑧ 执行原方法！（必须调）

        Long end = System.currentTimeMillis();     // ⑨ 后置：记录结束时间
        log.info("方法 {} 执行耗时：{} ms",          // ⑩ 输出耗时
                 pjp.getSignature(), end - begin);

        return result;                            // ⑪ 返回原方法的返回值（必须！）
    }
}
```

### 7.3 ⚠️ 常见踩坑：切入点表达式多了 `.*`

```java
// ❌ 错误写法（初学者最容易犯！）
@Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*.*(..))")
//                                                        ↑ 多了一个 .*
// 含义：UserServicesImpl 的内部类 → 内部类的方法
// 结果：UserServicesImpl 没有内部类，匹配不到任何方法，AOP 静默失效！

// ✅ 正确写法
@Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
//                                                        ↑ 只有一个 .*
// 含义：UserServicesImpl → 所有方法
```

### 7.4 运行效果

启动项目后，每次调用 `UserServicesImpl` 的方法都会输出：

```
2026-07-15 14:23:10.123  INFO ... : 方法 UserServicesImpl findAll() 执行耗时：23 ms
2026-07-15 14:23:11.456  INFO ... : 方法 UserServicesImpl deleteById() 执行耗时：5 ms
2026-07-15 14:23:12.789  INFO ... : 方法 UserServicesImpl login() 执行耗时：12 ms
```

---

## 八、依赖与自动配置

### 8.1 依赖

```xml
<!-- pom.xml 中只需这一个依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

这个 starter 自动引入：
- `spring-aop`：Spring 的 AOP 框架
- `aspectjweaver`：AspectJ 的注解解析引擎

### 8.2 Spring Boot 自动配置

**不需要**手动加 `@EnableAspectJAutoProxy`！Spring Boot 检测到 `spring-boot-starter-aop` 后会自动开启：

```
@SpringBootApplication                      ← 项目启动类
    └─ @EnableAutoConfiguration
         └─ AopAutoConfiguration            ← Spring Boot 自动配置
              └─ @EnableAspectJAutoProxy    ← 自动加！你不需要手动写
```

### 8.3 Spring AOP 的代理机制

Spring AOP 底层通过**动态代理**实现，根据目标类是否实现了接口，自动选择代理方式：

```
目标类实现了接口？
  ├─ 是 → JDK 动态代理（默认）
  │       创建实现了接口的代理对象
  │       只能拦截接口中声明的方法
  │       UserServicesImpl 实现 UserServices → 默认走 JDK 代理
  │
  └─ 否 → CGLIB 代理
          创建目标类的子类作为代理
          可以拦截所有非 final 方法
```

**强制使用 CGLIB**（需要在 `application.yml` 中配置）：

```yaml
spring:
  aop:
    proxy-target-class: true   # true = CGLIB, false = JDK 动态代理（默认）
```

> 💡 **对你的项目**：`UserServicesImpl` 实现了 `UserServices` 接口，默认走 JDK 动态代理。`execution` 切入点匹配的是目标类 `UserServicesImpl` 的方法执行，与代理方式无关，所以不影响 AOP 生效。

---

## 九、AOP vs Filter vs Interceptor 三者对比

这是项目中同时出现的三种"拦截机制"，容易混淆：

```
请求进来
  │
  ▼
┌───────────────┐
│    Filter      │  ← Servlet 层面，最早拦截
│  (TokenFilter) │     适用：编码设置、JWT 校验（不需要 Spring Bean）
└───────────────┘
  │
  ▼
┌────────────────┐
│  Interceptor    │  ← Spring MVC 层面
│ (LoginInterceptor)│     适用：需要注入 Service 的业务校验
└────────────────┘
  │
  ▼
┌────────────────┐
│   Controller    │
│       │         │
│       ▼         │
│ ┌─────────────┐ │
│ │   Service   │◄├─ AOP 切面在这里生效！← Spring Bean 层面的增强
│ │  (AOP 切入)  │ │     适用：日志、事务、缓存、权限注解
│ └─────────────┘ │
└────────────────┘
```

| 维度 | Filter | Interceptor | AOP |
|------|--------|------------|-----|
| **归属** | Servlet 规范 | Spring MVC | Spring 框架 |
| **粒度** | URL 级别 | URL → Controller | 方法级别（任意 Spring Bean） |
| **能拿到的信息** | 原始 Request/Response | Handler（知道哪个 Controller） | 方法签名、参数、返回值、异常 |
| **能否阻止执行** | 不调用 `chain.doFilter()` | `return false` | 不调用 `pjp.proceed()` |
| **能否注入 Spring Bean** | ❌ | ✅ | ✅ |
| **适用场景** | 编码、简单鉴权 | 登录校验、权限 | 日志、事务、缓存、性能监控 |

> **记忆口诀**：Filter 管大门（请求进不来），Interceptor 管房间（进哪个 Controller），AOP 管细节（方法执行的前后左右）。

---

## 十、一个完整的 AOP 开发检查清单

写完一个 AOP 切面后，按这个清单逐项检查：

```
☐ ① pom.xml 有 spring-boot-starter-aop 依赖
☐ ② 切面类有 @Aspect 注解
☐ ③ 切面类有 @Component 注解（交给 Spring 管理）
☐ ④ 切入点表达式语法正确（execution 和 @annotation 二选一）
☐ ⑤ @Around 通知中调用了 pjp.proceed()
☐ ⑥ @Around 通知返回了 pjp.proceed() 的结果
☐ ⑦ 异常声明了 throws Throwable（@Around 必须）
☐ ⑧ 启动日志中能看到切面类被加载
```

---

## 十一、项目文件索引

```
src/main/java/com/cds/javaweb/web_01/
├── aop/
│   └── RecordTimeAspect.java    ← AOP 计时切面（本文核心）
├── filter/
│   └── TokenFilter.java         ← Filter 实现（见《Filter与Interceptor学习总结》）
├── interceptor/
│   └── LoginInterceptor.java    ← Interceptor 实现（见《Filter与Interceptor学习总结》）
├── config/
│   └── WebConfig.java           ← Filter / Interceptor 注册配置
├── services/
│   ├── UserServices.java        ← AOP 切入点目标的接口
│   └── UserServicesImpl.java    ← AOP 切入点目标的实现类
├── controller/
│   └── UserController.java      ← 调用被 AOP 增强的 Service
└── utils/
    └── JwtUtils.java            ← JWT 工具类
```

---

> 📚 **相关文档**：
> - [Filter与Interceptor学习总结](./Filter与Interceptor学习总结.md)
> - [JWT与登录测试学习总结](./JWT与登录测试学习总结.md)
