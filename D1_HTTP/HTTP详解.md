# HTTP 详解

## 目录

- [1. HTTP 概述](#1-http-概述)
- [2. HTTP 请求 (Request)](#2-http-请求-request)
  - [2.1 请求行 (Request Line)](#21-请求行-request-line)
  - [2.2 请求头 (Request Headers)](#22-请求头-request-headers)
  - [2.3 请求体 (Request Body)](#23-请求体-request-body)
- [3. HTTP 响应 (Response)](#3-http-响应-response)
  - [3.1 响应行/状态行 (Status Line)](#31-响应行状态行-status-line)
  - [3.2 响应头 (Response Headers)](#32-响应头-response-headers)
  - [3.3 响应体 (Response Body)](#33-响应体-response-body)
- [4. HTTP 状态码详解](#4-http-状态码详解)
- [5. HTTP 请求方法](#5-http-请求方法)
- [6. 常见请求头与响应头](#6-常见请求头与响应头)
  - [6.1 常用请求头](#61-常用请求头)
  - [6.2 常用响应头](#62-常用响应头)
- [7. HTTP 协议版本演进](#7-http-协议版本演进)
- [8. Java Web 中的应用示例](#8-java-web-中的应用示例)

---

## 1. HTTP 概述

**HTTP（HyperText Transfer Protocol，超文本传输协议）** 是互联网上应用最为广泛的一种网络协议，用于客户端（浏览器）与服务器之间的数据传输。它是一种**无状态（stateless）**协议，每次请求都是独立的，服务器不会默认保留上一次请求的上下文信息。

### HTTP 的核心特点

| 特点 | 说明 |
|------|------|
| **无状态** | 每个请求独立，服务器不记录客户端状态（通过 Cookie/Session 弥补） |
| **基于 TCP/IP** | HTTP/1.x 和 HTTP/2 基于 TCP，HTTP/3 基于 QUIC (UDP) |
| **请求-响应模型** | 客户端发送请求，服务器返回响应 |
| **明文传输** | HTTP 本身以明文传输数据，HTTPS 在 HTTP 基础上加 TLS/SSL 加密 |
| **灵活性** | 可以传输任意类型数据，通过 `Content-Type` 标识 |

### HTTP 请求-响应流程图

```
客户端 (浏览器)                        服务器 (Web Server)
     |                                        |
     |  ① 建立 TCP 连接 (三次握手)              |
     |<--------------------------------------->|
     |                                        |
     |  ② 发送 HTTP 请求报文                    |
     |---------------------------------------->|
     |                                        |
     |  ③ 服务器处理请求                         |
     |                              [处理中...] |
     |                                        |
     |  ④ 返回 HTTP 响应报文                    |
     |<----------------------------------------|
     |                                        |
     |  ⑤ 客户端解析并渲染页面                     |
     | [解析中...]                              |
     |                                        |
     |  ⑥ 断开 TCP 连接 (四次挥手)               |
     |<--------------------------------------->|

```

---

## 2. HTTP 请求 (Request)

HTTP 请求报文由三部分组成：**请求行**、**请求头**、**请求体**。

### 报文结构

```
┌──────────────────────────────────┐
│  请求行 (Request Line)            │  ← 请求方式 + URL + 协议版本
├──────────────────────────────────┤
│  请求头 (Request Headers)         │  ← 键值对，描述客户端信息
├──────────────────────────────────┤
│  空行 (CRLF)                      │  ← 请求头与请求体的分隔
├──────────────────────────────────┤
│  请求体 (Request Body)            │  ← 实际传输的数据 (可选)
└──────────────────────────────────┘
```

**示例请求报文**（GET 请求）：

```
GET /api/user?id=123 HTTP/1.1
Host: www.example.com
Accept: text/html,application/json
Accept-Encoding: gzip, deflate
Connection: keep-alive
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)

```

**示例请求报文**（POST 请求）：

```
POST /api/user HTTP/1.1
Host: www.example.com
Content-Type: application/json
Content-Length: 45
Accept: application/json
Connection: keep-alive

{"name": "张三", "age": 25, "email": "zhangsan@example.com"}
```

### 2.1 请求行 (Request Line)

请求行是请求报文的第一行，格式为：

```
请求方式  请求路径  HTTP协议版本
```

| 组成部分 | 说明 | 示例 |
|----------|------|------|
| **请求方式** | 操作的 HTTP 方法 | `GET`, `POST`, `PUT`, `DELETE` |
| **请求路径** | URL 或 URI 路径 | `/api/user?id=123` 或 `/index.html` |
| **协议版本** | HTTP 协议版本号 | `HTTP/1.1`, `HTTP/2`, `HTTP/3` |

#### URL 与 URI 的区别

| 概念 | 全称 | 说明 | 示例 |
|------|------|------|------|
| **URI** | Uniform Resource Identifier | 统一资源标识符，标识一个资源的字符串 | `/api/user/123` |
| **URL** | Uniform Resource Locator | 统一资源定位符，标识资源的位置（URI 的子集） | `http://www.example.com/api/user/123` |

```
URL 的完整格式：
协议://主机名[:端口]/路径?查询参数#片段标识符

例：https://www.example.com:8080/api/user?id=123&name=张三#section1
     └─┬──┘  └──────┬──────┘└┬─┘└────┬──────┘└────────┬─────┘└───┬───┘
      协议      主机名      端口   路径        查询参数(query)   片段(fragment)
```

### 2.2 请求头 (Request Headers)

请求头是一组键值对，用于向服务器传递客户端的附加信息。

#### 常用请求头

| 请求头名称 | 说明 | 示例值 |
|-----------|------|--------|
| **Host** | 请求的主机名（HTTP/1.1 必需） | `www.example.com:8080` |
| **Accept** | 客户端能接收的 MIME 类型 | `text/html, application/json` |
| **Accept-Encoding** | 客户端支持的压缩格式 | `gzip, deflate, br` |
| **Accept-Language** | 客户端偏好的语言 | `zh-CN, en-US;q=0.8` |
| **User-Agent** | 客户端软件信息（浏览器/操作系统） | `Mozilla/5.0 (Windows NT 10.0; Win64; x64)` |
| **Content-Type** | 请求体的数据类型 | `application/json`, `application/x-www-form-urlencoded` |
| **Content-Length** | 请求体的字节长度 | `1024` |
| **Connection** | 连接管理方式 | `keep-alive` (保持连接), `close` (关闭) |
| **Cookie** | 客户端存储的 Cookie 信息 | `sessionId=abc123; userId=456` |
| **Authorization** | 认证凭证 | `Bearer eyJhbG...` |
| **Referer** | 当前请求的来源页面 URL | `https://www.example.com/login` |
| **Cache-Control** | 缓存控制指令 | `no-cache`, `max-age=3600` |

### 2.3 请求体 (Request Body)

请求体（Request Body）包含实际发送给服务器的数据。**GET 请求通常没有请求体**，数据通过 URL 的查询参数传递；POST/PUT 请求一般包含请求体。

#### 常见请求体格式

| Content-Type | 说明 | 示例 |
|-------------|------|------|
| **application/json** | JSON 格式 | `{"name":"张三","age":25}` |
| **application/x-www-form-urlencoded** | 表单格式（键值对） | `name=%E5%BC%A0%E4%B8%89&age=25` |
| **multipart/form-data** | 表单上传文件 | 每个字段一个分段，包含文件二进制数据 |
| **text/plain** | 纯文本 | `这是一段纯文本数据` |

> **对比**：`application/x-www-form-urlencoded` 会将特殊字符进行 URL 编码（如中文 → `%E5%BC%A0%E4%B8%89`），适合简单的文本表单；`multipart/form-data` 用于文件上传，不对数据进行编码，直接传输二进制。

---

## 3. HTTP 响应 (Response)

HTTP 响应报文同样由三部分组成：**状态行**、**响应头**、**响应体**。

### 报文结构

```
┌──────────────────────────────────┐
│  状态行 (Status Line)             │  ← 协议版本 + 状态码 + 状态描述
├──────────────────────────────────┤
│  响应头 (Response Headers)        │  ← 键值对，描述服务器信息
├──────────────────────────────────┤
│  空行 (CRLF)                      │  ← 响应头与响应体的分隔
├──────────────────────────────────┤
│  响应体 (Response Body)           │  ← 服务器返回的数据
└──────────────────────────────────┘
```

**示例响应报文**：

```
HTTP/1.1 200 OK
Server: nginx/1.18.0
Content-Type: application/json;charset=UTF-8
Content-Length: 89
Connection: keep-alive
Date: Sat, 04 Jul 2026 12:00:00 GMT

{"code": 200, "message": "请求成功", "data": {"id": 1, "name": "张三", "age": 25}}
```

### 3.1 响应行/状态行 (Status Line)

状态行是响应报文的第一行，格式为：

```
HTTP协议版本  状态码  状态描述
```

| 组成部分 | 说明 | 示例 |
|----------|------|------|
| **协议版本** | HTTP 协议版本号 | `HTTP/1.1` |
| **状态码** | 3 位数字，表示请求的处理结果 | `200`, `404`, `500` |
| **状态描述** | 状态码对应的简短文字说明 | `OK`, `Not Found`, `Internal Server Error` |

### 3.2 响应头 (Response Headers)

响应头用于向客户端传递服务器信息及响应数据的元信息。

#### 常用响应头

| 响应头名称 | 说明 | 示例值 |
|-----------|------|--------|
| **Content-Type** | 响应体的数据类型和编码 | `text/html;charset=UTF-8` |
| **Content-Length** | 响应体的字节长度 | `1024` |
| **Server** | 服务器软件信息 | `nginx/1.18.0`, `Apache/2.4.41` |
| **Set-Cookie** | 向客户端设置 Cookie | `sessionId=abc123; HttpOnly; Secure` |
| **Location** | 重定向目标 URL（配合 3xx 状态码） | `https://www.example.com/home` |
| **Cache-Control** | 缓存控制指令 | `public, max-age=3600` |
| **Access-Control-Allow-Origin** | 允许跨域请求的源（CORS） | `*` 或 `https://www.example.com` |
| **Expires** | 资源过期时间 | `Sat, 04 Jul 2026 18:00:00 GMT` |
| **Content-Encoding** | 响应体的压缩格式 | `gzip`, `deflate` |
| **Etag** | 资源的版本标识（用于缓存验证） | `"33a64df5"` |

### 3.3 响应体 (Response Body)

响应体是服务器返回的实际数据，格式由 `Content-Type` 指定。

#### 常见响应体格式

| Content-Type | 说明 | 示例 |
|-------------|------|------|
| **text/html** | HTML 页面 | `<h1>Hello World</h1>` |
| **application/json** | JSON 数据 | `{"code":200,"data":{...}}` |
| **text/plain** | 纯文本 | `Operation successful` |
| **application/xml** | XML 数据 | `<user><name>张三</name></user>` |
| **image/png** | 图片（二进制） | [二进制数据] |
| **application/pdf** | PDF 文件 | [二进制数据] |

---

## 4. HTTP 状态码详解

HTTP 状态码由 3 位数字组成，第一位表示响应类别。

### 状态码分类

| 状态码范围 | 类别 | 含义 |
|-----------|------|------|
| **1xx** | Informational（信息性） | 请求已接收，继续处理 |
| **2xx** | Success（成功） | 请求已成功接收、理解并处理 |
| **3xx** | Redirection（重定向） | 需要进一步操作才能完成请求 |
| **4xx** | Client Error（客户端错误） | 请求包含语法错误或无法完成 |
| **5xx** | Server Error（服务器错误） | 服务器处理请求时出错 |

### 常见状态码速查表

| 状态码 | 状态描述 | 说明 | 常见场景 |
|--------|----------|------|----------|
| **100** | Continue | 客户端应继续发送请求体 | 大文件上传前的确认 |
| **200** | OK | 请求成功 | 正常返回数据 |
| **201** | Created | 资源创建成功 | POST 请求创建新资源 |
| **204** | No Content | 请求成功，但无返回内容 | 删除操作成功后 |
| **301** | Moved Permanently | 资源永久重定向 | 网站迁移到新域名 |
| **302** | Found | 临时重定向 | 登录后跳转到首页 |
| **304** | Not Modified | 资源未修改（缓存可用） | 缓存验证通过 |
| **400** | Bad Request | 请求参数有误 | 参数校验失败 |
| **401** | Unauthorized | 未认证，需要登录 | 访问需登录的 API |
| **403** | Forbidden | 已认证但权限不足 | 普通用户访问管理员功能 |
| **404** | Not Found | 请求的资源不存在 | 访问不存在的 URL |
| **405** | Method Not Allowed | 请求方法不被允许 | GET 访问只支持 POST 的接口 |
| **415** | Unsupported Media Type | 不支持的媒体类型 | 服务器只接受 JSON，却发送了 XML |
| **429** | Too Many Requests | 请求频率超过限制 | 接口限流 |
| **500** | Internal Server Error | 服务器内部错误 | 代码异常或数据库连接失败 |
| **502** | Bad Gateway | 网关错误 | 反向代理/网关从上游收到无效响应 |
| **503** | Service Unavailable | 服务暂时不可用 | 服务器维护或过载 |
| **504** | Gateway Timeout | 网关超时 | 上游服务器响应时间过长 |

---

## 5. HTTP 请求方法

HTTP 定义了多种请求方法，每种方法表示对资源的不同操作。

| 方法 | 说明 | 幂等性 | 安全性 | 请求体 |
|------|------|--------|--------|--------|
| **GET** | 获取资源 | ✅ 幂等 | ✅ 安全 | ❌ 无 |
| **POST** | 创建资源 / 提交数据 | ❌ 不幂等 | ❌ 不安全 | ✅ 有 |
| **PUT** | 更新资源（全量替换） | ✅ 幂等 | ❌ 不安全 | ✅ 有 |
| **PATCH** | 更新资源（部分修改） | ❌ 不幂等* | ❌ 不安全 | ✅ 有 |
| **DELETE** | 删除资源 | ✅ 幂等 | ❌ 不安全 | 可选 |
| **HEAD** | 获取响应头（不含响应体） | ✅ 幂等 | ✅ 安全 | ❌ 无 |
| **OPTIONS** | 查询服务器支持的方法 | ✅ 幂等 | ✅ 安全 | ❌ 无 |

> **幂等性**：多次执行相同请求，结果与执行一次相同。  
> **安全性**：只读操作，不修改服务器资源。

### RESTful API 中的方法使用

```
GET    /api/users          → 获取所有用户
GET    /api/users/123      → 获取 ID 为 123 的用户
POST   /api/users          → 创建新用户
PUT    /api/users/123      → 全量更新 ID 为 123 的用户
PATCH  /api/users/123      → 部分更新 ID 为 123 的用户
DELETE /api/users/123      → 删除 ID 为 123 的用户
```

---

## 6. 常见请求头与响应头

### 6.1 常用请求头

#### Accept 系列 —— 内容协商

客户端通过 `Accept` 系列头部告知服务器自己能接受的数据格式：

```http
Accept: text/html, application/json, */*
Accept-Encoding: gzip, deflate, br
Accept-Language: zh-CN, zh;q=0.9, en;q=0.8
```

> `q` 表示权重（priority），范围 0~1，越大优先级越高。

#### Content-Type —— 数据类型标识

| 值 | 适用场景 |
|----|---------|
| `application/json` | REST API 的 JSON 数据 |
| `application/x-www-form-urlencoded` | HTML 表单提交（无文件） |
| `multipart/form-data` | 包含文件上传的表单 |
| `text/plain` | 纯文本 |

#### Cookie 与会话

```http
Cookie: sessionId=abc123; theme=dark
```

#### Authorization —— 认证凭证

```http
Authorization: Basic dXNlcjpwYXNzd29yZA==
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 6.2 常用响应头

#### Set-Cookie

```http
Set-Cookie: sessionId=abc123; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=3600
```

| 属性 | 说明 |
|------|------|
| `HttpOnly` | 禁止 JavaScript 访问，防止 XSS 攻击 |
| `Secure` | 仅在 HTTPS 连接中传输 |
| `SameSite` | 控制跨站请求是否携带 Cookie（`Strict` / `Lax` / `None`） |
| `Max-Age` | Cookie 有效期（秒） |

#### CORS 跨域相关

```http
Access-Control-Allow-Origin: https://www.example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Max-Age: 3600
```

#### 缓存控制

```http
Cache-Control: public, max-age=3600
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"
Last-Modified: Mon, 03 Jul 2026 10:00:00 GMT
Expires: Sat, 04 Jul 2026 13:00:00 GMT
```

---

## 7. HTTP 协议版本演进

| 版本 | 发布年份 | 核心特性 | 连接方式 |
|------|---------|---------|---------|
| **HTTP/0.9** | 1991 | 仅支持 GET，无请求头/响应头 | 每次请求新建 TCP |
| **HTTP/1.0** | 1996 | 支持请求头、状态码、POST/HEAD 方法 | 短连接（默认 `Connection: close`） |
| **HTTP/1.1** | 1997 | 持久连接、管道化、Host 头、Chunked 传输 | 持久连接（`Connection: keep-alive`） |
| **HTTP/2** | 2015 | 多路复用、头部压缩(HPACK)、服务器推送、二进制分帧 | 单个 TCP 连接多路复用 |
| **HTTP/3** | 2022 | 基于 QUIC (UDP)、0-RTT 握手、改进拥塞控制 | QUIC 多路复用 |

### HTTP/1.1 存在的核心问题

- **队头阻塞**（Head-of-Line Blocking）：同一连接上的请求必须按顺序处理，前一个请求卡住会影响后续请求。
- **头部冗余**：每次请求都要发送完整头部，大量重复数据浪费带宽。

### HTTP/2 的改进

- **二进制分帧**：将数据拆分为二进制帧，多个流的帧交错传输。
- **多路复用**：在单 TCP 连接上同时发送多个请求-响应，互不阻塞。
- **头部压缩**：使用 HPACK 算法压缩请求头，大幅减少冗余。

### HTTP/3 的革新

- **基于 QUIC**：在 UDP 上实现可靠传输，消除了 TCP 层面的队头阻塞。
- **0-RTT 握手**：对于已建立过连接的服务器，可以零往返时间恢复会话。

---

## 8. Java Web 中的应用示例

以下示例基于 **Spring Boot / Jakarta Servlet** 框架，展示如何在 Java Web 中处理 HTTP 请求和响应。

### 8.1 获取 HTTP 请求数据

```java
@RestController
public class RequestController {

    @RequestMapping("/request")
    public String request(HttpServletRequest request) {
        // 1. 获取请求方式
        String method = request.getMethod();
        System.out.println("请求方式：" + method);

        // 2. 获取请求 URL 和 URI
        String url = request.getRequestURL().toString();
        String uri = request.getRequestURI();
        System.out.println("请求 URL：" + url);
        System.out.println("请求 URI：" + uri);

        // 3. 获取请求协议
        String protocol = request.getProtocol();
        System.out.println("请求协议：" + protocol);

        // 4. 获取请求头 - Accept
        String header = request.getHeader("Accept");
        System.out.println("Accept：" + header);

        // 5. 获取请求参数
        String name = request.getParameter("name");
        String age = request.getParameter("age");
        System.out.println("name：" + name + "  age：" + age);

        return "请求成功";
    }
}
```

**对应的 HTTP 请求示例**：

```
GET /request?name=张三&age=25 HTTP/1.1
Host: localhost:8080
Accept: text/html,application/json

```

### 8.2 设置 HTTP 响应数据

#### 方式一：通过 HttpServletResponse 直接设置

```java
@RestController
public class ResponseController {

    @RequestMapping("/response")
    public void response(HttpServletResponse response) throws IOException {
        // 1. 设置响应状态码
        response.setStatus(200);

        // 2. 设置响应头
        response.setHeader("name", "cds");
        response.setContentType("text/html;charset=UTF-8");

        // 3. 设置响应体
        response.getWriter().write("<h1>hello world</h1>");
    }
}
```

**对应的 HTTP 响应**：

```
HTTP/1.1 200 OK
name: cds
Content-Type: text/html;charset=UTF-8

<h1>hello world</h1>
```

#### 方式二：通过 ResponseEntity（推荐）

```java
@RestController
public class ResponseController {

    @RequestMapping("/response2")
    public ResponseEntity<String> response2() {
        return ResponseEntity
                .status(200)                              // 状态码
                .header("name", "cds")                    // 响应头
                .contentType(MediaType.TEXT_HTML)          // Content-Type
                .body("<h1>hello world</h1>");            // 响应体
    }
}
```

### 8.3 HTTP 请求/响应信息获取方法速查

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `request.getMethod()` | `String` | 获取请求方式（GET/POST/PUT/DELETE） |
| `request.getRequestURL()` | `StringBuffer` | 获取完整请求 URL |
| `request.getRequestURI()` | `String` | 获取请求 URI |
| `request.getProtocol()` | `String` | 获取协议版本 |
| `request.getHeader(name)` | `String` | 获取指定请求头的值 |
| `request.getParameter(name)` | `String` | 获取 URL 查询参数或表单参数 |
| `response.setStatus(code)` | `void` | 设置响应状态码 |
| `response.setHeader(key, value)` | `void` | 设置响应头 |
| `response.setContentType(type)` | `void` | 设置 Content-Type |
| `response.getWriter()` | `PrintWriter` | 获取输出流，写入响应体 |

---

## 参考资料

- [MDN Web Docs - HTTP](https://developer.mozilla.org/zh-CN/docs/Web/HTTP)
- [RFC 7230 - HTTP/1.1](https://datatracker.ietf.org/doc/html/rfc7230)
- [RFC 7540 - HTTP/2](https://datatracker.ietf.org/doc/html/rfc7540)
- [RFC 9114 - HTTP/3](https://datatracker.ietf.org/doc/html/rfc9114)
- [Spring Framework - ResponseEntity](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ResponseEntity.html)
