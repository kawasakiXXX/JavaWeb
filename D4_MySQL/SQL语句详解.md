# SQL 语句详解

> **SQL（Structured Query Language）** 是用于管理和操作关系型数据库的标准语言。根据功能的不同，SQL 语句通常分为以下几大类：

| 分类 | 全称 | 中文含义 | 核心作用 |
|------|------|----------|----------|
| **DDL** | Data Definition Language | 数据定义语言 | 定义数据库对象（库、表、索引等） |
| **DML** | Data Manipulation Language | 数据操纵语言 | 增删改表中的数据 |
| **DQL** | Data Query Language | 数据查询语言 | 查询表中的数据 |
| **DCL** | Data Control Language | 数据控制语言 | 控制数据库访问权限 |
| **TCL** | Transaction Control Language | 事务控制语言 | 管理数据库事务 |

---

## 一、DDL — 数据定义语言

> DDL 用于定义和修改数据库的结构，包括数据库、表、索引、视图等对象的**创建、修改、删除**。DDL 语句执行后通常会自动提交，无法回滚。

### 1.1 数据库操作

#### 创建数据库

```sql
-- 基本语法
CREATE DATABASE [IF NOT EXISTS] 数据库名
  [DEFAULT CHARACTER SET 字符集]
  [DEFAULT COLLATE 排序规则];

-- 示例
CREATE DATABASE mydb;                                  -- 最简单
CREATE DATABASE IF NOT EXISTS mydb;                     -- 如果不存在才创建（推荐）
CREATE DATABASE mydb DEFAULT CHARACTER SET utf8mb4;    -- 指定字符集
CREATE DATABASE mydb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;                   -- 同时指定字符集和排序规则
```

#### 查看与切换数据库

```sql
SHOW DATABASES;                         -- 查看所有数据库
SHOW CREATE DATABASE mydb;              -- 查看创建数据库的 SQL
SELECT DATABASE();                      -- 查看当前使用的数据库
USE mydb;                              -- 切换 / 使用某个数据库
```

#### 修改与删除数据库

```sql
ALTER DATABASE mydb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_bin;          -- 修改数据库字符集 / 排序规则

DROP DATABASE [IF EXISTS] mydb;         -- 删除数据库（危险操作！）
```

### 1.2 表操作

#### 创建表（CREATE TABLE）

```sql
CREATE TABLE [IF NOT EXISTS] 表名 (
    列名1 数据类型 [列级约束],
    列名2 数据类型 [列级约束],
    ...
    [表级约束]
) [ENGINE=存储引擎] [DEFAULT CHARSET=字符集] [COMMENT='表注释'];

-- 完整示例：创建员工表
CREATE TABLE IF NOT EXISTS employee (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT  COMMENT '主键ID',
    name        VARCHAR(50)  NOT NULL                    COMMENT '姓名',
    gender      CHAR(1)      DEFAULT 'U'                 COMMENT '性别: M-男, F-女, U-未知',
    age         TINYINT      CHECK (age BETWEEN 18 AND 65) COMMENT '年龄',
    email       VARCHAR(100) UNIQUE                      COMMENT '邮箱（唯一）',
    dept_id     INT                                       COMMENT '部门ID',
    salary      DECIMAL(10,2) DEFAULT 0.00               COMMENT '薪资',
    hire_date   DATE                                     COMMENT '入职日期',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status      TINYINT      DEFAULT 1                   COMMENT '状态: 1-在职, 0-离职'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';
```

#### 常用数据类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **INT / BIGINT** | 整数 | `INT`, `BIGINT` |
| **TINYINT** | 小整数（-128~127） | 状态码、性别 |
| **DECIMAL(M,D)** | 精确小数 | `DECIMAL(10,2)` → 12345678.90 |
| **FLOAT / DOUBLE** | 浮点数 | 科学计算 |
| **CHAR(N)** | 定长字符串 | `CHAR(1)` 性别 |
| **VARCHAR(N)** | 变长字符串 | `VARCHAR(50)` 姓名 |
| **TEXT** | 长文本 | 文章内容 |
| **DATE** | 日期 | `2026-07-06` |
| **TIME** | 时间 | `14:30:00` |
| **DATETIME** | 日期时间 | `2026-07-06 14:30:00` |
| **TIMESTAMP** | 时间戳 | 自动更新 |
| **BLOB** | 二进制数据 | 图片、文件 |

#### 列级约束 vs 表级约束

```sql
-- 列级约束：紧跟在列定义后
CREATE TABLE demo (
    id   BIGINT PRIMARY KEY,          -- 主键（列级）
    name VARCHAR(50) NOT NULL UNIQUE   -- 非空 + 唯一（列级）
);

-- 表级约束：在所有列定义之后
CREATE TABLE demo (
    id     BIGINT,
    name   VARCHAR(50) NOT NULL,
    dept_id INT,
    CONSTRAINT pk_demo PRIMARY KEY (id),                     -- 主键（表级）
    CONSTRAINT uk_name UNIQUE (name),                        -- 唯一（表级）
    CONSTRAINT fk_dept FOREIGN KEY (dept_id) REFERENCES dept(id) -- 外键（必须表级）
);
```

#### 约束详解

| 约束 | 关键字 | 说明 |
|------|--------|------|
| **主键** | `PRIMARY KEY` | 唯一且非空，每表只能有一个 |
| **外键** | `FOREIGN KEY ... REFERENCES` | 引用另一张表的主键，保证参照完整性 |
| **唯一** | `UNIQUE` | 值不可重复，但允许 NULL（多个 NULL） |
| **非空** | `NOT NULL` | 列值不能为 NULL |
| **默认值** | `DEFAULT` | 未指定值时使用的默认值 |
| **检查** | `CHECK` | 限制列的取值范围（MySQL 8.0+ 真正生效） |
| **自增** | `AUTO_INCREMENT` | 自动递增，通常用于主键 |

```sql
-- 外键约束详解
CREATE TABLE department (
    id   INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE employee (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(50) NOT NULL,
    dept_id INT,
    -- 外键：employee.dept_id 引用 department.id
    CONSTRAINT fk_emp_dept
      FOREIGN KEY (dept_id) REFERENCES department(id)
      ON DELETE SET NULL    -- 部门被删时，员工 dept_id 置为 NULL
      ON UPDATE CASCADE     -- 部门 id 更新时，级联更新
);

-- ON DELETE / ON UPDATE 选项：
-- CASCADE    级联删除/更新
-- SET NULL   外键列置为 NULL
-- RESTRICT   拒绝操作（默认）
-- NO ACTION  与 RESTRICT 类似
-- SET DEFAULT 设为默认值
```

#### 查看表

```sql
SHOW TABLES;                            -- 查看当前数据库的所有表
DESC employee;                          -- 查看表结构
SHOW CREATE TABLE employee;             -- 查看建表 SQL
SHOW COLUMNS FROM employee;             -- 查看列信息
SHOW INDEX FROM employee;               -- 查看索引信息
```

#### 修改表（ALTER TABLE）

```sql
-- 添加列
ALTER TABLE employee ADD COLUMN phone VARCHAR(20) COMMENT '电话';

-- 修改列定义
ALTER TABLE employee MODIFY COLUMN name VARCHAR(100) NOT NULL;

-- 修改列名 + 定义
ALTER TABLE employee CHANGE COLUMN phone mobile VARCHAR(20) COMMENT '手机号';

-- 删除列
ALTER TABLE employee DROP COLUMN mobile;

-- 添加约束
ALTER TABLE employee ADD CONSTRAINT uk_email UNIQUE (email);

-- 删除约束
ALTER TABLE employee DROP INDEX uk_email;           -- 删除唯一约束
ALTER TABLE employee DROP FOREIGN KEY fk_emp_dept;  -- 删除外键约束
ALTER TABLE employee DROP PRIMARY KEY;              -- 删除主键

-- 重命名表
ALTER TABLE employee RENAME TO staff;
RENAME TABLE staff TO employee;
```

#### 删除表

```sql
DROP TABLE [IF EXISTS] employee;        -- 删除表（数据 + 结构全删）
TRUNCATE [TABLE] employee;              -- 清空表数据，保留结构（无法回滚，比 DELETE 快）
```

> **TRUNCATE vs DELETE 的区别：**
>
> | 特性 | TRUNCATE | DELETE |
> |------|----------|--------|
> | 能否回滚 | ❌ 隐式提交 | ✅ 可回滚 |
> | 触发触发器 | ❌ 不触发 | ✅ 触发 |
> | WHERE 条件 | ❌ 不支持 | ✅ 支持 |
> | 自增计数器 | 重置为初始值 | 不重置 |
> | 执行速度 | 快（DDL） | 慢（逐行删） |

---

## 二、DML — 数据操纵语言

> DML 用于对表中的**数据进行增、删、改**操作。DML 操作在事务中可以回滚（前提是未提交）。

### 2.1 插入数据（INSERT）

#### 单行插入

```sql
-- 语法1：指定列名（推荐，列的对应关系清晰）
INSERT INTO employee (name, gender, age, email, dept_id, salary, hire_date)
VALUES ('张三', 'M', 28, 'zhangsan@example.com', 1, 15000.00, '2025-03-15');

-- 语法2：按表定义的列顺序插入全部列（自增列可用 NULL 或 DEFAULT）
INSERT INTO employee VALUES (NULL, '李四', 'F', 26, 'lisi@example.com',
                              2, 12000.00, '2025-06-01', NOW(), NOW(), 1);

-- 语法3：SET 方式
INSERT INTO employee
SET name = '王五', gender = 'M', age = 30, email = 'wangwu@example.com';
```

#### 批量插入

```sql
INSERT INTO employee (name, gender, age, email, dept_id, salary, hire_date) VALUES
  ('赵六', 'M', 35, 'zhaoliu@example.com', 1, 25000.00, '2024-01-10'),
  ('孙七', 'F', 29, 'sunqi@example.com',   2, 18000.00, '2024-06-20'),
  ('周八', 'M', 32, 'zhouba@example.com',  3, 22000.00, '2023-09-05');
```

#### INSERT INTO ... SELECT（复制数据）

```sql
-- 将查询结果插入到另一张表中
INSERT INTO employee_archive (id, name, gender, email, dept_id, salary, hire_date)
SELECT id, name, gender, email, dept_id, salary, hire_date
FROM employee
WHERE status = 0;  -- 将离职员工数据移到归档表
```

#### INSERT ... ON DUPLICATE KEY UPDATE（插入或更新）

```sql
-- 如果主键/唯一键冲突，则更新；否则插入
INSERT INTO employee (id, name, email, salary)
VALUES (1, '张三', 'new_zhangsan@example.com', 20000.00)
ON DUPLICATE KEY UPDATE
  email = VALUES(email),        -- VALUES() 取插入的值
  salary = VALUES(salary);
```

#### REPLACE INTO（替换插入）

```sql
-- 如果存在相同主键/唯一键，先删除再插入
REPLACE INTO employee (id, name, email, salary)
VALUES (1, '张三', 'zhangsan@example.com', 18000.00);
```

### 2.2 更新数据（UPDATE）

```sql
-- 基本语法
UPDATE 表名
SET 列名1 = 值1, 列名2 = 值2, ...
[WHERE 条件];

-- 示例：给所有部门1的员工涨薪10%
UPDATE employee
SET salary = salary * 1.1,
    updated_at = NOW()
WHERE dept_id = 1;

-- 示例：多表关联更新
UPDATE employee e
JOIN department d ON e.dept_id = d.id
SET e.salary = e.salary * 1.05
WHERE d.name = '研发部';

-- ⚠️ 危险：不加 WHERE 会更新全表！
-- UPDATE employee SET salary = 0;  -- 所有人薪资归零
```

> **安全建议：** 执行 UPDATE/DELETE 前，先用同条件的 SELECT 确认要操作的数据。

### 2.3 删除数据（DELETE）

```sql
-- 基本语法
DELETE FROM 表名 [WHERE 条件];

-- 示例：删除离职员工
DELETE FROM employee WHERE status = 0;

-- 多表关联删除
DELETE e FROM employee e
JOIN department d ON e.dept_id = d.id
WHERE d.name = '已撤销部门';

-- ⚠️ 危险：不加 WHERE 会删除全表数据！
-- DELETE FROM employee;  -- 删除所有行
```

---

## 三、DQL — 数据查询语言

> DQL 是 SQL 中最复杂、最常用的部分，用于从表中**查询数据**。核心是 `SELECT` 语句。

### 3.1 SELECT 完整语法结构

```sql
SELECT [ALL | DISTINCT] 列1, 列2, ...
FROM 表名 | 视图名
[JOIN 其他表 ON 连接条件]
[WHERE 过滤条件（分组前）]
[GROUP BY 分组列]
[HAVING 过滤条件（分组后）]
[ORDER BY 排序列 [ASC | DESC]]
[LIMIT [偏移量,] 行数];
```

#### 执行顺序（逻辑顺序）

```
FROM  →  JOIN  →  WHERE  →  GROUP BY  →  HAVING  →  SELECT  →  ORDER BY  →  LIMIT
```

### 3.2 基本查询

```sql
-- 查询所有列
SELECT * FROM employee;

-- 查询指定列
SELECT name, email, salary FROM employee;

-- 去重查询
SELECT DISTINCT dept_id FROM employee;

-- 别名（AS 可省略）
SELECT name AS '姓名', salary * 12 AS '年薪' FROM employee;

-- 常量列 / 表达式
SELECT name, salary, salary * 0.1 AS '绩效', '在职' AS status_label FROM employee;
```

### 3.3 条件查询（WHERE）

| 运算符 | 说明 | 示例 |
|--------|------|------|
| `=`, `<>` / `!=` | 等于 / 不等于 | `WHERE dept_id = 1` |
| `>`, `<`, `>=`, `<=` | 比较 | `WHERE salary > 15000` |
| `BETWEEN ... AND ...` | 范围（闭区间） | `WHERE age BETWEEN 25 AND 35` |
| `IN (...)` | 在集合中 | `WHERE dept_id IN (1, 2, 3)` |
| `LIKE` | 模糊匹配 | `WHERE name LIKE '张%'` |
| `IS NULL` / `IS NOT NULL` | 空值判断 | `WHERE email IS NULL` |
| `AND`, `OR`, `NOT` | 逻辑运算 | `WHERE age > 30 AND dept_id = 1` |

#### LIKE 模糊匹配

```sql
-- % 匹配任意多个字符
SELECT * FROM employee WHERE name LIKE '张%';    -- 姓张的
SELECT * FROM employee WHERE email LIKE '%@gmail.com';  -- Gmail 邮箱

-- _ 匹配单个字符
SELECT * FROM employee WHERE name LIKE '张_';    -- 姓张且名字只有一个字

-- 转义特殊字符
SELECT * FROM employee WHERE name LIKE '100\%' ESCAPE '\';  -- 匹配 "100%"
```

#### NULL 值处理

```sql
-- NULL 不能用 = 或 <> 比较
-- SELECT * FROM employee WHERE email = NULL;        -- 错误！永远返回空
SELECT * FROM employee WHERE email IS NULL;           -- 正确

-- NULL 安全等于（MySQL 特有）
SELECT * FROM employee WHERE email <=> NULL;          -- 同 IS NULL
```

### 3.4 排序（ORDER BY）

```sql
SELECT name, salary, hire_date FROM employee
ORDER BY salary DESC;                         -- 按薪资降序

SELECT name, dept_id, salary FROM employee
ORDER BY dept_id ASC, salary DESC;           -- 先按部门升序，再按薪资降序
```

### 3.5 分页（LIMIT）

```sql
-- 前10条
SELECT * FROM employee LIMIT 10;

-- 跳过5条，取10条（第6-15条）
SELECT * FROM employee LIMIT 5, 10;
SELECT * FROM employee LIMIT 10 OFFSET 5;    -- 等效写法（更可读）
```

#### 分页公式

```
LIMIT (pageNo - 1) * pageSize, pageSize

第1页，每页10条：LIMIT 0, 10
第2页，每页10条：LIMIT 10, 10
第3页，每页10条：LIMIT 20, 10
```

### 3.6 聚合函数

| 函数 | 说明 |
|------|------|
| `COUNT(*)` | 统计总行数（含 NULL） |
| `COUNT(列名)` | 统计该列非 NULL 的行数 |
| `SUM(列名)` | 求和 |
| `AVG(列名)` | 求平均 |
| `MAX(列名)` | 最大值 |
| `MIN(列名)` | 最小值 |

```sql
SELECT
  COUNT(*)        AS '总人数',
  COUNT(email)    AS '有邮箱的人数',  -- 不计 NULL
  AVG(salary)     AS '平均薪资',
  SUM(salary)     AS '薪资总额',
  MAX(salary)     AS '最高薪资',
  MIN(salary)     AS '最低薪资'
FROM employee;
```

### 3.7 分组查询（GROUP BY）

```sql
-- 按部门统计人数和平均薪资
SELECT
  dept_id,
  COUNT(*)     AS emp_count,
  AVG(salary)  AS avg_salary
FROM employee
GROUP BY dept_id;

-- 分组后过滤（HAVING）
SELECT
  dept_id,
  COUNT(*)     AS emp_count,
  AVG(salary)  AS avg_salary
FROM employee
GROUP BY dept_id
HAVING emp_count > 3;           -- 只显示人数 > 3 的部门
```

#### WHERE vs HAVING

| 区别 | WHERE | HAVING |
|------|-------|--------|
| 执行时机 | **分组前**过滤行 | **分组后**过滤分组结果 |
| 能否用聚合函数 | ❌ 不能 | ✅ 可以 |
| 性能 | 优（减少分组数据量） | 相对差 |

```sql
-- 正确用法：WHERE 过滤行，HAVING 过滤分组
SELECT dept_id, AVG(salary) AS avg_salary
FROM employee
WHERE status = 1          -- 先过滤：只要在职员工
GROUP BY dept_id
HAVING avg_salary > 15000; -- 再过滤：平均薪资 > 15000 的部门
```

### 3.8 连接查询（JOIN）

#### 连接类型总览

```
内连接: 只返回匹配的行
  ┌──────┐  ┌──────┐
  │  A∩B  │  │      │
  └──────┘  └──────┘

左外连接: 返回左表全部 + 右表匹配的行（无匹配填 NULL）
  ┌──────┬──────┐
  │  A   │  A∩B  │
  └──────┴──────┘

右外连接: 返回右表全部 + 左表匹配的行
  ┌──────┬──────┐
  │ A∩B  │  B   │
  └──────┴──────┘

全外连接: 返回两表全部（MySQL 不直接支持，用 UNION 模拟）
  ┌──────┬──────┬──────┐
  │  A   │ A∩B  │  B   │
  └──────┴──────┴──────┘
```

#### 内连接（INNER JOIN）

```sql
-- 显式内连接（推荐）
SELECT e.name, e.salary, d.name AS dept_name
FROM employee e
INNER JOIN department d ON e.dept_id = d.id;

-- 隐式内连接（不推荐，WHERE 中写连接条件）
SELECT e.name, e.salary, d.name AS dept_name
FROM employee e, department d
WHERE e.dept_id = d.id;
```

#### 左外连接（LEFT JOIN）

```sql
-- 查询所有员工及其部门（包括未分配部门的员工）
SELECT e.name, d.name AS dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.id;

-- 找出没有分配部门的员工
SELECT e.name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.id
WHERE d.id IS NULL;
```

#### 右外连接（RIGHT JOIN）

```sql
-- 查询所有部门及其员工（包括没有员工的部门）
SELECT d.name AS dept_name, e.name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.id;
```

#### 自连接

```sql
-- 表内关联：员工表中存了 manager_id（上司 ID）
SELECT
  e.name AS '员工',
  m.name AS '上司'
FROM employee e
LEFT JOIN employee m ON e.manager_id = m.id;
```

#### 交叉连接（CROSS JOIN）— 笛卡尔积

```sql
SELECT e.name, d.name
FROM employee e
CROSS JOIN department d;  -- 每行员工 × 每行部门，慎用！
```

### 3.9 子查询

子查询是嵌套在另一个 SQL 语句中的 SELECT 查询。

#### 标量子查询（返回单个值）

```sql
-- 查询薪资高于平均薪资的员工
SELECT name, salary FROM employee
WHERE salary > (SELECT AVG(salary) FROM employee);

-- 查询与"张三"同部门的员工
SELECT name FROM employee
WHERE dept_id = (SELECT dept_id FROM employee WHERE name = '张三');
```

#### 列子查询（返回一列，配合 IN / ANY / ALL）

```sql
-- IN：查询有员工的部门
SELECT name FROM department
WHERE id IN (SELECT DISTINCT dept_id FROM employee WHERE dept_id IS NOT NULL);

-- ALL：查询薪资比所有部门1员工都高的员工
SELECT name, salary FROM employee
WHERE salary > ALL (SELECT salary FROM employee WHERE dept_id = 1);

-- ANY：查询薪资比部门1中任意一个员工高的员工
SELECT name, salary FROM employee
WHERE salary > ANY (SELECT salary FROM employee WHERE dept_id = 1);
```

#### 行子查询（返回一行多列）

```sql
-- 查询和"张三"同部门且同薪资的员工
SELECT name FROM employee
WHERE (dept_id, salary) = (
  SELECT dept_id, salary FROM employee WHERE name = '张三'
);
```

#### 表子查询（在 FROM 后作为派生表，必须有别名）

```sql
-- 查询各部门平均薪资中超过 15000 的
SELECT dept_id, avg_sal
FROM (
  SELECT dept_id, AVG(salary) AS avg_sal
  FROM employee
  GROUP BY dept_id
) AS dept_stats          -- ← 派生表必须有别名
WHERE avg_sal > 15000;
```

#### EXISTS 子查询（相关子查询，判断是否存在）

```sql
-- 查询有员工的部门
SELECT name FROM department d
WHERE EXISTS (
  SELECT 1 FROM employee e WHERE e.dept_id = d.id
);

-- 查询没有员工的部门
SELECT name FROM department d
WHERE NOT EXISTS (
  SELECT 1 FROM employee e WHERE e.dept_id = d.id
);
```

> **EXISTS vs IN 的选择：**
> - 外表大、子查询小 → 用 `IN`
> - 外表小、子查询大 → 用 `EXISTS`
> - `NOT EXISTS` 可用索引，`NOT IN` 遇到 NULL 有陷阱，推荐优先用 `NOT EXISTS`

### 3.10 UNION 联合查询

```sql
-- UNION：合并结果并去重
SELECT name, email FROM employee
UNION
SELECT name, email FROM employee_archive;

-- UNION ALL：合并结果不去重（更快）
SELECT name, email FROM employee
UNION ALL
SELECT name, email FROM employee_archive;

-- 要求：列数、列类型、列顺序必须一致
```

### 3.11 常用内置函数

#### 字符串函数

```sql
SELECT CONCAT(name, ' - ', email) FROM employee;     -- 拼接
SELECT UPPER(name), LOWER(email) FROM employee;       -- 大小写
SELECT TRIM('  hello  ');                             -- 去空格 → 'hello'
SELECT SUBSTRING('HelloWorld', 1, 5);                 -- 截取 → 'Hello'
SELECT REPLACE('HelloWorld', 'World', 'MySQL');       -- 替换
SELECT LENGTH('你好');                                  -- 字节长度
SELECT CHAR_LENGTH('你好');                             -- 字符长度 → 2
SELECT LPAD('5', 3, '0');                             -- 左填充 → '005'
```

#### 数值函数

```sql
SELECT ROUND(123.456, 2);     -- 四舍五入 → 123.46
SELECT CEIL(123.1);           -- 向上取整 → 124
SELECT FLOOR(123.9);          -- 向下取整 → 123
SELECT MOD(10, 3);            -- 取余 → 1
SELECT RAND();                -- 随机数 [0, 1)
SELECT ABS(-10);              -- 绝对值 → 10
```

#### 日期函数

```sql
SELECT NOW();                              -- 当前日期时间 → 2026-07-06 10:30:00
SELECT CURDATE();                          -- 当前日期 → 2026-07-06
SELECT CURTIME();                          -- 当前时间 → 10:30:00
SELECT YEAR(NOW());                        -- 年份 → 2026
SELECT MONTH(NOW());                       -- 月份 → 7
SELECT DAY(NOW());                         -- 日 → 6
SELECT DATE_ADD(NOW(), INTERVAL 7 DAY);    -- 7天后
SELECT DATE_SUB(NOW(), INTERVAL 1 MONTH);  -- 1个月前
SELECT DATEDIFF('2026-07-20', '2026-07-06'); -- 日期差 → 14
SELECT DATE_FORMAT(NOW(), '%Y年%m月%d日');   -- 格式化 → '2026年07月06日'
```

#### 流程控制函数

```sql
-- IF(条件, 真值, 假值)
SELECT name, IF(salary > 15000, '高薪', '普通') AS level FROM employee;

-- CASE WHEN ... THEN ... ELSE ... END
SELECT name, salary,
  CASE
    WHEN salary >= 20000 THEN '高薪'
    WHEN salary >= 10000 THEN '中等'
    ELSE '普通'
  END AS salary_level
FROM employee;

-- IFNULL(expr, 替代值)
SELECT name, IFNULL(email, '未填写邮箱') FROM employee;

-- COALESCE 返回第一个非 NULL 值
SELECT name, COALESCE(mobile, phone, '无联系方式') FROM employee;
```

---

## 四、DCL — 数据控制语言

> DCL 用于管理数据库用户的**访问权限**，控制谁能对数据库做什么。

### 4.1 用户管理

```sql
-- 创建用户
CREATE USER 'username'@'host' IDENTIFIED BY 'password';

-- 示例
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'Str0ng@Pass';
CREATE USER 'app_user'@'%'         IDENTIFIED BY 'Str0ng@Pass';  -- % 表示任意主机

-- 修改密码
ALTER USER 'app_user'@'localhost' IDENTIFIED BY 'NewStr0ng@Pass';

-- 删除用户
DROP USER 'app_user'@'localhost';

-- 查看所有用户
SELECT Host, User FROM mysql.user;
```

### 4.2 权限管理

#### 常见权限列表

| 权限 | 说明 |
|------|------|
| `ALL PRIVILEGES` / `ALL` | 所有权限 |
| `SELECT` | 查询数据 |
| `INSERT` | 插入数据 |
| `UPDATE` | 更新数据 |
| `DELETE` | 删除数据 |
| `CREATE` | 创建表 / 数据库 |
| `ALTER` | 修改表结构 |
| `DROP` | 删除表 / 数据库 |
| `INDEX` | 创建 / 删除索引 |
| `EXECUTE` | 执行存储过程 / 函数 |
| `GRANT OPTION` | 授予其他用户权限 |

```sql
-- 授予权限
GRANT 权限列表 ON 数据库.表 TO 'username'@'host';

-- 示例
GRANT SELECT, INSERT, UPDATE, DELETE ON mydb.* TO 'app_user'@'localhost';   -- 特定权限
GRANT ALL PRIVILEGES ON mydb.* TO 'admin'@'localhost';                       -- 全部权限
GRANT SELECT ON mydb.employee TO 'readonly'@'%';                             -- 单表只读

-- 授予 WITH GRANT OPTION（允许该用户再将权限授给其他人）
GRANT SELECT ON mydb.* TO 'manager'@'localhost' WITH GRANT OPTION;

-- 查看权限
SHOW GRANTS FOR 'app_user'@'localhost';

-- 撤销权限
REVOKE DELETE ON mydb.* FROM 'app_user'@'localhost';

-- 刷新权限（让权限修改立刻生效）
FLUSH PRIVILEGES;
```

### 4.3 角色管理（MySQL 8.0+）

```sql
-- 创建角色
CREATE ROLE 'read_only', 'read_write', 'admin_role';

-- 给角色授权
GRANT SELECT ON mydb.* TO 'read_only';
GRANT SELECT, INSERT, UPDATE, DELETE ON mydb.* TO 'read_write';
GRANT ALL PRIVILEGES ON mydb.* TO 'admin_role';

-- 将角色授予用户
GRANT 'read_only' TO 'app_user'@'localhost';

-- 设置默认角色（登录时自动激活）
SET DEFAULT ROLE 'read_only' TO 'app_user'@'localhost';
```

---

## 五、TCL — 事务控制语言

> TCL 用于管理数据库**事务**，保证一组操作的原子性——要么全部成功，要么全部回滚。

### 5.1 事务的四大特性（ACID）

| 特性 | 说明 |
|------|------|
| **原子性 (Atomicity)** | 事务中的操作要么全做，要么全不做 |
| **一致性 (Consistency)** | 事务前后数据必须满足所有约束 |
| **隔离性 (Isolation)** | 并发事务之间互不干扰 |
| **持久性 (Durability)** | 事务提交后，数据永久保存 |

### 5.2 基本事务操作

```sql
-- 开启事务
START TRANSACTION;
-- 或 BEGIN;
-- 或 BEGIN WORK;

-- 执行操作
UPDATE account SET balance = balance - 1000 WHERE id = 1;
UPDATE account SET balance = balance + 1000 WHERE id = 2;

-- 提交（确认修改）
COMMIT;

-- 回滚（撤销修改）
ROLLBACK;
```

### 5.3 保存点（Savepoint）

```sql
START TRANSACTION;

INSERT INTO employee (name, email) VALUES ('测试1', 'test1@example.com');
SAVEPOINT sp1;                           -- 设置保存点

INSERT INTO employee (name, email) VALUES ('测试2', 'test2@example.com');
SAVEPOINT sp2;                           -- 设置第二个保存点

INSERT INTO employee (name, email) VALUES ('测试3', 'test3@example.com');

ROLLBACK TO SAVEPOINT sp1;              -- 回滚到 sp1，撤销 sp1 之后的所有操作
-- ROLLBACK TO SAVEPOINT sp2;           -- 回滚到 sp2

RELEASE SAVEPOINT sp1;                  -- 释放保存点

COMMIT;                                  -- 最终提交
```

### 5.4 事务隔离级别

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;              -- MySQL 8.0+
SELECT @@tx_isolation;                       -- MySQL 5.x

-- 设置隔离级别（会话级别）
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 设置隔离级别（全局级别）
SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | MySQL 默认 |
|----------|:----:|:--------:|:----:|:----------:|
| `READ UNCOMMITTED` | ✅ | ✅ | ✅ | |
| `READ COMMITTED` | ❌ | ✅ | ✅ | |
| `REPEATABLE READ` | ❌ | ❌ | ❌* | ✅ |
| `SERIALIZABLE` | ❌ | ❌ | ❌ | |

> *MySQL 的 `REPEATABLE READ` 通过间隙锁（Gap Lock）解决了大部分幻读问题。

### 5.5 自动提交

```sql
-- 查看自动提交状态
SHOW VARIABLES LIKE 'autocommit';          -- 默认 ON

-- 关闭自动提交（之后需要手动 COMMIT / ROLLBACK）
SET autocommit = 0;

-- 开启自动提交
SET autocommit = 1;
```

> 当 `autocommit = ON` 时，每条 DML 语句都是一个独立的事务；`autocommit = OFF` 时多条语句在一个事务中。

---

## 六、索引操作

> 索引虽然常用 DDL 创建，但它对查询性能影响巨大，单独拿出来讲解。

### 6.1 索引类型

| 索引类型 | 说明 | 适用场景 |
|----------|------|----------|
| **普通索引** | 无限制 | 一般查询 |
| **唯一索引** | 值必须唯一 | 邮箱、手机号 |
| **主键索引** | 唯一且非空，每个表只能有一个 | 主键列 |
| **全文索引** | 用于全文搜索 | 文章内容搜索 |
| **复合索引** | 多列组成一个索引 | 多条件查询 |
| **空间索引** | 地理空间数据 | GIS 应用 |

### 6.2 索引操作

```sql
-- 创建索引
CREATE [UNIQUE | FULLTEXT | SPATIAL] INDEX 索引名 ON 表名(列名1 [ASC|DESC], ...);

-- 示例
CREATE INDEX idx_name ON employee(name);                     -- 普通索引
CREATE UNIQUE INDEX uk_email ON employee(email);             -- 唯一索引
CREATE INDEX idx_dept_salary ON employee(dept_id, salary);   -- 复合索引

-- 添加索引（ALTER TABLE 方式）
ALTER TABLE employee ADD INDEX idx_age(age);
ALTER TABLE employee ADD UNIQUE uk_phone(phone);

-- 删除索引
DROP INDEX idx_name ON employee;
ALTER TABLE employee DROP INDEX uk_email;

-- 查看索引
SHOW INDEX FROM employee;
```

### 6.3 索引设计原则

1. **WHERE / JOIN / ORDER BY 涉及的列**建索引
2. **区分度高的列**放在复合索引前面
3. **避免过多索引**（增删改变慢）
4. **短索引更好**（前缀索引）
5. 使用 `EXPLAIN` 分析查询是否走索引

```sql
-- 使用 EXPLAIN 分析查询
EXPLAIN SELECT * FROM employee WHERE dept_id = 1 AND salary > 10000;
-- key 列：使用的索引
-- type 列：ALL(全表扫) < index < range < ref < const（越靠后越快）
-- rows 列：扫描行数

-- 强制使用索引
SELECT * FROM employee FORCE INDEX(idx_dept_salary) WHERE dept_id = 1;
```

---

## 七、视图（VIEW）

> 视图是**虚拟表**，基于 SQL 查询结果定义，不存储实际数据。

```sql
-- 创建视图
CREATE VIEW v_active_employee AS
SELECT e.name, e.email, e.salary, d.name AS dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.id
WHERE e.status = 1;

-- 使用视图（和查表一样）
SELECT * FROM v_active_employee WHERE salary > 15000;

-- 查看视图定义
SHOW CREATE VIEW v_active_employee;

-- 修改视图
CREATE OR REPLACE VIEW v_active_employee AS
SELECT name, email, salary FROM employee WHERE status = 1;

-- 删除视图
DROP VIEW IF EXISTS v_active_employee;
```

> 视图的作用：**简化复杂查询、隐藏敏感列、逻辑独立性**。

---

## 八、存储过程与函数

### 8.1 存储过程

```sql
-- 修改分隔符（因为过程体里可能有 ;）
DELIMITER //

CREATE PROCEDURE get_employees_by_dept(IN dept_id_param INT)
BEGIN
  SELECT name, email, salary
  FROM employee
  WHERE dept_id = dept_id_param AND status = 1;
END //

DELIMITER ;

-- 调用存储过程
CALL get_employees_by_dept(1);

-- 带 OUT 参数的存储过程
DELIMITER //

CREATE PROCEDURE get_dept_stats(IN dept_id_param INT, OUT emp_count INT, OUT avg_sal DECIMAL(10,2))
BEGIN
  SELECT COUNT(*), AVG(salary) INTO emp_count, avg_sal
  FROM employee
  WHERE dept_id = dept_id_param;
END //

DELIMITER ;

-- 调用带 OUT 参数的存储过程
CALL get_dept_stats(1, @count, @avg_sal);
SELECT @count AS '人数', @avg_sal AS '平均薪资';

-- 删除存储过程
DROP PROCEDURE IF EXISTS get_employees_by_dept;
```

### 8.2 函数

```sql
DELIMITER //

CREATE FUNCTION calc_annual_salary(monthly_salary DECIMAL(10,2), months INT)
RETURNS DECIMAL(10,2)
DETERMINISTIC                 -- 确定性函数，相同输入必有相同输出
READS SQL DATA                -- 只读数据
BEGIN
  DECLARE result DECIMAL(10,2);
  SET result = monthly_salary * months;
  RETURN result;
END //

DELIMITER ;

-- 使用函数
SELECT name, salary, calc_annual_salary(salary, 13) AS annual_salary FROM employee;

-- 删除函数
DROP FUNCTION IF EXISTS calc_annual_salary;
```

### 8.3 存储过程 vs 函数

| 区别 | 存储过程 | 函数 |
|------|----------|------|
| 返回值 | 可以有多个 OUT 参数 | 必须有且只有一个返回值 |
| 调用方式 | `CALL procedure(...)` | `SELECT function(...)` |
| 能否在 SQL 中使用 | ❌ | ✅ |
| 事务控制 | ✅ 可用 COMMIT/ROLLBACK | ❌ 不可用 |
| 适用场景 | 批量处理、复杂业务 | 计算、转换 |

---

## 九、触发器（TRIGGER）

> 触发器在 INSERT / UPDATE / DELETE 操作**之前或之后**自动执行。

```sql
-- 语法
CREATE TRIGGER 触发器名
{BEFORE | AFTER} {INSERT | UPDATE | DELETE}
ON 表名 FOR EACH ROW
BEGIN
  -- 触发器逻辑
  -- NEW: 新插入/更新后的行
  -- OLD: 更新前/删除前的行
END;

-- 示例：插入员工时自动记录日志
DELIMITER //

CREATE TRIGGER trg_employee_insert
AFTER INSERT ON employee
FOR EACH ROW
BEGIN
  INSERT INTO audit_log (table_name, record_id, action, created_at)
  VALUES ('employee', NEW.id, 'INSERT', NOW());
END //

DELIMITER ;

-- 示例：更新员工时自动更新 updated_at
DELIMITER //

CREATE TRIGGER trg_employee_update
BEFORE UPDATE ON employee
FOR EACH ROW
BEGIN
  SET NEW.updated_at = NOW();
END //

DELIMITER ;

-- 查看触发器
SHOW TRIGGERS;

-- 删除触发器
DROP TRIGGER IF EXISTS trg_employee_insert;
```

---

## 十、最佳实践与常见问题

### 10.1 SQL 编写规范

```sql
-- ✅ 好的写法
SELECT e.name, e.email, d.name AS dept_name
FROM employee e
INNER JOIN department d ON e.dept_id = d.id
WHERE e.status = 1
  AND e.salary > 10000
ORDER BY e.salary DESC
LIMIT 20;

-- ❌ 避免
SELECT * FROM employee, department WHERE employee.dept_id = department.id;
--   ① 用 SELECT *      ② 隐式连接    ③ 无别名
```

**规范要点：**
1. 关键字大写，表名列名小写
2. 用显式 `JOIN ... ON`，不要隐式连接
3. 避免 `SELECT *`，明确列出需要的列
4. 表起有意义的别名（`e`, `d`）
5. 复杂查询添加注释

### 10.2 SQL 注入防护

```java
// ❌ 危险：拼接 SQL
String sql = "SELECT * FROM user WHERE username='" + username + "' AND password='" + password + "'";

// ✅ 安全：使用 PreparedStatement（参数化查询）
String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, username);
pstmt.setString(2, password);
```

### 10.3 性能优化速查

| 优化手段 | 说明 |
|----------|------|
| **加合适的索引** | 最重要，用 `EXPLAIN` 验证 |
| **避免 SELECT *** | 只取需要的列，减少网络传输 |
| **用 LIMIT** | 分页查询，减少数据量 |
| **WHERE 中不用函数包裹列** | `WHERE YEAR(hire_date) = 2025` → 不走索引 |
| **小表驱动大表** | IN 子查询用小表，EXISTS 用大表 |
| **避免在 WHERE 中用 OR** | 考虑用 UNION ALL 替代 |
| **用连接代替子查询** | 连接通常优化器更友好 |
| **批量操作** | 用批量 INSERT 代替逐条插入 |

---

> 📚 **总结**：SQL 语句的核心分类就是 **DDL（建）、DML（增删改）、DQL（查）、DCL（权限）、TCL（事务）**。其中 DQL 的 SELECT 查询是最常用也最复杂的部分，掌握好 JOIN、子查询、聚合函数和索引是写好 SQL 的关键。
