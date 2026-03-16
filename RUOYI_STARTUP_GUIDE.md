# RuoYi 若依框架启动与开发指南（新手文档）

> 适用版本：RuoYi v4.8.2（前后端不分离版）  
> 操作系统：Windows  
> 日期：2026-03-15

---

## 一、环境要求

| 软件 | 版本 | 用途 | 验证命令 |
|------|------|------|---------|
| JDK | **17+**（必须） | Java运行环境 | `java -version` |
| Maven | 3.6+ | 项目构建 | `mvn -version` |
| MySQL | 5.7+ 或 8.0+ | 数据库 | Navicat连接测试 |
| Navicat | 任意版本 | 数据库管理工具 | — |
| IDE | VS Code / IntelliJ IDEA | 代码编辑 | — |

---

## 二、首次启动（全新环境）

### 步骤1：创建数据库

打开 Navicat → 连接 MySQL（localhost:3306, root/root）→ 执行：

```sql
CREATE DATABASE ruoyi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

或者在 Navicat 中：右键连接 → 新建数据库 → 名称`ruoyi`、字符集`utf8mb4`、排序规则`utf8mb4_general_ci`

### 步骤2：导入SQL脚本

在 Navicat 中双击打开 `ruoyi` 数据库，然后右键 → 运行SQL文件，**按顺序**导入：

| 顺序 | 文件路径 | 说明 |
|------|---------|------|
| 1 | `RuoYi-master\RuoYi-master\sql\ry_20250416.sql` | 框架核心表（20张）+ 初始数据 |
| 2 | `RuoYi-master\RuoYi-master\sql\quartz.sql` | 定时任务引擎表（11张） |

导入完成后，刷新数据库，应该能看到 **31张表**。

### 步骤3：检查数据库连接配置

打开文件：`RuoYi-master\RuoYi-master\ruoyi-admin\src\main\resources\application-druid.yml`

确认以下内容与你的MySQL一致：

```yaml
spring:
    datasource:
        druid:
            master:
                url: jdbc:mysql://localhost:3306/ruoyi?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
                username: root
                password: root    # ← 改成你的MySQL密码
```

> **重要**：如果你的数据库名不是`ruoyi`，需要修改url中的数据库名。如果MySQL密码不是`root`，需要修改password。

### 步骤4：编译项目

打开终端（PowerShell/CMD），进入项目根目录：

```bash
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
mvn clean install -DskipTests
```

首次编译需要下载依赖，预计耗时 **3-10分钟**（取决于网速）。

看到以下输出表示编译成功：

```
[INFO] BUILD SUCCESS
[INFO] Total time: xx:xx min
```

### 步骤5：启动项目

```bash
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

看到以下ASCII图案表示启动成功：

```
(♥◠‿◠)ﾉﾞ  若依启动成功   ლ(´ڡ`ლ)ﾞ
```

### 步骤6：访问系统

| 地址 | 说明 |
|------|------|
| http://localhost | 系统首页（登录页） |
| http://localhost/swagger-ui.html | API接口文档 |
| http://localhost/druid | 数据库监控（账号：ruoyi / 123456） |

**登录账号：**
- 管理员：`admin` / `admin123`
- 测试员：`ry` / `admin123`

---

## 三、日常启动（已完成首次配置）

如果你之前已经成功启动过，日常启动只需要两步：

### 快速启动

```bash
# 1. 确保MySQL已启动（Navicat能连接即可）
# 2. 在项目根目录下执行：
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

### 停止项目

- 在运行java命令的终端窗口，按 `Ctrl + C` 即可停止

---

## 四、修改代码后如何重新启动

### 核心规则

| 修改了什么 | 是否需要重新编译 | 是否需要重启 |
|-----------|:---------------:|:-----------:|
| Java代码（.java文件） | ✅ 需要编译 | ✅ 需要重启 |
| MyBatis XML（Mapper.xml） | ✅ 需要编译 | ✅ 需要重启 |
| HTML模板（.html文件） | ✅ 需要编译 | ✅ 需要重启 |
| application.yml配置 | ✅ 需要编译 | ✅ 需要重启 |
| application-druid.yml配置 | ✅ 需要编译 | ✅ 需要重启 |
| SQL脚本 | ❌ 不需要编译 | ❌ 不需要重启（在Navicat中直接执行） |
| 静态资源（js/css/图片） | ✅ 需要编译 | ✅ 需要重启 |

> **为什么每次都要编译？** 因为我们是用`java -jar`运行打包后的JAR文件。代码修改后，JAR文件里的内容不会自动更新，必须重新打包。

### 标准操作流程（修改代码后）

```bash
# 第1步：停止正在运行的项目
# 在运行java的终端中按 Ctrl + C

# 第2步：重新编译打包
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
mvn clean package -DskipTests

# 第3步：重新启动
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

### 简化操作（一行命令完成编译+启动）

```bash
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
mvn clean package -DskipTests && java -jar ruoyi-admin\target\ruoyi-admin.jar
```

### SQL变更的操作（不需要编译）

如果只是修改了SQL（比如新增菜单、新增字典数据、新建业务表），不需要重新编译项目：

```
1. 在Navicat中打开 ruoyi 数据库
2. 右键 → 运行SQL文件 → 选择你的SQL脚本 → 执行
   或者：新建查询 → 粘贴SQL → 运行
3. 刷新浏览器页面即可看到效果（菜单变化需要重新登录）
```

---

## 五、新增业务模块时的完整操作步骤

以新增"项目管理"模块为例，完整操作流程如下：

### 第1步：执行SQL脚本（Navicat中操作）

```
1. 打开Navicat → 双击ruoyi数据库
2. 右键 → 运行SQL文件 → 按顺序执行：
   ① create_table.sql   → 创建业务表
   ② dict.sql           → 插入字典数据
   ③ menu.sql           → 插入菜单权限
3. 刷新数据库确认表已创建
```

### 第2步：放置Java代码文件

```
将生成的Java文件放到对应目录：

Domain/Mapper/Service文件 →
  ruoyi-system\src\main\java\com\ruoyi\{module}\

Mapper XML文件 →
  ruoyi-system\src\main\resources\mapper\{module}\

Controller文件 →
  ruoyi-admin\src\main\java\com\ruoyi\web\controller\{module}\

HTML模板文件 →
  ruoyi-admin\src\main\resources\templates\{module}\{business}\
```

### 第3步：编译并启动

```bash
# 停止旧进程（如果在运行中，按Ctrl+C）

# 重新编译
cd e:\beiruan_develop\RuoYi-master\RuoYi-master
mvn clean package -DskipTests

# 启动
java -jar ruoyi-admin\target\ruoyi-admin.jar
```

### 第4步：分配权限

```
1. 打开浏览器 http://localhost
2. 用admin账号登录
3. 进入 系统管理 → 角色管理 → 编辑"普通角色"
4. 在菜单权限中勾选新模块的所有菜单
5. 保存
6. 刷新页面，左侧菜单出现新模块
```

---

## 六、常见问题排查

### Q1：编译失败 - Maven下载依赖太慢

**配置阿里云Maven镜像：** 编辑 `C:\Users\你的用户名\.m2\settings.xml`（如果没有就创建）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>阿里云Maven镜像</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### Q2：启动失败 - Unknown database 'xxx'

数据库名称不匹配。检查：
1. Navicat中确认数据库名称的**精确拼写**
2. 修改 `application-druid.yml` 中 url 里的数据库名
3. 重新编译：`mvn clean package -DskipTests`

### Q3：启动失败 - Access denied for user 'root'

MySQL密码不正确。检查：
1. 在Navicat中测试连接，确认密码
2. 修改 `application-druid.yml` 中的 password
3. 重新编译

### Q4：端口80被占用

修改 `application.yml` 中的端口号：

```yaml
server:
  port: 8080    # 改成其他端口
```

然后重新编译，访问地址变为 http://localhost:8080

### Q5：编译提示 "终止批处理操作吗(Y/N)?"

这是因为上一次编译被中断。输入 `Y` 回车，然后重新执行编译命令。

### Q6：修改了代码但页面没变化

确认你执行了完整的3步操作：
1. 停止项目（Ctrl+C）
2. 重新编译（`mvn clean package -DskipTests`）
3. 重新启动（`java -jar ruoyi-admin\target\ruoyi-admin.jar`）

如果只是修改了HTML而没有重新编译，JAR包里的文件还是旧的。

### Q7：新增菜单后左侧看不到

1. 确认SQL执行成功（在Navicat中查询 `SELECT * FROM sys_menu WHERE menu_name='你的菜单名'`）
2. 确认角色已分配该菜单（角色管理 → 编辑角色 → 勾选新菜单）
3. **重新登录**（不是刷新页面，是退出再登录）

---

## 七、项目目录结构速查

```
e:\beiruan_develop\RuoYi-master\RuoYi-master\
│
├── sql\                           ← 数据库脚本
│   ├── ry_20250416.sql            ← 核心表+初始数据
│   └── quartz.sql                 ← 定时任务表
│
├── ruoyi-admin\                   ← Web入口模块（启动类在这里）
│   └── src\main\
│       ├── java\com\ruoyi\
│       │   ├── RuoYiApplication.java    ← 启动类
│       │   └── web\controller\          ← 所有Controller
│       │       ├── system\              ← 系统管理
│       │       ├── monitor\             ← 系统监控
│       │       ├── tool\                ← 系统工具
│       │       └── {你的模块}\          ← 新业务Controller放这里
│       └── resources\
│           ├── application.yml          ← 主配置（端口、日志等）
│           ├── application-druid.yml    ← 数据库配置
│           ├── templates\               ← HTML页面模板
│           │   ├── system\              ← 系统管理页面
│           │   └── {你的模块}\          ← 新业务页面放这里
│           └── static\                  ← js/css/图片
│
├── ruoyi-system\                  ← 业务逻辑模块
│   └── src\main\
│       ├── java\com\ruoyi\system\ ← Domain/Mapper/Service
│       │   ├── domain\            ← 实体类
│       │   ├── mapper\            ← Mapper接口
│       │   └── service\           ← Service接口+实现
│       └── resources\mapper\      ← MyBatis XML文件
│
├── ruoyi-common\                  ← 通用工具（不要修改）
│   └── BaseEntity, BaseController, 注解, 工具类
│
├── ruoyi-framework\               ← 框架核心（不要修改）
│   └── Shiro配置, MyBatis配置, 全局异常处理
│
├── ruoyi-generator\               ← 代码生成器
├── ruoyi-quartz\                  ← 定时任务
└── pom.xml                        ← 父工程Maven配置
```

---

## 八、操作命令速查表

| 场景 | 命令 |
|------|------|
| 首次编译 | `mvn clean install -DskipTests` |
| 修改代码后重新编译 | `mvn clean package -DskipTests` |
| 启动项目 | `java -jar ruoyi-admin\target\ruoyi-admin.jar` |
| 一行命令编译+启动 | `mvn clean package -DskipTests && java -jar ruoyi-admin\target\ruoyi-admin.jar` |
| 停止项目 | 在终端按 `Ctrl + C` |
| 检查Java版本 | `java -version` |
| 检查Maven版本 | `mvn -version` |

> **记住这个核心循环：修改代码 → Ctrl+C停止 → mvn编译 → java -jar启动 → 浏览器验证**
