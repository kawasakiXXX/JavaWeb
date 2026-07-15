# JWT 令牌机制与登录测试 学习总结

> 基于项目 `JavaWeb_demo` 中 `web_01` 模块的实际代码总结

---

## 一、JWT 是什么

JWT（JSON Web Token）是一个开放标准（RFC 7519），用于在各方之间**安全地传输信息**。

**核心特点**：信息是明文传输的（Base64 编码不算加密），但附带一个**数字签名**，防止信息被篡改。

### 1.1 Token 结构

一个 JWT 由三部分组成，用 `.` 连接：

```
eyJhbGciOiJIUzI1NiJ9.eyJpZCI6IjEiLCJ1c2VybmFtZSI6ImFkbWluIiwiZXhwIjoxNzg0MDIwNzQzfQ.G-djlztx-ryK7TYYq4Bu1Hqwy7X0H22v3A8X7TaXa_Y
     ▲                         ▲                                                      ▲
   Header                   Payload                                               Signature
```

| 部分 | 内容 | 作用 |
|------|------|------|
| **Header** | `{"alg":"HS256","typ":"JWT"}` | 声明签名算法和令牌类型 |
| **Payload** | `{"id":"1","username":"admin","exp":时间戳}` | 存放用户信息（claims） |
| **Signature** | header + payload + 秘钥算出来的哈希值 | 防篡改，验签用 |

### 1.2 签名过程

```
┌────────────────────────────────────────────┐
│  Header    {"alg":"HS256","typ":"JWT"}     │ → Base64 → eyJhbGciOiJIUzI1NiJ9
│  Payload   {"id":"1","username":"admin"}   │ → Base64 → eyJpZCI6IjEi...
└────────────────────────────────────────────┘
                    │
        header + "." + payload
                    │
                    ▼
        ┌──────────────────────┐
        │  HMAC-SHA256 计算     │
        │  输入: 上面的字符串     │
        │  秘钥: Y2RzY2RzY2Rz...│
        └──────────────────────┘
                    │
                    ▼
        Signature: G-djlztx-ryK7TYYq...
                    │
                    ▼
        最终 token = header.payload.signature
```

---

## 二、签名 vs 加密

> ⚠️ JWT 是**签名**，不是加密。这是最常见的误解。

| | 签名（JWT 用的） | 加密 |
|------|-----------------|------|
| 目的 | 防止**篡改** | 防止**偷看** |
| 内容可见性 | 明文，任何人 Base64 解码就能看到 | 密文，不可见 |
| 能不能还原 | 不需要还原，比对签名是否一致 | 用密钥解密还原 |
| 能放什么 | 用户 id、用户名（不敏感信息） | 可以放敏感信息 |

> **结论**：JWT 的 Payload 任何人都能解码看到，所以**千万不要在 JWT 里放密码**。

### 类比

```
签名密钥 = 你的私人印章

签发：在纸条上盖章       → 签名完成
验证：对比纸条上的章     → 章对就信，不对就是伪造

纸条内容公开可读（Base64 不是加密，是编码）
章才是防伪手段
```

---

## 三、Token 的三要素

```
┌─────────────────────────────────────────────┐
│ ① Header（头）   {"alg":"HS256","typ":"JWT"} │  ← 声明算法和类型
│ ② Payload（体）  {"id":"1","username":"admin","exp":...} │  ← 携带用户信息
│ ③ Secret（秘钥） "Y2RzY2RzY2Rz..."          │  ← 自定义密钥，签发和验签都用它
└─────────────────────────────────────────────┘
              │
              │  三者通过 HMAC-SHA256 算法计算
              ▼
         最终的 token 字符串
```

---

## 四、JwtUtils 代码逐行拆解

> 文件位置：`src/main/java/com/cds/javaweb/web_01/utils/JwtUtils.java`

### 4.1 固定配置

```java
// 自定义签名秘钥（必须和签发时一致，否则验签失败）
private static final String SECRET_STRING = "Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==";
private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

// token 过期时间：10 小时
private static final long EXPIRATION_MILLIS = 3600000 * 10L;
```

| 项 | 说明 |
|----|------|
| `SECRET_STRING` | 自定义密钥，**签发和验证必须用同一个** |
| `KEY` | 把字符串转成 `SecretKey` 对象 |
| `EXPIRATION_MILLIS` | 过期时间，单位毫秒 |

### 4.2 生成 Token：`generateToken()`

```java
public static String generateToken(Map<String, Object> claims) {
    String token = Jwts.builder()
            .claims(claims)                                              // ① 自定义声明（用户 id、用户名等）
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MILLIS)) // ② 过期时间
            .signWith(KEY)                                               // ③ 签名
            .compact();                                                  // ④ 压缩成字符串
    return token;
}
```

**参数 `claims` 的内容决定 token 里携带什么信息**：

```java
// 调用示例
Map<String, Object> claims = Map.of("id", "1", "username", "admin");
String token = JwtUtils.generateToken(claims);
```

**每次生成的 token 都不同**——因为过期时间戳每次在变，签名结果随之改变。

### 4.3 解析 Token：`parseToken()`

```java
public static Claims parseToken(String token) {
    try {
        return Jwts.parser()
                .verifyWith(KEY)          // ① 用同一个秘钥验签
                .build()
                .parseSignedClaims(token) // ② 解析
                .getBody();               // ③ 取 Payload
    } catch (Exception e) {
        return null;  // 任何异常（过期、格式错误、签名不匹配）都返回 null
    }
}
```

**验证逻辑**：

```
接收到的 token
    │
    ├─ 格式不对（不是三段）→ 抛异常 → return null
    ├─ 签名不匹配（被篡改或秘钥不一致）→ 抛异常 → return null
    ├─ 已过期 → 抛异常 → return null
    └─ 全部通过 → 返回 Claims 对象
```

### 4.4 验证 Token：`validateToken()`

```java
public static boolean validateToken(String token) {
    Claims claims = parseToken(token);
    return claims != null;  // 解析成功就是有效
}
```

### 4.5 JwtUtils 调用关系图

```
LoginInterceptor.preHandle()
    │
    │  登录成功
    ▼
JwtUtils.generateToken(claims)    ← 生成 token，返回给前端
    │
    │  前端存储 token，下次请求放入请求头
    ▼
TokenFilter.doFilter()
    │
    │  从请求头取 token
    ▼
JwtUtils.parseToken(token)        ← 解析验证 token
    ├─ 有效 → 放行
    └─ null → 401
```

---

## 五、JwtTest 测试类拆解

> 文件位置：`src/test/java/com/cds/javaweb/JwtTest.java`

### 5.1 测试生成：`testGenerateJWT()`

```java
@Test
public void testGenerateJWT() {
    // ① 用同样的秘钥
    SecretKey key = Keys.hmacShaKeyFor("Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==".getBytes());

    // ② 准备声明（模拟 admin 用户）
    Map<String, Object> claims = Map.of("id", "1", "username", "陈德圣");

    // ③ 生成 token
    String token = Jwts.builder()
            .claims(claims)
            .expiration(new Date(System.currentTimeMillis() + 3600000 * 100))
            .signWith(key)
            .compact();

    System.out.println("生成的 Token: " + token);
}
```

**用途**：学习阶段用，单独运行这个测试方法，能直接拿到一个可用的 token，复制去 Apifox 测试。

### 5.2 测试解析：`testParseJWT()`

```java
@Test
public void testParseJWT() {
    SecretKey key = Keys.hmacShaKeyFor("Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==".getBytes());

    // 硬编码一个之前生成好的 token
    String token = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6IjEiLC..." ;

    // 解析
    Claims claim = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getBody();

    System.out.println("解析出的内容: " + claim);
    // 输出: {id=1, username=陈德圣, exp=1784466086}
}
```

**用途**：验证 parseToken 逻辑是否正确，确保用同一个秘钥能解出正确内容。

### 5.3 测试类 vs 业务代码的关系

```
测试类 JwtTest                         业务代码 JwtUtils
─────────────                         ────────────────
testGenerateJWT() → 验证生成逻辑    →  generateToken() 在 LoginInterceptor 中调用
testParseJWT()    → 验证解析逻辑    →  parseToken()    在 TokenFilter 中调用

密钥、API 用法完全一致，确保测试通过 = 业务代码也能通过
```

---

## 六、完整的登录令牌流转

### 6.1 登录 → 生成 Token → 返回

```
POST /login
Content-Type: application/json
{"username": "admin", "password": "123456"}

    │
    ▼
┌──────────────────────────────┐
│ TokenFilter                  │
│ /login 在白名单 → 直接放行     │
└──────────────────────────────┘
    │
    ▼
┌──────────────────────────────┐
│ LoginInterceptor.preHandle() │
│                              │
│ ① 读请求体 → LoginRequest     │
│ ② userServices.login()      │
│    └─ 查数据库验证用户名密码     │
│ ③ 成功 → 生成 JWT             │
│    claims = {"id":"1",      │
│             "username":"admin"} │
│ ④ 返回 JSON:                  │
│    {"code":1,                │
│     "data":{"token":"eyJ...","│
│             "username":"admin"}}│
│ ⑤ return false（结束）        │
└──────────────────────────────┘
    │
    ▼
前端收到 token → 存 localStorage 或变量
```

### 6.2 携带 Token 访问 → 验证 → 业务处理

```
GET /web01
token: eyJhbGciOiJIUzI1NiJ9...

    │
    ▼
┌──────────────────────────────┐
│ TokenFilter                  │
│                              │
│ ① /web01 不在白名单            │
│ ② request.getHeader("token") │
│ ③ JwtUtils.parseToken(token) │
│    ├─ 有效 → 放行              │
│    └─ 无效/过期 → 401 JSON     │
└──────────────────────────────┘
    │ (有效)
    ▼
┌──────────────────────────────┐
│ LoginInterceptor             │
│ 路径 != /login → return true │
└──────────────────────────────┘
    │
    ▼
┌──────────────────────────────┐
│ UserController               │
│ 执行业务，返回数据             │
└──────────────────────────────┘
```

### 6.3 无 Token / 假 Token

```
GET /web01
（不带 token 头 或 token 是瞎编的）

    │
    ▼
TokenFilter → 401
  ├─ 无 header → "未登录，请先进行登录"
  └─ token 无效 → "令牌无效或已过期，请重新登录"

请求到此为止，不会到达 Controller
```

---

## 七、Apifox 测试步骤

### 7.1 登录获取 Token

```
POST http://localhost:8080/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456"
}
```

> ⚠️ 必须用数据库中存在的用户名和密码

**预期响应**：

```json
{
    "code": 1,
    "msg": "操作成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6IjEiLCJ1c2VybmFtZSI6ImFkbWluIiwiZXhwIjoxNzQ0NjA4NTY3fQ.xxx",
        "username": "admin"
    }
}
```

复制 `data.token` 的值，下一步用。

### 7.2 携带 Token 访问受保护接口

```
GET http://localhost:8080/web01
token: eyJhbGciOiJIUzI1NiJ9...
           ↑ 注意：Header 名称是 "token"，值直接粘贴，不需要 "Bearer " 前缀
```

**预期响应**：

```json
{
    "code": 1,
    "msg": "操作成功",
    "data": [
        {"id": 1, "username": "admin", "password": null, ...},
        ...
    ]
}
```

### 7.3 Apifox 完整测试矩阵

| # | 方法 | URL | Header | Body | 预期 |
|---|------|-----|--------|------|------|
| 1 | GET | `/web01` | 无 token | 无 | 401 `"未登录，请先进行登录"` |
| 2 | POST | `/login` | 无 | `{"username":"admin","password":"123456"}` | 200 + token |
| 3 | POST | `/login` | 无 | `{"username":"admin","password":"wrong"}` | 200 `"用户名或密码错误"` |
| 4 | GET | `/web01` | `token: <正确的token>` | 无 | 200 + 用户列表 |
| 5 | GET | `/web01/1` | `token: <正确的token>` | 无 | 200 + id=1 的用户 |
| 6 | POST | `/web01` | `token: <正确的token>` | `{"username":"test","password":"123"}` | 200 + 插入成功 |
| 7 | PUT | `/web01` | `token: <正确的token>` | `{"id":1,"username":"newname"}` | 200 + 更新成功 |
| 8 | DELETE | `/web01/2` | `token: <正确的token>` | 无 | 200 + 删除成功 |
| 9 | GET | `/web01` | `token: abc123`（瞎编的） | 无 | 401 `"令牌无效或已过期"` |

### 7.4 验证清单

```
✅ 无 token → 401
✅ 错误密码 → "用户名或密码错误"
✅ 正确登录 → 返回 token
✅ 正确 token → 正常访问 CRUD
✅ 瞎编 token → 401
✅ 不同类型用户登录 → 返回不同 token
```

---

## 八、Token 生成的时机和位置

### 8.1 时机：必须在登录成功之后

```
① 收到用户名密码
        │
② 查数据库验证
        │
   ┌────┴────┐
   │ 失败     │ → "用户名或密码错误"，不生成 token
   └────┬────┘
        │ 成功
        ▼
③ 生成 token  ← 只有登录成功才生成
        │
④ 返回给前端
```

### 8.2 位置：在登录拦截器中

```java
// LoginInterceptor.preHandle()
User user = userServices.login(username, password);
if (user == null) {
    // 失败 → 不生成 token
    return false;
}

// 成功 → 生成 token
Map<String, Object> claims = Map.of("id", user.getId().toString(), "username", user.getUsername());
String token = JwtUtils.generateToken(claims);  // ← 动态生成
```

### 8.3 什么叫"动态"生成

| | 固定不变 | 每次不同 |
|------|----------|----------|
| 签名密钥 | ✅ | |
| 签名算法 | ✅ | |
| 过期时长 | ✅ | |
| 用户 id | | ✅ 谁登录就写谁的 |
| 用户名 | | ✅ 谁登录就写谁的 |
| 过期时间戳 | | ✅ 每次生成时间不同 |
| 最终 token 值 | | ✅ 每次生成都不同 |

> **对于"动态生成"这个需求，当前代码已经满足，不需要修改。**

---

## 九、本项目 Token 的传输方式

### 9.1 请求头名称：`token`

```java
// TokenFilter.java
String token = request.getHeader("token");  // ← 自定义头名称
```

| 项目实际用 | 标准做法 |
|-----------|---------|
| `token: eyJhbG...` | `Authorization: Bearer eyJhbG...` |
| 更简洁，不用截取前缀 | 符合 RFC 6750 规范 |

> 学习阶段用自定义头更直观；生产环境建议用标准 `Authorization: Bearer`。

### 9.2 完整传输流程

```
第 1 次请求（登录）：
  前端 → POST /login + body JSON
  后端 → 验证通过，生成 token 放在响应体里

第 2 次请求（业务）：
  前端 → GET /web01 + 请求头 token: <token>
  后端 → TokenFilter 验证 token → 放行 → 业务处理
```

---

## 十、常见问题排查

### 10.1 登录返回 401？

**原因**：TokenFilter 在检查 header，检查一下 TokenFilter 白名单是不是完整。

### 10.2 `NullPointerException` at `Map.of()`？

**原因**：`Map.of()` 不接受 null 值。

```java
// ❌ 会报错
Map.of("id", null, "username", "admin");  // NPE!

// ✅ 确保值非 null
Map.of("id", String.valueOf(user.getId()), "username", user.getUsername());
```

### 10.3 MyBatis 查询到数据，但字段是 null？

**原因**：数据库列名和 Java 属性名不一致。

```
数据库: username       Java: name          → 映射不上
数据库: username       Java: username      → ✅ 自动映射
```

> 本项目已统一为 `username`，不需要额外配置。

### 10.4 token 过期时间是多少？

```java
// JwtUtils.java
private static final long EXPIRATION_MILLIS = 3600000 * 10L;
// = 10 小时
```

---

## 十一、关键知识点速记

```
JWT 三部分：         Header.Payload.Signature
JWT 用途：           签名防篡改，不是加密
签名三要素：          算法(HS256) + 用户信息(claims) + 密钥
密钥作用：           签发和验证必须用同一个
动态内容：           id、username 谁登录写谁的
固定内容：           密钥、算法、过期时长
生成时机：           登录验证成功后
生成位置：           LoginInterceptor.preHandle()
验证位置：           TokenFilter.doFilter()
传输方式：           请求头 token: <token值>
前端存储：           localStorage / 变量
过期处理：           前端收到 401 → 跳转到登录页
```

---

## 十二、代码对照表

| 步骤 | 代码位置 | 方法 |
|------|---------|------|
| 生成 token | `JwtUtils.java:32` | `generateToken(Map claims)` |
| 解析 token | `JwtUtils.java:48` | `parseToken(String token)` |
| 调用生成 | `LoginInterceptor.java:67` | `JwtUtils.generateToken(claims)` |
| 调用解析 | `TokenFilter.java:57` | `JwtUtils.parseToken(token)` |
| 测试生成 | `JwtTest.java:16` | `testGenerateJWT()` |
| 测试解析 | `JwtTest.java:33` | `testParseJWT()` |
| 密钥定义 | `JwtUtils.java:20` | `SECRET_STRING` |
| 过期时间 | `JwtUtils.java:24` | `EXPIRATION_MILLIS` |

---

## 十三、项目文件索引

```
src/main/java/com/cds/javaweb/web_01/
├── utils/
│   └── JwtUtils.java               ← JWT 生成/解析工具类
├── filter/
│   └── TokenFilter.java            ← 从请求头取 token，调用 JwtUtils 验证
├── interceptor/
│   └── LoginInterceptor.java       ← 登录成功后调用 JwtUtils 生成 token
├── pojo/
│   ├── LoginRequest.java           ← 登录请求 DTO {username, password}
│   ├── User.java                   ← 用户实体（字段已统一为 username）
│   └── Result.java                 ← 统一响应格式
├── services/
│   ├── UserServices.java           ← 服务接口
│   └── UserServicesImpl.java       ← 服务实现（含登录验证）
└── config/
    └── WebConfig.java              ← Filter/Interceptor 注册

src/test/java/com/cds/javaweb/
└── JwtTest.java                    ← JWT 生成/解析单元测试
```
