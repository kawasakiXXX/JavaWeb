# Maven 多模块项目实践总结

> 基于 `javaweb01` 项目从单模块拆分为多模块的实战经验

---

## 一、继承（Inheritance）

### 概念
父 POM 中定义公共配置，子模块通过 `<parent>` 标签继承，避免重复配置。

### 项目中的体现

```
web_parent (父工程，packaging=pom)
  ├── web_pojo     (子模块 → 继承 lombok、java.version)
  ├── web_utils    (子模块 → 继承 lombok、java.version)
  └── JavaWeb_demo (子模块 → 继承 lombok、java.version)
```

### 父 POM 关键配置 (`web_parent/pom.xml`)

| 配置项 | 说明 |
|---|---|
| `<packaging>pom</packaging>` | 父工程必须是 pom 类型 |
| `<dependencies>` 中的依赖 | 所有子模块**无条件继承**（如 lombok） |
| `<dependencyManagement>` | 只统一版本，子模块用到时才引入 |
| `<relativePath>../web_parent/pom.xml</relativePath>` | 告诉 Maven 父 POM 位置 |

### 子模块关键配置

```xml
<parent>
    <groupId>com.cds</groupId>
    <artifactId>web_parent</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>../web_parent/pom.xml</relativePath>
</parent>
```

---

## 二、聚合（Aggregation）

### 概念
父 POM 通过 `<modules>` 声明所有子模块，在父目录执行 `mvn install` 时**按依赖顺序**自动构建全部模块。

### 项目中的体现

```xml
<!-- web_parent/pom.xml -->
<modules>
    <module>../web_pojo</module>
    <module>../web_utils</module>
    <module>../JavaWeb_demo</module>
</modules>
```

### 聚合 vs 继承 对比

| | 聚合 | 继承 |
|---|---|---|
| 配置位置 | `<modules>` | `<parent>` |
| 谁声明 | 父声明子 | 子声明父 |
| 作用 | 一键构建所有模块 | 复用公共配置 |
| 关系 | 父子平级，都可以独立存在 | 子依赖父 |

---

## 三、版本控制（Version Management）

### 三层版本体系

```
spring-boot-starter-parent (3.3.0)
  └── web_parent (1.0-SNAPSHOT)
        ├── java.version = 17    ← 自定义属性，控制编译目标
        │
        ├── Spring 全家桶        ← 版本由 spring-boot-starter-parent 管控
        ├── lombok (1.18.44)     ← 在父 POM 中锁定版本
        └── jjwt (0.12.6)        ← 在子模块中声明版本
```

### 两种版本控制方式

| 方式 | 配置 | 效果 |
|---|---|---|
| **直接 `dependencies`** | 父 POM 写死 groupId + artifactId + version | 子模块自动获得,不需要重复声明 |
| **`dependencyManagement`** | 父 POM 只声明版本 | 子模块用的时候可省略 `<version>` |

### 实战教训：JDK 版本坑

```xml
<!-- ❌ 错误：直接写死不灵活，且可能和 Spring Boot 版本不兼容 -->
<maven.compiler.source>25</maven.compiler.source>

<!-- ✅ 正确：用属性统一管理 -->
<java.version>17</java.version>
<!-- maven-compiler-plugin 自动读取 ${java.version} -->
```

> Spring Boot 3.3.0 的 ASM 库不支持 Java 25 的 class 文件（major version 69），编译目标必须与 Spring Boot 版本匹配。

---

## 四、自定义属性（Custom Properties）

### 概念
在 `<properties>` 中定义变量，用 `${变量名}` 引用，统一管理可变的配置值。

### 项目中的体现

```xml
<!-- web_parent/pom.xml -->
<properties>
    <java.version>17</java.version>                    <!-- JDK 编译版本 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>  <!-- 编码 -->
</properties>
```

### 属性继承链

```
spring-boot-starter-parent 定义 java.version 默认值
  └── web_parent 重写为 17
        ├── web_pojo → 继承 = 17
        ├── web_utils → 继承 = 17
        └── JavaWeb_demo → 继承 = 17
```

### 常见自定义属性场景

| 属性 | 用途 |
|---|---|
| `java.version` | 被 Spring Boot 父 POM 的 maven-compiler-plugin 读取 |
| `<project.build.sourceEncoding>` | 防止中文注释/文件编码问题 |
| 自定义版本号属性 | 如 `<jjwt.version>0.12.6</jjwt.version>`，一处改全局生效 |

---

## 五、私服（Private Repository）

### 概念
私服是公司/团队内部的 Maven 仓库（如 Nexus、Artifactory），用于：

| 作用 | 说明 |
|---|---|
| **代理缓存** | 缓存中央仓库的依赖，加速下载 |
| **托管私有包** | 存放公司内部模块（如 `web_pojo`、`web_utils`）供团队共享 |
| **版本发布** | 发布 RELEASE/SNAPSHOT 版本，统一管理 |

### 与本项目的关联

当前项目是**本地多模块**模式，不需要私服。但如果团队化开发：

```xml
<!-- settings.xml 或 pom.xml 中配置私服地址 -->
<repositories>
    <repository>
        <id>company-nexus</id>
        <url>http://nexus.company.com/repository/maven-public/</url>
    </repository>
</repositories>
```

开发人员只需 `git pull` + `mvn install`，无需手动安装 `web_pojo`/`web_utils` 到本地仓库。

### 本地 vs 私服 vs 中央仓库

```
中央仓库 (Maven Central)
    ↑
  私服 (Nexus/Artifactory) ← 公司内部包
    ↑
  本地仓库 (~/.m2/repository) ← mvn install 安装的包
    ↑
  你的本地项目
```

---

## 六、今日踩坑复盘

| 坑 | 原因 | 解决 |
|---|---|---|
| `com.cds:web_pojo:jar:1.0-SNAPSHOT` 找不到 | 独立模块未 `mvn install` 到本地仓库 | 创建聚合 POM，一键构建 |
| `java: 程序包com.cds.pojo不存在` | import 包名写错（`web_01.pojo` vs `pojo`） | 统一包名，全局替换 |
| `Unsupported class file major version 69` | 编译目标 Java 25，Spring Boot 3.3 不支持 | 改为 `java.version=17` |
| `org.slf4j` 不存在 | web_utils 用了 @Slf4j 但缺少 slf4j-api | 显式添加 slf4j-api 依赖 |
| JAXB 依赖警告 | 历史遗留，无人使用 | 删除冗余依赖 |

---

## 七、最终项目结构

```
javaweb01/
├── web_parent/               ← 父工程（聚合 + 继承）
│   └── pom.xml               → lombok, java.version=17, modules
│
├── web_pojo/                 ← 实体类模块
│   └── pom.xml               → 只继承父 POM，无额外依赖
│
├── web_utils/                ← 工具类模块
│   └── pom.xml               → slf4j-api + jjwt
│
└── JavaWeb_demo/             ← 主应用模块
    └── pom.xml               → Spring Boot + MyBatis + OSS + 本地模块
```
