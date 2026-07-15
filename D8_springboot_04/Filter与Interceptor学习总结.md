# Spring Boot 过滤器(Filter)、拦截器(Interceptor) 与全局异常处理 学习总结

> 基于项目 `JavaWeb_demo` 中 `web_01` 模块的实际代码总结

---

## 一、一句话区别

| 维度 | Filter（过滤器） | Interceptor（拦截器） |
|------|-----------------|-----------------------|
| **归属** | Servlet 规范（Java 标准） | Spring MVC 框架 |
| **执行时机** | 在 DispatcherServlet **之前** | 在 DispatcherServlet **之后**、Controller **之前** |
| **能拿到什么** | 只有原始的 `HttpServletRequest` / `HttpServletResponse` | 可以拿到 Handler（知道哪个 Controller 方法会被调用） |
| **能做什么** | 编码设置、权限校验、日志记录 | AOP 切面、权限校验、参数预处理 |
| **能否访问 Spring Bean** | ❌ 不能直接注入 | ✅ 可以直接 `@Autowired` |

---

## 二、执行链路图

```
客户端请求
    │
    ▼
┌──────────────────┐
│     Filter        │  ← Servlet 层面，最早接触请求
│   (TokenFilter)   │     能做的事：字符编码、安全检查、打日志
└──────────────────┘
    │
    ▼
┌──────────────────┐
│ DispatcherServlet │  ← Spring 核心调度器
└──────────────────┘
    │
    ▼
┌──────────────────┐
│   Interceptor     │  ← Spring MVC 层面
│ (LoginInterceptor)│     能做的事：拿到 Controller 信息、注入 Spring Bean
└──────────────────┘
    │
    ▼
┌──────────────────┐
│    Controller     │  ← 真正的业务处理
└──────────────────┘
    │
    │  任何一层抛出异常
    │  (Filter / Interceptor / Controller / Service / Mapper)
    ▼
┌──────────────────────┐
│ GlobalExceptionHandler│  ← 最后一道防线，兜底捕获所有未处理异常
│  @RestControllerAdvice│     统一返回 {"code":0, "msg":"..."} 给前端
└──────────────────────┘
```

> **核心记忆**：Filter 是"进大门"的保安（Servlet 层面），Interceptor 是"进办公室"的秘书（Spring 层面）。保安先看到你，秘书后看到你。

---

## 三、Filter（过滤器）构建逻辑

### 3.1 核心骨架

```java
public class XxxFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // 1. 前置处理：拦截前的逻辑
        // 2. 放行 or 拦截
        // 3. 后置处理（如果有）
    }
}
```

### 3.2 关键概念：FilterChain（过滤器链）

`doFilter` 方法接收一个 `chain` 参数：

- **调用 `chain.doFilter()`** → **放行**，请求继续往下走
- **不调用 `chain.doFilter()`** → **拦截**，自己写 response 返回给客户端

```
请求 → Filter.doFilter()
         │
         ├─ chain.doFilter()  ← 放行，请求继续往下走
         │
         └─ 不调用 chain.doFilter()  ← 拦截，自己写 response 返回
```

### 3.3 实际代码：TokenFilter

> 文件位置：`src/main/java/com/cds/javaweb/web_01/filter/TokenFilter.java`

```java
@Slf4j
public class TokenFilter implements Filter {                          // ① 实现 Filter 接口

    private final ObjectMapper objectMapper = new ObjectMapper();    // 用于写 JSON 响应

    private static final String[] EXCLUDE_PATHS = {"/login", "/error"};  // 白名单

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        // ② 类型转换：Servlet API 只有原始的 ServletRequest，需要强转
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI(); // 获取请求路径

        // ③ 白名单放行
        if (isExcluded(uri)) {
            filterChain.doFilter(request, response);  // 放行
            return;
        }

        // ④ 获取并验证 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录，请先进行登录");  // 拦截：直接写响应，不调 chain
            return;
        }

        String token = authHeader.substring(7);            // 去掉 "Bearer " 前缀
        Claims claims = JwtUtils.parseToken(token);        // 解析验证
        if (claims == null) {
            writeError(response, 401, "令牌无效或已过期");   // 拦截
            return;
        }

        // ⑤ 通过验证，将用户信息存入 request 属性，供后续使用
        request.setAttribute("userId", claims.get("id"));
        request.setAttribute("username", claims.get("username"));

        filterChain.doFilter(request, response);  // 放行
    }

    // 辅助方法：向客户端返回 JSON 格式的错误信息
    private void writeError(HttpServletResponse response, int status, String msg)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(msg)));
    }
}
```

### 3.4 Filter 代码结构模板

```
┌─────────────────────────────────────────────────┐
│             Filter 代码结构模板                     │
│                                                   │
│  doFilter(ServletRequest, ServletResponse) {      │
│                                                   │
│      // ① 强转为 HttpServletXxx（必须做）           │
│                                                   │
│      // ② 写你的判断逻辑                            │
│      if (条件不满足) {                              │
│          直接写 response，然后 return;              │
│          ← 关键：不调 chain.doFilter() 就是拦截      │
│      }                                            │
│                                                   │
│      // ③ 放行                                    │
│      chain.doFilter(req, resp);                   │
│  }                                                │
└─────────────────────────────────────────────────┘
```

---

## 四、Interceptor（拦截器）构建逻辑

### 4.1 核心骨架

Interceptor 需要实现 `HandlerInterceptor` 接口，提供 **3 个可选时机**：

```java
public class XxxInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // Controller 执行之前
        // return true  → 放行，继续往下
        // return false → 拦截，请求到此为止
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        // Controller 执行之后，视图渲染之前
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // 整个请求完成之后（视图也渲染完了），即使异常也会执行
    }
}
```

### 4.2 三个时间点图示

```
请求
  │
  ▼
┌──────────────────┐
│   preHandle()    │  ← return false = 拦截
│   前置处理        │     return true  = 放行
└──────────────────┘
  │ (return true)
  ▼
┌──────────────────┐
│   Controller     │  ← 执行业务
└──────────────────┘
  │
  ▼
┌──────────────────┐
│  postHandle()    │  ← 可以修改返回结果
│   后置处理        │
└──────────────────┘
  │
  ▼
┌──────────────────┐
│    视图渲染       │
└──────────────────┘
  │
  ▼
┌──────────────────┐
│ afterCompletion()│  ← 收尾工作（记录日志等）
│    最终收尾       │     即使异常也会执行
└──────────────────┘
```

### 4.3 实际代码：LoginInterceptor

> 文件位置：`src/main/java/com/cds/javaweb/web_01/interceptor/LoginInterceptor.java`

```java
@Slf4j
@Component  // ← 是 Spring Bean，可以注入其他 Bean
public class LoginInterceptor implements HandlerInterceptor {  // ① 实现 HandlerInterceptor 接口

    @Autowired
    private UserServices userServices;  // ← Filter 做不到的：直接注入 Spring Bean

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {  // ② 参数直接就是 Http 类型，不需要强转

        // 只处理 /login 的 POST 请求
        if ("/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {

            // 读取请求体中的 JSON
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            // 反序列化
            LoginRequest loginRequest = objectMapper.readValue(body.toString(), LoginRequest.class);

            // ③ 调用 Service 层验证（这是 Interceptor 的核心优势）
            User user = userServices.login(loginRequest.getUsername(), loginRequest.getPassword());
            if (user == null) {
                response.getWriter().write(
                    objectMapper.writeValueAsString(Result.error("用户名或密码错误")));
                return false;  // ← 拦截！请求到此结束
            }

            // ④ 生成 token，写回响应
            String token = JwtUtils.generateToken(
                Map.of("id", user.getId().toString(), "username", user.getName()));
            response.getWriter().write(
                objectMapper.writeValueAsString(
                    Result.success(Map.of("token", token, "username", user.getName()))));
            return false;  // ← 拦截！响应已在上面写好了
        }

        return true;  // 非 /login 请求，放行
    }
}
```

### 4.4 Interceptor 代码结构模板

```
┌─────────────────────────────────────────────────┐
│           Interceptor 代码结构模板                  │
│                                                   │
│  preHandle(HttpServletRequest,                    │
│            HttpServletResponse,                   │
│            Object handler) {                      │
│                                                   │
│      // ① 不需要强转！参数直接就是 Http 类型         │
│      //    handler 告诉你哪个 Controller 方法      │
│                                                   │
│      // ② 可以 @Autowired 注入任何 Spring Bean     │
│                                                   │
│      // ③ 判断逻辑                                │
│      if (条件不满足) {                              │
│          自己写 response；                         │
│          return false;  ← 拦截                     │
│      }                                            │
│                                                   │
│      return true;  ← 放行                          │
│  }                                                │
└─────────────────────────────────────────────────┘
```

---

## 五、配置注册（如何挂到系统上）

> 文件位置：`src/main/java/com/cds/javaweb/web_01/config/WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // ========== 注册 Interceptor ==========
    // Interceptor 是 Spring MVC 的一部分，通过重写 addInterceptors 注册
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/login")   // 只对这个路径生效
                .order(1);
    }

    // ========== 注册 Filter ==========
    // Filter 是 Servlet 原生的，需要通过 FilterRegistrationBean 注册
    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilterRegistration() {
        FilterRegistrationBean<TokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TokenFilter());   // 手动 new（不是注入的）
        registration.addUrlPatterns("/*");            // 对所有路径生效
        registration.setOrder(1);
        registration.setName("tokenFilter");
        return registration;
    }
}
```

| 对比项 | Interceptor 注册 | Filter 注册 |
|--------|-----------------|-------------|
| 方式 | 重写 `addInterceptors()` | 声明 `@Bean` 返回 `FilterRegistrationBean` |
| 获取组件 | `@Autowired` 注入（是 Spring Bean） | `new` 手动创建（不是 Spring Bean） |
| 路径配置 | `.addPathPatterns("/login")` | `.addUrlPatterns("/*")` |
| 所属体系 | Spring MVC 框架 | Servlet 容器 |

---

## 六、两种写法对比速查表

```
┌──────────────────┬────────────────────────┬──────────────────────────┐
│                  │        Filter          │       Interceptor        │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 实现接口          │ jakarta.servlet        │ org.springframework      │
│                  │   .Filter              │   .HandlerInterceptor    │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 核心方法          │ doFilter()             │ preHandle()              │
│                  │                        │ postHandle()             │
│                  │                        │ afterCompletion()        │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 放行方式          │ chain.doFilter()       │ return true              │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 拦截方式          │ 不调 chain.doFilter()  │ return false             │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 参数类型          │ ServletRequest         │ HttpServletRequest       │
│                  │ (需要手动强转)           │ (直接用，不需要转)         │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 能否注入 Spring   │ ❌ 不能直接注入         │ ✅ @Autowired 即可        │
│ Bean              │                        │                          │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 知道目标          │ ❌ 不知道               │ ✅ Object handler        │
│ Controller 是哪个 │                        │   告诉你谁会处理请求       │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 注册方式          │ FilterRegistrationBean │ addInterceptors()       │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 运行层面          │ Servlet 容器           │ Spring MVC 框架           │
├──────────────────┼────────────────────────┼──────────────────────────┤
│ 执行顺序          │ 先                     │ 后                        │
└──────────────────┴────────────────────────┴──────────────────────────┘
```

---

## 七、应用场景速判

```
需要拦截某个请求 →
  ├─ 不需要调用数据库/Service → 用 Filter
  └─ 需要调用数据库/Service → 用 Interceptor

需要拦截所有请求 →
  ├─ 只需要简单校验（如 token、编码）→ 用 Filter
  └─ 需要复杂的业务判断 → 用 Interceptor

你的代码被其他框架复用 →
  ├─ 是 → 用 Filter（不依赖 Spring）
  └─ 否 → 看上面两条
```

---

## 八、全局异常处理（GlobalExceptionHandler）

### 8.1 它是什么

项目中任何一层（Controller、Service、Mapper、Filter、Interceptor）都可能抛出异常。如果不处理，Spring Boot 会把丑陋的堆栈信息直接返回给前端，既不友好也不安全。

**全局异常处理器**就是最后一道防线——用一个类统一捕获所有异常，返回统一格式的 JSON 错误信息给前端。

### 8.2 两个关键注解

| 注解 | 作用 |
|------|------|
| `@RestControllerAdvice` | 标记这个类为全局异常处理器，覆盖所有 Controller（`@ControllerAdvice` + `@ResponseBody` 的合体） |
| `@ExceptionHandler` | 标记某个方法用来处理指定类型的异常 |

### 8.3 实际代码

> 文件位置：`src/main/java/com/cds/javaweb/web_01/GlobalExceptionHandler/GlobalExceptionHandler.java`

```java
@Slf4j
@RestControllerAdvice  // ① 标记为全局异常处理器（覆盖所有 Controller）
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)  // ② 捕获所有类型的异常
    public Result handleException(Exception e) {
        log.error("服务器发生异常:", e);             // ③ 服务端记录完整堆栈，方便排查
        return Result.error("出错了，服务器发生异常"); // ④ 给前端只返回模糊提示，不暴露细节
    }
}
```

### 8.4 工作原理

```
Controller / Service / Mapper 某处抛出了异常（如 NullPointerException）
    │
    │  异常向上冒泡
    ▼
DispatcherServlet 捕获到异常
    │
    │  查找 @RestControllerAdvice 中有没有匹配的 @ExceptionHandler
    ▼
┌───────────────────────────────────────────────┐
│ GlobalExceptionHandler                        │
│                                               │
│ @ExceptionHandler(Exception.class)            │
│                                               │
│ Exception.class 能匹配所有异常                     │
│ → 执行 handleException(e)                      │
│ → 返回 Result.error("出错了，服务器发生异常")       │
│ → Spring MVC 自动序列化为:                       │
│    {"code":0, "msg":"出错了，服务器发生异常"}       │
└───────────────────────────────────────────────┘
    │
    ▼
  前端收到统一格式的错误 JSON
```

### 8.5 为什么要用

| 没有全局异常处理 | 有全局异常处理 |
|-----------------|---------------|
| 前端收到 500 错误 + HTML 堆栈页面 | 前端收到 `{"code":0,"msg":"出错了，服务器发生异常"}` |
| 暴露代码细节，不安全 | 模糊提示，安全 |
| 每个接口要自己 try-catch | 一处写，全局生效 |
| 前端要处理各种格式的错误 | 前端只认一种 JSON 格式 |

### 8.6 进阶：精准捕获不同异常

当前项目只捕获了 `Exception.class`（最宽泛）。实际项目可以这样细化：

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 捕获空指针异常 */
    @ExceptionHandler(NullPointerException.class)
    public Result handleNullPointer(NullPointerException e) {
        log.error("空指针异常:", e);
        return Result.error("系统数据异常，请联系管理员");
    }

    /** 捕获业务异常（自定义的） */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());  // 业务异常可以把消息透传给前端
    }

    /** 兜底：捕获其他所有异常 */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未知异常:", e);
        return Result.error("服务器繁忙，请稍后再试");
    }
}
```

> **匹配规则**：Spring 会找最精确匹配的 `@ExceptionHandler`。抛 `NullPointerException` 时，`handleNullPointer` 比 `handleException` 更精确，所以优先用它。没有精确匹配的，才落到 `handleException`。

### 8.7 与 Filter / Interceptor 的协同

```
异常可能发生在任何环节：

  环节              异常示例                     谁来处理
  ─────────────────────────────────────────────────────
  Filter            JSON 解析失败              GlobalExceptionHandler ✅
  Interceptor       数据库连接超时              GlobalExceptionHandler ✅
  Controller        参数校验失败                GlobalExceptionHandler ✅
  Service           空指针                     GlobalExceptionHandler ✅
  Mapper            SQL 语法错误               GlobalExceptionHandler ✅
```

> ⚠️ **注意**：Filter 中的异常需要特殊处理。因为 Filter 运行在 Spring MVC 之前，异常不一定能被 `@RestControllerAdvice` 捕获。你的项目中 `TokenFilter` 直接自己 try-catch 并写 response，不走异常处理器，这是一种安全做法。

---

## 九、本项目实际运行逻辑

### 登录流程：`POST /login`

```
前端 POST /login {"username":"admin","password":"123456"}
    │
    ▼
┌─────────────────────────────────────┐
│ TokenFilter（过滤器）                 │
│ /login 在白名单 → 直接放行             │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ DispatcherServlet                   │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ LoginInterceptor（拦截器）            │
│ 1. 读取请求体 JSON → LoginRequest     │
│ 2. userServices.login() 验证用户名密码 │
│ 3. 成功 → JwtUtils.generateToken()   │
│ 4. 返回 JSON 给前端                   │
│    {"code":1,"data":{"token":"eyJ...","username":"admin"}} │
│ 5. return false（请求到此结束）        │
└─────────────────────────────────────┘
    │
    ▼
  前端收到 token → 存入 localStorage
```

### 受保护接口访问：`GET /web01`

```
前端 GET /web01
Header: Authorization: Bearer eyJ...
    │
    ▼
┌─────────────────────────────────────┐
│ TokenFilter（过滤器）                 │
│ 1. /web01 不在白名单                  │
│ 2. 提取 Authorization 头             │
│ 3. JwtUtils.parseToken(token)       │
│    ├─ 有效 → 放行                     │
│    └─ 无效/过期 → 401 JSON            │
└─────────────────────────────────────┘
    │ (有效)
    ▼
┌─────────────────────────────────────┐
│ LoginInterceptor（拦截器）            │
│ 路径不是 /login → return true 放行     │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ UserController                      │
│ 执行业务逻辑 → 返回数据                │
└─────────────────────────────────────┘
```

### 无 Token 访问受保护接口

```
前端 GET /web01（无 Authorization 头）
    │
    ▼
TokenFilter → 401 {"code":0,"msg":"未登录，请先进行登录"}
请求被拦截，不会到达 Controller
```

---

## 十、一个记忆口诀

> **Filter 是"守大门"** —— 不看你是谁，没带令牌就不让进。工具类的逻辑放这里。
>
> **Interceptor 是"找老板"** —— 知道你要找哪个 Controller，还能调 Service 查数据库。需要 Spring Bean 的业务逻辑放这里。
>
> **GlobalExceptionHandler 是"救火队"** —— 哪一层爆了异常，它都能兜底，统一给前端返回友好提示，不让堆栈信息泄露出去。
>
> **Filter 先到，Interceptor 后到，Controller 最后。异常随时可能发生，最终由异常处理器兜底。**

---

## 十一、项目文件索引

```
src/main/java/com/cds/javaweb/web_01/
├── config/
│   └── WebConfig.java              ← Filter / Interceptor / 异常处理 的注册配置
├── filter/
│   └── TokenFilter.java            ← Filter 实现：验证 JWT token
├── interceptor/
│   └── LoginInterceptor.java       ← Interceptor 实现：处理登录请求
├── GlobalExceptionHandler/
│   └── GlobalExceptionHandler.java ← 全局异常处理：统一兜底所有异常
├── utils/
│   └── JwtUtils.java               ← JWT 令牌生成/解析工具类
├── controller/
│   ├── LoginController.java        ← 登录接口（备用，实际被 Interceptor 拦截）
│   └── UserController.java         ← 受保护的 CRUD 接口
├── services/
│   ├── UserServices.java           ← 服务接口
│   └── UserServicesImpl.java       ← 服务实现（含登录验证逻辑）
├── pojo/
│   ├── LoginRequest.java           ← 登录请求 DTO
│   ├── Result.java                 ← 统一响应格式（异常处理也返回它）
│   └── User.java                   ← 用户实体
```
