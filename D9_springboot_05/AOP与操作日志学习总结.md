# AOP 切面编程、操作日志与代码改进 学习总结

> 基于项目 `JavaWeb_demo` 中 `web_01` 模块，记录 2026-07-15 学习与改动内容

---

## 一、今日改动概览

```
新增功能：
├── AOP 切面编程（3 个切面类）
├── 自定义注解 @Log
├── 操作日志自动记录到数据库
├── ThreadLocal 存储当前登录用户
└── 操作日志表 operate_log

代码修复：
├── ThreadLocal 清理放入 finally
├── 吞异常增加日志记录
└── 删除死代码 LoginController
```

---

## 二、AOP 切面编程

### 2.1 什么是 AOP

AOP（Aspect Oriented Programming，面向切面编程）是一种编程思想——**把横跨多个方法的公共逻辑抽出来，集中管理**。

```
没有 AOP：                      有了 AOP：
                               
Controller.method1() {          Controller.method1() {
    记录日志                        业务逻辑
    业务逻辑                    }
    记录耗时                    }
                               
Controller.method2() {          切面类（一个地方写）：
    记录日志                       记录日志
    业务逻辑                       记录耗时
    记录耗时                    }
                               
Controller.method3() {          所有方法自动织入切面逻辑
    记录日志                    
    业务逻辑                    
    记录耗时                    
}                              
```

### 2.2 Spring AOP 关键概念

| 概念 | 说明 | 本项目对应 |
|------|------|-----------|
| **切面（Aspect）** | 切面类，封装横切逻辑 | `OperateLogAspect`、`RecordTimeAspect` |
| **通知（Advice）** | 切面在什么时机执行 | `@Around`（环绕）、`@Before`、`@After` |
| **切入点（Pointcut）** | 对哪些方法生效 | `execution(* ...)` 或 `@annotation(...)` |
| **连接点（JoinPoint）** | 程序执行过程中的某个点 | `ProceedingJoinPoint` 对象 |

### 2.3 两种切入点表达式

| 表达式 | 示例 | 作用 |
|--------|------|------|
| `execution()` | `execution(* com.cds..services.*.*(..))` | 匹配方法签名（包、类、方法、参数） |
| `@annotation()` | `@annotation(com.cds.javaweb.web_01.anno.Log)` | 匹配标注了特定注解的方法 |

---

## 三、自定义注解 @Log

> 文件位置：`src/main/java/com/cds/javaweb/web_01/anno/Log.java`

```java
@Target(ElementType.METHOD)      // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留（注解能被反射读取）
public @interface Log {
    String value() default "";   // 操作描述，如"删除用户"
}
```

**两条关键元注解**：

| 元注解 | 含义 | 为什么这样写 |
|--------|------|-------------|
| `@Target(ElementType.METHOD)` | 只能贴在方法上 | 操作日志只记录方法调用 |
| `@Retention(RetentionPolicy.RUNTIME)` | 运行时不丢弃 | AOP 在运行时通过反射读取注解 |

**使用方式**（在 UserController 中）：

```java
@Log("删除用户")
@DeleteMapping("/{id}")
public Result delete(@PathVariable Integer id) { ... }

@Log("新增用户")
@PostMapping
public Result add(@RequestBody User user) { ... }

@Log("更新用户")
@PutMapping
public Result update(@RequestBody User user) { ... }
```

---

## 四、三个 AOP 切面类详解

### 4.1 Aspect1.java — 入门教学示例

> 文件位置：`src/main/java/com/cds/javaweb/web_01/aop/Aspect1.java`

```java
@Slf4j
//@Aspect
@Component
public class Aspect1 {
    @Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
    public void before(JoinPoint joinPoint) {
        // 获取目标对象、类名、方法名、参数
        Object target = joinPoint.getTarget();
        String className = target.getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("目标类：{}", className);
        log.info("目标方法：{}", methodName);
        log.info("目标方法参数：{}", Arrays.toString(args));
    }
}
```

**作用**：演示如何通过 `JoinPoint` 获取被拦截方法的信息——目标对象、类名、方法名、参数。

**注意**：`@Aspect` 测试阶段被注释，启用时取消注释即可；`@Around` 方法应返回 `Object` 并调用 `joinPoint.proceed()`，否则目标方法不会执行、返回值会丢失。

### 4.2 RecordTimeAspect.java — 耗时统计

> 文件位置：`src/main/java/com/cds/javaweb/web_01/aop/RecordTimeAspect.java`

```java
@Slf4j
//@Aspect
@Component
public class RecordTimeAspect {
    @Around("execution(* com.cds.javaweb.web_01.services.UserServicesImpl.*(..))")
    public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {
        long begin = System.currentTimeMillis();  // ① 开始计时

        Object result = pjp.proceed();             // ② 执行目标方法

        long end = System.currentTimeMillis();    // ③ 结束计时
        log.info("方法 {} 执行耗时：{} ms", pjp.getSignature(), end - begin);
        return result;                             // ④ 返回目标方法的结果
    }
}
```

这个写对了：
- 返回 `Object` 类型 ✅
- 调用了 `pjp.proceed()` ✅
- `return result` ✅

### 4.3 OperateLogAspect.java — 操作日志入库（核心）

> 文件位置：`src/main/java/com/cds/javaweb/web_01/aop/OperateLogAspect.java`

```java
@Slf4j
@Aspect           // ✅ 这个已启用
@Component
public class OperateLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;  // 注入日志 Mapper

    @Around("@annotation(com.cds.javaweb.web_01.anno.Log)")  // 拦截所有 @Log 方法
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        // ① 准备日志对象
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(CurrentHolder.getCurrentUserId());  // 从 ThreadLocal 取当前用户
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));

        Object result;
        try {
            // ② 执行目标方法
            result = joinPoint.proceed();
            operateLog.setReturnValue(result != null ? result.toString() : "");
        } catch (Throwable e) {
            // ③ 异常路径：也记录日志
            operateLog.setCostTime(System.currentTimeMillis() - begin);
            operateLog.setReturnValue("异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            try {
                operateLogMapper.insert(operateLog);
            } catch (Exception ex) {
                log.warn("操作日志入库失败: {}", ex.getMessage());  // 日志入库失败不影响业务
            }
            throw e;  // 异常继续往上抛
        }

        // ④ 正常路径：记录耗时，日志入库
        operateLog.setCostTime(System.currentTimeMillis() - begin);
        try {
            operateLogMapper.insert(operateLog);
        } catch (Exception ex) {
            log.warn("操作日志入库失败: {}", ex.getMessage());
        }

        return result;  // ⑤ 返回目标方法的结果
    }
}
```

**设计要点**：

```
执行流程：

@Around 切面开始
    │
    ├─ ① 组装 OperateLog 对象（记录操作人、时间、类名、方法名、参数）
    │
    ├─ ② joinPoint.proceed() → 执行 Controller 方法
    │         │
    │     ┌───┴───┐
    │  成功        异常
    │     │         │
    │     │    ③ 记录异常信息 + 耗时 + 入库
    │     │         │
    │     │    ④ throw e（继续向上抛）
    │     │
    │  ④ 记录返回值 + 耗时 + 入库
    │
    └─ ⑤ return result（返回给前端）
```

**关键点**：
- 正常和异常两条路径都会记录日志
- 日志入库失败不影响业务（try-catch 包裹，只打 warn 不抛异常）
- 使用 `CurrentHolder.getCurrentUserId()` 获取当前登录用户 ID

---

## 五、ThreadLocal 线程局部变量

### 5.1 什么是 ThreadLocal

每个 HTTP 请求由 Tomcat 线程池中的一个线程处理。`ThreadLocal` 让**同一个线程内**任意地方都能存取数据，线程之间互不干扰。

```
线程A 处理 用户1 的请求         线程B 处理 用户2 的请求
┌──────────────────┐          ┌──────────────────┐
│ ThreadLocal      │          │ ThreadLocal      │
│ userId = 1       │          │ userId = 2       │
│ username = admin │          │ username = aaa   │
└──────────────────┘          └──────────────────┘
    互不干扰，各自独立
```

### 5.2 CurrentHolder 代码

> 文件位置：`src/main/java/com/cds/javaweb/web_01/utils/CurrentHolder.java`

```java
public class CurrentHolder {
    // ThreadLocal 容器，存 Integer 类型的 userId
    private static final ThreadLocal<Integer> CURRENT_LOCAL = new ThreadLocal<>();

    // 存入当前线程的 userId
    public static void setCurrentUserId(Integer userId) {
        CURRENT_LOCAL.set(userId);
    }

    // 取出当前线程的 userId
    public static Integer getCurrentUserId() {
        return CURRENT_LOCAL.get();
    }

    // 清理（线程归还线程池前必须清理！）
    public static void remove() {
        CURRENT_LOCAL.remove();
    }
}
```

### 5.3 在 Filter 中的使用与清理

> 文件位置：`src/main/java/com/cds/javaweb/web_01/filter/TokenFilter.java`

```java
// 验证 token 通过后
String userId = claims.get("id", String.class);
CurrentHolder.setCurrentUserId(Integer.valueOf(userId));  // ① 存入 ThreadLocal

// 放行，finally 确保清理
try {
    filterChain.doFilter(request, response);  // ② 业务处理中随时可取
} finally {
    CurrentHolder.remove();  // ③ 线程归还前必须清理！
}
```

**为什么 `finally` 中清理很重要**：

```
没有 finally（改前）：
  filterChain.doFilter() → 业务抛异常 → remove() 跳过 → 数据残留

有 finally（改后）：
  filterChain.doFilter() → 无论是否异常 → finally 必定执行 → 清理干净
```

> Tomcat 线程池复用线程，如果不清理，下一个请求可能读到上一个用户的 ID。

### 5.4 在 AOP 中的取用

```java
// OperateLogAspect.java
operateLog.setOperateEmpId(CurrentHolder.getCurrentUserId());
// ↑ 从 ThreadLocal 取出当前登录用户 ID
```

**完整数据流**：

```
TokenFilter:  CurrentHolder.setCurrentUserId(1)
                    │
                    │  存入 ThreadLocal ──────────────────┐
                    ▼                                     │
              Controller                                 │
                    │                                     │
                    ▼                                     │
         @Log 注解触发 OperateLogAspect                    │
                    │                                     │
                    ▼                                     │
         CurrentHolder.getCurrentUserId() ← ← ← ← ← ← ──┘
                    │           从 ThreadLocal 取出 1
                    ▼
         operateLog.setOperateEmpId(1) → 入库
```

---

## 六、操作日志完整链路

### 6.1 数据库表结构

```sql
-- operate_log 表
CREATE TABLE operate_log (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    operate_emp_id  INT,            -- 操作人 ID（来自 ThreadLocal）
    operate_time    DATETIME,       -- 操作时间
    class_name      VARCHAR(255),   -- 目标类名
    method_name     VARCHAR(255),   -- 目标方法名
    method_params   VARCHAR(1000),  -- 方法参数
    return_value    VARCHAR(2000),  -- 返回值
    cost_time       BIGINT          -- 执行耗时（毫秒）
);
```

### 6.2 实体类 OperateLog

```java
@Data
public class OperateLog {
    private Integer id;
    private Integer operateEmpId;    // 操作人ID
    private LocalDateTime operateTime; // 操作时间
    private String className;        // 目标类名
    private String methodName;       // 目标方法名
    private String methodParams;     // 方法参数
    private String returnValue;      // 返回值
    private long costTime;           // 执行耗时(ms)
}
```

### 6.3 Mapper

```java
@Mapper
public interface OperateLogMapper {
    @Insert("insert into operate_log (operate_emp_id, operate_time, class_name, " +
            "method_name, method_params, return_value, cost_time) " +
            "values (#{operateEmpId}, #{operateTime}, #{className}, #{methodName}, " +
            "#{methodParams}, #{returnValue}, #{costTime})")
    public void insert(OperateLog operateLog);
}
```

### 6.4 从请求到日志入库的完整链路

```
POST /web01
token: eyJhbG...
{"username":"test","password":"123"}

    │
    ▼
TokenFilter
  ├─ 验证 token 通过
  ├─ CurrentHolder.setCurrentUserId(1)  ← 存入 ThreadLocal
  └─ 放行
    │
    ▼
LoginInterceptor → 不是 /login → return true
    │
    ▼
UserController.add()
  ↑ 被 @Log("新增用户") 标注
    │
    ▼
OperateLogAspect.around()（AOP 环绕）
  ├─ CurrentHolder.getCurrentUserId() → 1  ← 从 ThreadLocal 取出
  ├─ 组装 OperateLog {
  │    operateEmpId = 1,
  │    operateTime = 2026-07-15T16:30:00,
  │    className = "com.cds.javaweb.web_01.controller.UserController",
  │    methodName = "add",
  │    methodParams = "[User(id=null, username=test, password=123)]",
  │    costTime = 25,
  │    returnValue = "Result(code=1, msg=操作成功)"
  │  }
  └─ operateLogMapper.insert(operateLog) → 写入数据库
    │
    ▼
TokenFilter.finally
  └─ CurrentHolder.remove()  ← 清理 ThreadLocal
```

---

## 七、代码审查与修复记录

### 7.1 已修复

| # | 问题 | 位置 | 修复方式 |
|---|------|------|---------|
| 1 | ThreadLocal 未放 finally | `TokenFilter.java:80-83` | 用 `try { doFilter } finally { remove }` 包裹 |
| 2 | 吞异常无日志 | `OperateLogAspect.java:46,51` | `catch (Exception ignored) {}` → `catch (Exception ex) { log.warn(...) }` |
| 3 | LoginController 死代码 | `LoginController.java` | 删除整个文件 |

---

## 八、AOP 通知类型速查

Spring AOP 提供 5 种通知类型：

| 注解 | 时机 | 说明 |
|------|------|------|
| `@Before` | 目标方法执行前 | 前置通知 |
| `@After` | 目标方法执行后（无论是否异常） | 后置通知，类似 finally |
| `@AfterReturning` | 目标方法正常返回后 | 可以获取返回值 |
| `@AfterThrowing` | 目标方法抛异常后 | 可以获取异常信息 |
| `@Around` | 前后都包住 | 最强大，需要手动调 `proceed()` |

```
@Around 的执行包裹关系：

    @Around 开始
        │
        ├─ @Before
        │      │
        │      ▼
        │   目标方法执行
        │      │
        │  ┌───┴───┐
        │ 成功      异常
        │  │         │
        │  ▼         ▼
        │ @AfterReturning   @AfterThrowing
        │  │         │
        │  └────┬────┘
        │       ▼
        │     @After
        │       │
        ▼       ▼
    @Around 结束
```

---

## 九、最终项目文件结构

```
src/main/java/com/cds/javaweb/web_01/
├── anno/
│   └── Log.java                    ← 自定义 @Log 注解
├── aop/
│   ├── Aspect1.java                ← 教学示例
│   ├── OperateLogAspect.java       ← 操作日志切面（已启用）
│   └── RecordTimeAspect.java       ← 耗时统计切面
├── config/
│   └── WebConfig.java              ← Filter / Interceptor 注册
├── controller/
│   ├── UploadController.java       ← 文件上传
│   └── UserController.java         ← CRUD + @Log 注解
├── filter/
│   └── TokenFilter.java            ← JWT 验证 + ThreadLocal 存取
├── interceptor/
│   └── LoginInterceptor.java       ← 登录处理 + 生成 JWT
├── mapper/
│   ├── OperateLogMapper.java       ← 操作日志入库
│   └── UserMapper.java             ← 用户 CRUD
├── pojo/
│   ├── LoginRequest.java           ← 登录请求 DTO
│   ├── OperateLog.java             ← 操作日志实体
│   ├── Result.java                 ← 统一响应格式
│   └── User.java                   ← 用户实体
├── services/
│   ├── UserServices.java           ← 服务接口
│   └── UserServicesImpl.java       ← 服务实现
├── utils/
│   ├── CurrentHolder.java          ← ThreadLocal 工具类
│   └── JwtUtils.java               ← JWT 工具类
└── GlobalExceptionHandler/
    └── GlobalExceptionHandler.java ← 全局异常处理
```

---

## 十、一次完整请求的全链路

```
客户端 POST /web01
token: eyJhbG...
{"username":"test","password":"123"}

    │
    ▼
① TokenFilter（过滤器）
   验证 token → CurrentHolder.setCurrentUserId(1) → 放行
    │
    ▼
② DispatcherServlet（Spring 调度器）
    │
    ▼
③ LoginInterceptor（拦截器）
   不是 /login → return true → 放行
    │
    ▼
④ OperateLogAspect（AOP 切面 — @Around 开始）
   组装 OperateLog → joinPoint.proceed() →
    │
    ▼
⑤ UserController.add()（目标方法）
   执行业务逻辑
    │
    ▼
⑥ OperateLogAspect（AOP 切面 — @Around 结束）
   记录耗时 → operateLogMapper.insert() → return result
    │
    ▼
⑦ TokenFilter.finally
   CurrentHolder.remove()（清理 ThreadLocal）
    │
    ▼
⑧ 响应返回给客户端
```

**五层防护与增强**：

```
Filter        → 安全守卫（验证 token）
Interceptor   → 登录处理（生成 token）
AOP           → 横向增强（记录日志、统计耗时）
Controller    → 业务处理（CRUD）
ExceptionHandler → 兜底（统一错误响应）
```

---

## 十一、关键知识点速记

```
AOP 核心思想：        横切关注点集中管理，不侵入业务代码
@Around 规则：        返回 Object + 调用 proceed() + return result
自定义注解两步：       @Target（贴在哪儿）+ @Retention（保留到何时）
ThreadLocal 场景：    同一请求跨层传递数据（Filter → AOP → Service）
ThreadLocal 铁律：    set 之后必须 remove，放在 finally 里
操作日志设计：        正常/异常两条路径都记录 + 日志入库失败不影响业务
@Aspect 生效条件：    注解不能被注释 + 类必须是 Spring Bean（@Component）
切面切入点两种：       execution() 匹配方法签名 / @annotation() 匹配注解
```
