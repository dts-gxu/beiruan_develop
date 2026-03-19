# 基于RuoYi的AI代码生成框架设计文档

> 版本：v1.0  
> 日期：2025-03-15  
> 基础框架：RuoYi v4.8.2（Spring Boot 4.x + Thymeleaf + Bootstrap）  
> 文档类型：框架设计

---

## 一、设计目标

### 1.1 核心目标

基于RuoYi v4.8.2框架，建立一套AI驱动的代码生成工作流，使AI能够：

1. 理解并遵循RuoYi框架的分层架构和编码规范
2. 生成与框架无缝集成的业务模块代码
3. 自动生成菜单权限、字典数据等框架配置SQL
4. 生成符合RuoYi风格的Thymeleaf前端页面

### 1.2 设计原则

- **框架优先** — 所有生成的代码必须100%遵循RuoYi框架规范
- **内置复用** — 优先使用RuoYi内置功能，不重复造轮子
- **增量扩展** — 新业务模块以增量方式加入，不修改框架核心代码
- **模板驱动** — 建立标准代码模板，AI基于模板进行定制化生成

---

## 二、RuoYi框架技术全景

### 2.1 技术栈概览

```
┌─────────────────────────────────────────────────────────────┐
│                       RuoYi v4.8.2                          │
├──────────────┬──────────────┬───────────────────────────────┤
│   基础框架    │   持久层     │         视图层                │
│ Spring Boot  │  MyBatis     │  Thymeleaf 模板引擎           │
│    4.0.3     │   4.0.1      │  Bootstrap 3.3.7              │
│ Apache Shiro │  Druid       │  jQuery + Ajax                │
│    2.1.0     │   1.2.28     │  bootstrap-table              │
│ JDK 17+     │  PageHelper  │  layer.js 弹窗                │
│              │   2.1.1      │  Hplus(H+) 后台主题           │
├──────────────┼──────────────┼───────────────────────────────┤
│   API文档    │   代码生成   │         其他                   │
│ springdoc    │  Velocity    │  EhCache 缓存                 │
│  openapi     │   2.3        │  Quartz 定时任务              │
│   3.0.2      │              │  POI Excel处理                │
│              │              │  Fastjson JSON解析            │
└──────────────┴──────────────┴───────────────────────────────┘
```

### 2.2 模块架构

```
ruoyi（父工程）
├── ruoyi-admin      — Web入口模块（Controller层、配置文件、前端模板）
│   ├── controller/  — 所有业务Controller
│   ├── resources/templates/  — Thymeleaf页面模板
│   └── resources/static/     — 静态资源（js/css/img）
├── ruoyi-framework  — 框架核心模块（安全、配置、AOP）
│   ├── aspectj/     — 切面（日志、数据权限、数据源切换）
│   ├── config/      — 全局配置（Shiro、MyBatis、Druid等）
│   ├── shiro/       — Shiro安全框架集成
│   └── web/         — Web层异常处理、拦截器
├── ruoyi-system     — 系统业务模块（系统管理的Domain/Mapper/Service）
│   ├── domain/      — 系统实体类
│   ├── mapper/      — MyBatis接口
│   └── service/     — 业务逻辑
├── ruoyi-common     — 通用工具模块
│   ├── annotation/  — 自定义注解（@Log、@DataScope、@Excel等）
│   ├── config/      — 通用配置
│   ├── constant/    — 常量定义
│   ├── core/        — 核心类（BaseEntity、BaseController、AjaxResult等）
│   ├── enums/       — 枚举类
│   ├── exception/   — 异常处理
│   ├── utils/       — 工具类（StringUtils、DateUtils、ShiroUtils等）
│   └── xss/         — XSS防护
├── ruoyi-generator  — 代码生成模块（Velocity模板引擎）
├── ruoyi-quartz     — 定时任务模块
└── sql/             — 数据库初始化脚本
```

### 2.3 RuoYi内置功能清单（不需要重复开发）

| 功能模块 | 说明 | AI生成时注意 |
|---------|------|-------------|
| 用户管理 | 用户CRUD、角色分配、部门关联 | 新模块只需关联用户ID |
| 部门管理 | 树结构组织机构 | 可复用数据权限体系 |
| 角色管理 | 角色-菜单-数据权限 | 新模块需生成对应菜单SQL |
| 菜单管理 | 三级菜单（目录→菜单→按钮） | **必须生成菜单INSERT SQL** |
| 字典管理 | 字典类型+字典数据 | 新模块的枚举字段应使用字典 |
| 参数管理 | 系统参数键值对 | 模块配置参数可存入sys_config |
| 日志管理 | 操作日志+登录日志 | Controller方法使用@Log注解 |
| 代码生成 | 基于数据库表生成代码 | 可作为AI生成的参照/补充 |
| 通知公告 | 系统通知 | 可直接复用 |
| 在线用户 | 用户会话监控 | 无需重复开发 |
| 定时任务 | Quartz任务调度 | 新定时任务实现AbstractQuartzJob |
| 服务监控 | 系统资源监控 | 无需重复开发 |
| 缓存监控 | EhCache监控 | 无需重复开发 |

---

## 三、AI代码生成框架设计

### 3.1 整体流程（适配RuoYi版）

```
┌──────────────────────────────────────────────────────────────┐
│ 第0步：RuoYi框架规范学习（AI上下文准备）                        │
│ 输入：框架源码+规范文档 → 输出：AI Prompt上下文模板             │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第一步：需求分析与RuoYi适配检查（人工+AI）                      │
│ 输入：需求文档 → 输出：结构化需求 + 框架复用分析                │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第二步：数据库与模块设计（AI生成）                              │
│ 输入：结构化需求 → 输出：DDL + 模块设计 + 菜单权限设计         │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第三步：设计评审（人工）                                        │
│ 输入：设计文档 → 输出：评审通过的设计                           │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第四步：代码生成（AI + RuoYi Generator辅助）                   │
│ 输入：DDL + 设计文档 → 输出：全套代码 + SQL脚本                │
│ 生成物：Domain → Mapper → MapperXML → Service                │
│         → ServiceImpl → Controller → HTML模板 → SQL          │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第五步：测试用例生成（AI）                                      │
│ 输入：代码 + 需求 → 输出：JUnit5测试 + MockMvc测试            │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第六步：代码评审 + 框架规范检查（人工+自动化）                  │
│ 输入：代码 → 输出：评审报告 + 修正后的代码                      │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 第七步：集成部署与测试（人工+AI辅助）                           │
│ 输入：代码 + SQL → 输出：运行的系统 + 测试报告                  │
└──────────────────────────────────────────────────────────────┘
                            ↓
                    ┌───────┴────────┐
                    ↓                ↓
              ✓ 通过            ✗ 失败 → 增量迭代（回到第二步）
                    ↓
                  交付完成
```

### 3.2 关键设计决策

| 决策点 | 决策 | 理由 |
|--------|------|------|
| 新业务模块放置位置 | 在ruoyi-admin中新建业务包 或 新建独立module | 简单业务放admin，复杂业务独立module |
| Domain命名空间 | `com.ruoyi.{模块}.domain` | 遵循RuoYi包命名规范 |
| 权限标识格式 | `{模块名}:{业务名}:{操作}` | 如`project:task:list` |
| 前端页面路径 | `templates/{模块名}/{业务名}/` | 如`templates/project/task/task.html` |
| 字典使用策略 | 有限枚举字段统一使用sys_dict | 便于管理和维护 |

---

## 四、代码生成规范（核心）

### 4.1 Domain实体类规范

```java
/**
 * 【业务名称】对象 【表名】
 * 
 * @author 【作者】
 */
public class 【ClassName】 extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long 【pkField】;

    /** 字段说明 */
    @Excel(name = "字段中文名")
    private String fieldName;

    /** 字典字段示例 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 日期字段示例 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date someDate;

    // getter/setter方法（不使用Lombok）
    public Long get【PkField】()
    {
        return 【pkField】;
    }

    public void set【PkField】(Long 【pkField】)
    {
        this.【pkField】 = 【pkField】;
    }

    // ... 其余字段的getter/setter

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("【pkField】", get【PkField】())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
```

**关键规则：**
- ✅ 必须继承 `BaseEntity`（普通表）或 `TreeEntity`（树形结构表）
- ✅ BaseEntity已包含：`searchValue`、`createBy`、`createTime`、`updateBy`、`updateTime`、`remark`、`params`
- ✅ 使用 `@Excel` 注解支持导入导出
- ✅ 使用Jakarta Validation注解（`@NotBlank`、`@Size`等）
- ✅ 使用 `@Xss` 注解防止XSS攻击
- ❌ 不使用Lombok
- ❌ 不使用 `@Data`、`@Builder` 等注解

### 4.2 Mapper接口规范

```java
/**
 * 【业务名称】Mapper接口
 * 
 * @author 【作者】
 */
public interface 【ClassName】Mapper
{
    /**
     * 查询【业务名称】列表
     * 
     * @param 【className】 【业务名称】
     * @return 【业务名称】集合
     */
    public List<【ClassName】> select【ClassName】List(【ClassName】 【className】);

    /**
     * 查询【业务名称】
     * 
     * @param 【pkField】 【业务名称】主键
     * @return 【业务名称】
     */
    public 【ClassName】 select【ClassName】By【PkField】(Long 【pkField】);

    /**
     * 新增【业务名称】
     * 
     * @param 【className】 【业务名称】
     * @return 结果
     */
    public int insert【ClassName】(【ClassName】 【className】);

    /**
     * 修改【业务名称】
     * 
     * @param 【className】 【业务名称】
     * @return 结果
     */
    public int update【ClassName】(【ClassName】 【className】);

    /**
     * 删除【业务名称】
     * 
     * @param 【pkField】 【业务名称】主键
     * @return 结果
     */
    public int delete【ClassName】By【PkField】(Long 【pkField】);

    /**
     * 批量删除【业务名称】
     * 
     * @param 【pkField】s 需要删除的数据主键集合
     * @return 结果
     */
    public int delete【ClassName】By【PkField】s(String[] 【pkField】s);
}
```

### 4.3 Mapper XML规范

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.【module】.mapper.【ClassName】Mapper">
    
    <resultMap type="【ClassName】" id="【ClassName】Result">
        <result property="【pkField】" column="【pk_column】" />
        <result property="fieldName" column="field_name" />
        <!-- 其他字段映射 -->
        <result property="createBy" column="create_by" />
        <result property="createTime" column="create_time" />
        <result property="updateBy" column="update_by" />
        <result property="updateTime" column="update_time" />
        <result property="remark" column="remark" />
    </resultMap>

    <sql id="select【ClassName】Vo">
        select 【pk_column】, field_name, ..., create_by, create_time, 
               update_by, update_time, remark 
        from 【table_name】
    </sql>

    <select id="select【ClassName】List" parameterType="【ClassName】" resultMap="【ClassName】Result">
        <include refid="select【ClassName】Vo"/>
        <where>  
            <if test="fieldName != null and fieldName != ''">
                AND field_name like concat('%', #{fieldName}, '%')
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
            <if test="params.beginTime != null and params.beginTime != ''">
                AND date_format(create_time,'%Y%m%d') &gt;= date_format(#{params.beginTime},'%Y%m%d')
            </if>
            <if test="params.endTime != null and params.endTime != ''">
                AND date_format(create_time,'%Y%m%d') &lt;= date_format(#{params.endTime},'%Y%m%d')
            </if>
        </where>
        <!-- 数据权限过滤 -->
        <!-- ${params.dataScope} -->
    </select>

    <select id="select【ClassName】By【PkField】" parameterType="Long" resultMap="【ClassName】Result">
        <include refid="select【ClassName】Vo"/>
        where 【pk_column】 = #{【pkField】}
    </select>
        
    <insert id="insert【ClassName】" parameterType="【ClassName】" useGeneratedKeys="true" keyProperty="【pkField】">
        insert into 【table_name】
        <trim prefix="(" suffix=")" suffixOverrides=",">
            <if test="fieldName != null and fieldName != ''">field_name,</if>
            <if test="status != null and status != ''">status,</if>
            <if test="createBy != null and createBy != ''">create_by,</if>
            create_time,
            <if test="remark != null and remark != ''">remark,</if>
        </trim>
        <trim prefix="values (" suffix=")" suffixOverrides=",">
            <if test="fieldName != null and fieldName != ''">#{fieldName},</if>
            <if test="status != null and status != ''">#{status},</if>
            <if test="createBy != null and createBy != ''">#{createBy},</if>
            sysdate(),
            <if test="remark != null and remark != ''">#{remark},</if>
        </trim>
    </insert>

    <update id="update【ClassName】" parameterType="【ClassName】">
        update 【table_name】
        <trim prefix="SET" suffixOverrides=",">
            <if test="fieldName != null and fieldName != ''">field_name = #{fieldName},</if>
            <if test="status != null and status != ''">status = #{status},</if>
            <if test="updateBy != null and updateBy != ''">update_by = #{updateBy},</if>
            update_time = sysdate(),
            <if test="remark != null">remark = #{remark},</if>
        </trim>
        where 【pk_column】 = #{【pkField】}
    </update>

    <delete id="delete【ClassName】By【PkField】" parameterType="Long">
        delete from 【table_name】 where 【pk_column】 = #{【pkField】}
    </delete>

    <delete id="delete【ClassName】By【PkField】s" parameterType="String">
        delete from 【table_name】 where 【pk_column】 in 
        <foreach item="【pkField】" collection="array" open="(" separator="," close=")">
            #{【pkField】}
        </foreach>
    </delete>
</mapper>
```

### 4.4 Service接口规范

```java
/**
 * 【业务名称】Service接口
 * 
 * @author 【作者】
 */
public interface I【ClassName】Service
{
    /**
     * 查询【业务名称】
     */
    public 【ClassName】 select【ClassName】By【PkField】(Long 【pkField】);

    /**
     * 查询【业务名称】列表
     */
    public List<【ClassName】> select【ClassName】List(【ClassName】 【className】);

    /**
     * 新增【业务名称】
     */
    public int insert【ClassName】(【ClassName】 【className】);

    /**
     * 修改【业务名称】
     */
    public int update【ClassName】(【ClassName】 【className】);

    /**
     * 批量删除【业务名称】
     */
    public int delete【ClassName】ByIds(String ids);

    /**
     * 删除【业务名称】信息
     */
    public int delete【ClassName】By【PkField】(Long 【pkField】);
}
```

### 4.5 Service实现类规范

```java
/**
 * 【业务名称】Service业务层处理
 * 
 * @author 【作者】
 */
@Service
public class 【ClassName】ServiceImpl implements I【ClassName】Service
{
    @Autowired
    private 【ClassName】Mapper 【className】Mapper;

    /**
     * 查询【业务名称】
     */
    @Override
    public 【ClassName】 select【ClassName】By【PkField】(Long 【pkField】)
    {
        return 【className】Mapper.select【ClassName】By【PkField】(【pkField】);
    }

    /**
     * 查询【业务名称】列表
     */
    @Override
    // @DataScope(deptAlias = "d", userAlias = "u")  // 按需启用数据权限
    public List<【ClassName】> select【ClassName】List(【ClassName】 【className】)
    {
        return 【className】Mapper.select【ClassName】List(【className】);
    }

    /**
     * 新增【业务名称】
     */
    @Override
    public int insert【ClassName】(【ClassName】 【className】)
    {
        【className】.setCreateTime(DateUtils.getNowDate());
        return 【className】Mapper.insert【ClassName】(【className】);
    }

    /**
     * 修改【业务名称】
     */
    @Override
    public int update【ClassName】(【ClassName】 【className】)
    {
        【className】.setUpdateTime(DateUtils.getNowDate());
        return 【className】Mapper.update【ClassName】(【className】);
    }

    /**
     * 批量删除【业务名称】
     */
    @Override
    public int delete【ClassName】ByIds(String ids)
    {
        return 【className】Mapper.delete【ClassName】By【PkField】s(Convert.toStrArray(ids));
    }

    /**
     * 删除【业务名称】信息
     */
    @Override
    public int delete【ClassName】By【PkField】(Long 【pkField】)
    {
        return 【className】Mapper.delete【ClassName】By【PkField】(【pkField】);
    }
}
```

### 4.6 Controller规范

```java
/**
 * 【业务名称】Controller
 * 
 * @author 【作者】
 */
@Controller
@RequestMapping("/【module】/【business】")
public class 【ClassName】Controller extends BaseController
{
    private String prefix = "【module】/【business】";

    @Autowired
    private I【ClassName】Service 【className】Service;

    @RequiresPermissions("【module】:【business】:view")
    @GetMapping()
    public String 【business】()
    {
        return prefix + "/【business】";
    }

    /**
     * 查询【业务名称】列表
     */
    @RequiresPermissions("【module】:【business】:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(【ClassName】 【className】)
    {
        startPage();
        List<【ClassName】> list = 【className】Service.select【ClassName】List(【className】);
        return getDataTable(list);
    }

    /**
     * 导出【业务名称】列表
     */
    @RequiresPermissions("【module】:【business】:export")
    @Log(title = "【业务名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(【ClassName】 【className】)
    {
        List<【ClassName】> list = 【className】Service.select【ClassName】List(【className】);
        ExcelUtil<【ClassName】> util = new ExcelUtil<【ClassName】>(【ClassName】.class);
        return util.exportExcel(list, "【业务名称】数据");
    }

    /**
     * 新增【业务名称】
     */
    @RequiresPermissions("【module】:【business】:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存【业务名称】
     */
    @RequiresPermissions("【module】:【business】:add")
    @Log(title = "【业务名称】", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated 【ClassName】 【className】)
    {
        【className】.setCreateBy(getLoginName());
        return toAjax(【className】Service.insert【ClassName】(【className】));
    }

    /**
     * 修改【业务名称】
     */
    @RequiresPermissions("【module】:【business】:edit")
    @GetMapping("/edit/{【pkField】}")
    public String edit(@PathVariable("【pkField】") Long 【pkField】, ModelMap mmap)
    {
        【ClassName】 【className】 = 【className】Service.select【ClassName】By【PkField】(【pkField】);
        mmap.put("【className】", 【className】);
        return prefix + "/edit";
    }

    /**
     * 修改保存【业务名称】
     */
    @RequiresPermissions("【module】:【business】:edit")
    @Log(title = "【业务名称】", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated 【ClassName】 【className】)
    {
        【className】.setUpdateBy(getLoginName());
        return toAjax(【className】Service.update【ClassName】(【className】));
    }

    /**
     * 删除【业务名称】
     */
    @RequiresPermissions("【module】:【business】:remove")
    @Log(title = "【业务名称】", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(【className】Service.delete【ClassName】ByIds(ids));
    }
}
```

**关键规则：**
- ✅ 使用 `@Controller`（非`@RestController`）
- ✅ 返回JSON的方法加 `@ResponseBody`
- ✅ 页面跳转返回Thymeleaf视图路径字符串
- ✅ 每个方法添加 `@RequiresPermissions` 权限注解
- ✅ 增删改操作添加 `@Log` 日志注解
- ✅ 列表查询调用 `startPage()` 开启分页
- ✅ 新增/修改时设置 `createBy`/`updateBy`

### 4.7 Thymeleaf列表页面规范

```html
<!DOCTYPE html>
<html lang="zh" xmlns:th="http://www.thymeleaf.org" 
      xmlns:shiro="http://www.pollix.at/thymeleaf/shiro">
<head>
    <th:block th:include="include :: header('【业务名称】列表')" />
</head>
<body class="gray-bg">
    <div class="container-div">
        <!-- 搜索条件区域 -->
        <div class="row">
            <div class="col-sm-12 search-collapse">
                <form id="formId">
                    <div class="select-list">
                        <ul>
                            <li>
                                <label>字段名称：</label>
                                <input type="text" name="fieldName"/>
                            </li>
                            <li>
                                <label>状态：</label>
                                <select name="status" th:with="type=${@dict.getType('sys_normal_disable')}">
                                    <option value="">所有</option>
                                    <option th:each="dict : ${type}" th:text="${dict.dictLabel}" 
                                            th:value="${dict.dictValue}"></option>
                                </select>
                            </li>
                            <li>
                                <label>创建时间：</label>
                                <input type="text" class="time-input" placeholder="开始时间" 
                                       name="params[beginTime]"/>
                                <span>-</span>
                                <input type="text" class="time-input" placeholder="结束时间" 
                                       name="params[endTime]"/>
                            </li>
                            <li>
                                <a class="btn btn-primary btn-rounded btn-sm" 
                                   onclick="$.table.search()"><i class="fa fa-search"></i>&nbsp;搜索</a>
                                <a class="btn btn-warning btn-rounded btn-sm" 
                                   onclick="$.form.reset()"><i class="fa fa-refresh"></i>&nbsp;重置</a>
                            </li>
                        </ul>
                    </div>
                </form>
            </div>
        </div>

        <!-- 工具栏按钮区域 -->
        <div class="btn-group-sm" id="toolbar" role="group">
            <a class="btn btn-success" onclick="$.operate.add()" 
               shiro:hasPermission="【module】:【business】:add">
                <i class="fa fa-plus"></i> 添加
            </a>
            <a class="btn btn-primary single disabled" onclick="$.operate.edit()" 
               shiro:hasPermission="【module】:【business】:edit">
                <i class="fa fa-edit"></i> 修改
            </a>
            <a class="btn btn-danger multiple disabled" onclick="$.operate.removeAll()" 
               shiro:hasPermission="【module】:【business】:remove">
                <i class="fa fa-remove"></i> 删除
            </a>
            <a class="btn btn-warning" onclick="$.table.exportExcel()" 
               shiro:hasPermission="【module】:【business】:export">
                <i class="fa fa-download"></i> 导出
            </a>
        </div>

        <!-- 数据表格区域 -->
        <div class="col-sm-12 select-table table-striped">
            <table id="bootstrap-table"></table>
        </div>
    </div>
    
    <th:block th:include="include :: footer" />
    <script th:inline="javascript">
        var editFlag = [[${@permission.hasPermi('【module】:【business】:edit')}]];
        var removeFlag = [[${@permission.hasPermi('【module】:【business】:remove')}]];
        // 字典数据（如需要）
        // var statusDatas = [[${@dict.getType('sys_normal_disable')}]];
        var prefix = ctx + "【module】/【business】";

        $(function() {
            var options = {
                url: prefix + "/list",
                createUrl: prefix + "/add",
                updateUrl: prefix + "/edit/{id}",
                removeUrl: prefix + "/remove",
                exportUrl: prefix + "/export",
                modalName: "【业务名称】",
                columns: [{
                    checkbox: true
                },
                {
                    field: '【pkField】',
                    title: '序号',
                    visible: false
                },
                {
                    field: 'fieldName',
                    title: '字段名称'
                },
                {
                    field: 'status',
                    title: '状态',
                    formatter: function(value, row, index) {
                        return $.table.selectDictLabel(statusDatas, value);
                    }
                },
                {
                    field: 'createTime',
                    title: '创建时间',
                    sortable: true
                },
                {
                    title: '操作',
                    align: 'center',
                    formatter: function(value, row, index) {
                        var actions = [];
                        actions.push('<a class="btn btn-success btn-xs ' + editFlag + '" href="javascript:void(0)" onclick="$.operate.edit(\'' + row.【pkField】 + '\')"><i class="fa fa-edit"></i>编辑</a> ');
                        actions.push('<a class="btn btn-danger btn-xs ' + removeFlag + '" href="javascript:void(0)" onclick="$.operate.remove(\'' + row.【pkField】 + '\')"><i class="fa fa-remove"></i>删除</a>');
                        return actions.join('');
                    }
                }]
            };
            $.table.init(options);
        });
    </script>
</body>
</html>
```

### 4.8 菜单权限SQL规范

每个新业务模块必须生成完整的三级菜单SQL：

```sql
-- ----------------------------
-- 【业务名称】菜单SQL
-- ----------------------------

-- 一级菜单（目录） - 如果是新模块目录
-- INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon)
-- VALUES ('【模块中文名】', '0', '10', '#', 'M', '0', '', 'fa fa-gear');

-- 二级菜单（菜单）
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('【业务名称】', @parentId, '1', '/【module】/【business】', 'C', '0', '【module】:【business】:view', '#', 'admin', sysdate());

-- 获取刚插入的菜单ID
SELECT @menuId := LAST_INSERT_ID();

-- 三级菜单（按钮权限）
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
VALUES 
('【业务名称】查询', @menuId, '1', '#', 'F', '0', '【module】:【business】:list', '#', 'admin', sysdate()),
('【业务名称】新增', @menuId, '2', '#', 'F', '0', '【module】:【business】:add', '#', 'admin', sysdate()),
('【业务名称】修改', @menuId, '3', '#', 'F', '0', '【module】:【business】:edit', '#', 'admin', sysdate()),
('【业务名称】删除', @menuId, '4', '#', 'F', '0', '【module】:【business】:remove', '#', 'admin', sysdate()),
('【业务名称】导出', @menuId, '5', '#', 'F', '0', '【module】:【business】:export', '#', 'admin', sysdate());
```

### 4.9 数据库表设计规范

```sql
-- ----------------------------
-- 【业务名称】表
-- ----------------------------
DROP TABLE IF EXISTS 【table_name】;
CREATE TABLE 【table_name】 (
    【pk_column】      BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    field_name        VARCHAR(100)  DEFAULT ''               COMMENT '字段说明',
    status            CHAR(1)       DEFAULT '0'              COMMENT '状态（0正常 1停用）',
    del_flag          CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0存在 2删除）',
    create_by         VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time       DATETIME                               COMMENT '创建时间',
    update_by         VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time       DATETIME                               COMMENT '更新时间',
    remark            VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (【pk_column】)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='【业务名称】表';
```

**必须字段：**
| 字段 | 类型 | 说明 | 来源 |
|------|------|------|------|
| create_by | VARCHAR(64) | 创建者 | BaseEntity |
| create_time | DATETIME | 创建时间 | BaseEntity |
| update_by | VARCHAR(64) | 更新者 | BaseEntity |
| update_time | DATETIME | 更新时间 | BaseEntity |
| remark | VARCHAR(500) | 备注 | BaseEntity |

**可选公共字段：**
| 字段 | 类型 | 说明 | 适用场景 |
|------|------|------|---------|
| del_flag | CHAR(1) | 逻辑删除标志 | 需要软删除的表 |
| status | CHAR(1) | 状态 | 需要启用/停用的数据 |
| order_num | INT(4) | 排序号 | 需要排序的数据 |

**树形表额外字段（继承TreeEntity）：**
| 字段 | 类型 | 说明 |
|------|------|------|
| parent_id | BIGINT(20) | 父级ID |
| ancestors | VARCHAR(500) | 祖级列表 |
| order_num | INT(4) | 排序号 |

---

## 五、AI Prompt上下文模板

### 5.1 框架规范Prompt（每次AI生成前注入）

```
你是一个基于RuoYi v4.8.2框架的Java开发专家。请严格遵循以下规范：

【框架版本】
- RuoYi v4.8.2, Spring Boot 4.0.3, JDK 17+
- 前后端不分离，使用Thymeleaf模板引擎 + Bootstrap 3.3.7
- 权限框架：Apache Shiro 2.1.0
- 持久层：MyBatis + Druid连接池
- JSON：Fastjson
- API文档：springdoc-openapi 3.0.2

【分层架构】
- Domain实体类继承BaseEntity（含createBy/createTime/updateBy/updateTime/remark）
- Mapper接口 + XML文件（动态SQL）
- Service接口 + ServiceImpl实现类
- Controller继承BaseController，使用@Controller（非@RestController）

【编码规范】
- 不使用Lombok，手写getter/setter
- 权限注解：@RequiresPermissions("模块:业务:操作")
- 日志注解：@Log(title="xxx", businessType=BusinessType.INSERT)
- 导出注解：@Excel(name="xxx")
- XSS防护：@Xss注解
- 分页：Controller调用startPage()，返回getDataTable(list)
- 响应格式：AjaxResult（增删改）、TableDataInfo（列表查询）
- 工具类：使用RuoYi内置（StringUtils、DateUtils、Convert、ShiroUtils等）

【前端规范】
- Thymeleaf HTML模板，引用include :: header和include :: footer
- bootstrap-table表格组件
- layer.js弹窗
- jQuery Ajax通信
- shiro:hasPermission标签控制按钮权限

【SQL规范】
- 表名：snake_case，建议加业务前缀
- 必须字段：create_by、create_time、update_by、update_time、remark
- 必须生成sys_menu菜单SQL（目录→菜单→按钮五权限）
- 字典字段需插入sys_dict_type和sys_dict_data
```

### 5.2 需求分析Prompt模板

```
请分析以下需求文档，并输出结构化JSON：

{
  "module": {
    "name": "模块英文名（小写）",
    "cnName": "模块中文名",
    "packageName": "com.ruoyi.{module}",
    "parentMenuId": "父菜单ID"
  },
  "entities": [
    {
      "tableName": "表名",
      "className": "类名",
      "businessName": "业务名",
      "cnName": "中文名",
      "extendsClass": "BaseEntity|TreeEntity",
      "tplCategory": "crud|tree|sub",
      "fields": [
        {
          "columnName": "字段名",
          "javaField": "Java属性名",
          "javaType": "String|Long|Integer|Date|BigDecimal",
          "columnType": "varchar(100)|bigint(20)|...",
          "isPk": true/false,
          "isRequired": true/false,
          "isInsert": true/false,
          "isEdit": true/false,
          "isList": true/false,
          "isQuery": true/false,
          "queryType": "EQ|NE|GT|GTE|LT|LTE|LIKE|BETWEEN",
          "htmlType": "input|textarea|select|checkbox|radio|datetime|upload",
          "dictType": "字典类型（如有）",
          "comment": "字段说明"
        }
      ]
    }
  ],
  "dictionaries": [
    {
      "dictType": "字典类型编码",
      "dictName": "字典类型名称",
      "values": [
        {"dictValue": "0", "dictLabel": "标签", "dictSort": 0}
      ]
    }
  ],
  "reusableFeatures": ["可复用的RuoYi内置功能列表"],
  "permissions": [
    {
      "entity": "实体名",
      "dataScope": "全部|自定义|部门|部门及以下|仅本人"
    }
  ]
}
```

---

## 六、代码生成质量检查清单

### 6.1 框架规范检查

| # | 检查项 | 必须 | 说明 |
|---|--------|------|------|
| 1 | Domain是否继承BaseEntity/TreeEntity | ✅ | |
| 2 | Domain是否手写getter/setter（无Lombok） | ✅ | |
| 3 | Domain是否包含@Excel注解 | ✅ | 支持导入导出 |
| 4 | Domain是否包含toString方法 | ✅ | 使用ToStringBuilder |
| 5 | Controller是否继承BaseController | ✅ | |
| 6 | Controller是否使用@Controller（非@RestController） | ✅ | |
| 7 | Controller是否有@RequiresPermissions注解 | ✅ | 每个方法都需要 |
| 8 | 增删改方法是否有@Log注解 | ✅ | |
| 9 | 列表查询是否调用startPage() | ✅ | |
| 10 | Service是否使用@Service注解 | ✅ | |
| 11 | Mapper XML是否包含resultMap | ✅ | |
| 12 | Mapper XML是否使用动态SQL | ✅ | `<if test="...">` |

### 6.2 SQL检查

| # | 检查项 | 必须 | 说明 |
|---|--------|------|------|
| 1 | 表是否包含create_by/create_time/update_by/update_time | ✅ | BaseEntity要求 |
| 2 | 表名是否使用snake_case | ✅ | |
| 3 | 是否生成sys_menu插入SQL | ✅ | 三级菜单 |
| 4 | 按钮权限是否包含查询/新增/修改/删除/导出 | ✅ | 5个标准按钮 |
| 5 | 字典字段是否生成sys_dict_type和sys_dict_data SQL | ✅ | |
| 6 | 是否有remark字段 | ⚠️ | BaseEntity包含 |

### 6.3 前端页面检查

| # | 检查项 | 必须 | 说明 |
|---|--------|------|------|
| 1 | 是否引用include :: header和footer | ✅ | |
| 2 | 是否使用bootstrap-table初始化表格 | ✅ | |
| 3 | 是否使用shiro:hasPermission控制按钮 | ✅ | |
| 4 | 字典字段是否使用th:with获取字典数据 | ✅ | |
| 5 | 搜索表单是否正确配置 | ✅ | |
| 6 | 新增/编辑页面表单验证是否完整 | ✅ | |

---

## 七、新业务模块集成步骤

当AI完成代码生成后，按以下步骤集成到RuoYi框架：

### 方式一：在ruoyi-admin模块内扩展（简单业务）

```
1. 将Domain类放入     ruoyi-system/src/main/java/com/ruoyi/{module}/domain/
2. 将Mapper接口放入   ruoyi-system/src/main/java/com/ruoyi/{module}/mapper/
3. 将Mapper XML放入   ruoyi-system/src/main/resources/mapper/{module}/
4. 将Service放入      ruoyi-system/src/main/java/com/ruoyi/{module}/service/
5. 将ServiceImpl放入  ruoyi-system/src/main/java/com/ruoyi/{module}/service/impl/
6. 将Controller放入   ruoyi-admin/src/main/java/com/ruoyi/web/controller/{module}/
7. 将HTML模板放入     ruoyi-admin/src/main/resources/templates/{module}/{business}/
8. 执行菜单SQL和字典SQL
9. 重新编译并启动应用
```

### 方式二：独立Maven模块（复杂业务）

```
1. 创建新模块 ruoyi-{module}（参照ruoyi-system结构）
2. 在父pom.xml中添加module声明和依赖管理
3. 在ruoyi-admin的pom.xml中添加对新模块的依赖
4. 在MyBatisConfig中配置新模块的Mapper扫描路径
5. 按方式一放置Controller和HTML模板
6. 执行SQL脚本
7. 重新编译并启动应用
```

---

## 八、推荐开源工具链

| 阶段 | 工具 | 用途 | 与RuoYi集成度 |
|------|------|------|-------------|
| 需求分析 | LangChain / Docling | 文档解析与AI处理 | 独立使用 |
| 设计 | Mermaid / PlantUML | 图表生成 | 嵌入Markdown |
| 代码生成 | RuoYi Generator（内置） | 基础CRUD代码生成 | ⭐⭐⭐⭐⭐ 原生支持 |
| 代码生成 | AI（GPT/Claude等） | 复杂业务逻辑生成 | ⭐⭐⭐ 需要Prompt工程 |
| 模板引擎 | Velocity 2.3 | 代码模板渲染 | ⭐⭐⭐⭐⭐ 框架内置 |
| 代码检查 | Checkstyle | 代码格式检查 | ⭐⭐⭐ Maven插件 |
| 代码质量 | SonarQube | 静态分析 | ⭐⭐⭐ 独立部署 |
| API文档 | springdoc-openapi 3.0.2 | 接口文档 | ⭐⭐⭐⭐⭐ 已集成 |
| 单元测试 | JUnit5 + Mockito | Java单元测试 | ⭐⭐⭐⭐ 标准集成 |
| 集成测试 | MockMvc | Controller层测试 | ⭐⭐⭐⭐ Spring原生 |
| UI测试 | Selenium / Playwright | 端到端页面测试 | ⭐⭐⭐ 适合SSR页面 |
| 性能测试 | JMeter | 压力测试 | ⭐⭐⭐ 独立使用 |
| CI/CD | Jenkins / GitLab CI | 持续集成 | ⭐⭐⭐ 标准Maven项目 |
| 容器化 | Docker | 部署 | ⭐⭐⭐ 需编写Dockerfile |

---

## 九、示例：完整业务模块生成产物清单

以"项目任务管理"为例，AI应生成以下文件：

```
生成产物/
├── sql/
│   ├── create_table.sql          -- 建表DDL
│   ├── menu.sql                  -- 菜单权限SQL
│   └── dict.sql                  -- 字典数据SQL（如需要）
├── java/
│   ├── domain/
│   │   └── ProjectTask.java      -- 实体类（extends BaseEntity）
│   ├── mapper/
│   │   └── ProjectTaskMapper.java -- Mapper接口
│   ├── service/
│   │   ├── IProjectTaskService.java    -- Service接口
│   │   └── impl/
│   │       └── ProjectTaskServiceImpl.java -- Service实现
│   └── controller/
│       └── ProjectTaskController.java  -- Controller
├── xml/
│   └── ProjectTaskMapper.xml     -- MyBatis XML
├── html/
│   ├── task.html                 -- 列表页
│   ├── add.html                  -- 新增页
│   └── edit.html                 -- 编辑页
└── test/
    ├── ProjectTaskServiceTest.java    -- Service单元测试
    └── ProjectTaskControllerTest.java -- Controller集成测试
```

---

## 十、Dify平台深度集成设计（核心）

### 10.1 集成架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        用户浏览器（RuoYi前端）                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Dify配置管理  │  │ Dify应用管理  │  │  AI对话（选择App+历史）   │  │
│  │ config.html  │  │  app.html    │  │      chat.html           │  │
│  └──────┬───────┘  └──────┬───────┘  └────────────┬─────────────┘  │
└─────────┼─────────────────┼───────────────────────┼────────────────┘
          │ Ajax             │ Ajax                   │ Ajax
          ▼                  ▼                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     RuoYi后端（Spring Boot）                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────────┐  │
│  │AiDifyConfigCtrl │  │AiDifyAppCtrl    │  │AiAssistantCtrl    │  │
│  │(API密钥CRUD)    │  │(应用CRUD+同步)  │  │(对话+代码生成)    │  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬──────────┘  │
│           │                    │                     │             │
│  ┌────────▼────────────────────▼─────────────────────▼──────────┐  │
│  │              Service层（业务逻辑 + Dify API调用）              │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌─────────────────────┐  │  │
│  │  │DifyApiClient │ │AiDifyAppSvc  │ │AiChatSessionSvc     │  │  │
│  │  │(HTTP封装)    │ │(应用管理)    │ │(会话+消息持久化)    │  │  │
│  │  └──────┬───────┘ └──────────────┘ └─────────────────────┘  │  │
│  └─────────┼───────────────────────────────────────────────────┘  │
│            │                                                       │
│  ┌─────────▼───────────────────────────────────────────────────┐  │
│  │              Mapper层 + MySQL数据库                           │  │
│  │  ai_dify_config | ai_dify_app | ai_chat_conversation        │  │
│  │                               | ai_chat_message              │  │
│  └─────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP REST API
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Dify平台（Docker部署）                          │
│  ┌──────────┐  ┌───────────┐  ┌────────────┐  ┌────────────────┐  │
│  │Chat API  │  │Workflow   │  │Knowledge   │  │Completion API  │  │
│  │/chat-msg │  │/workflows │  │/datasets   │  │/completion-msg │  │
│  └──────────┘  └───────────┘  └────────────┘  └────────────────┘  │
│  访问地址: http://localhost:8180/v1                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### 10.2 数据库设计

#### 10.2.1 ai_dify_config（Dify API连接配置表）

存储Dify平台的连接信息，支持多环境（开发/测试/生产）。

```sql
CREATE TABLE ai_dify_config (
    config_id       BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '配置ID',
    config_name     VARCHAR(100)  NOT NULL                 COMMENT '配置名称（如：开发环境Dify）',
    base_url        VARCHAR(500)  NOT NULL                 COMMENT 'Dify API地址（如：http://localhost:8180/v1）',
    api_key         VARCHAR(500)  NOT NULL                 COMMENT 'Dify API密钥（加密存储）',
    config_type     CHAR(1)       DEFAULT '0'              COMMENT '配置类型（0=Chat应用 1=Workflow应用 2=Completion应用）',
    is_default      CHAR(1)       DEFAULT 'N'              COMMENT '是否默认配置（Y=是 N=否）',
    status          CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=停用）',
    last_test_time  DATETIME                               COMMENT '最后连接测试时间',
    last_test_result CHAR(1)                               COMMENT '最后测试结果（0=成功 1=失败）',
    del_flag        CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by       VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME                               COMMENT '创建时间',
    update_by       VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time     DATETIME                               COMMENT '更新时间',
    remark          VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify API连接配置表';
```

#### 10.2.2 ai_dify_app（Dify应用管理表）

管理在Dify平台上创建的应用（Chat/Workflow/Completion），每个应用有独立的API Key。

```sql
CREATE TABLE ai_dify_app (
    app_id          BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '应用ID',
    config_id       BIGINT(20)    NOT NULL                 COMMENT '关联的Dify配置ID',
    app_name        VARCHAR(200)  NOT NULL                 COMMENT '应用名称',
    app_type        CHAR(1)       DEFAULT '0'              COMMENT '应用类型（0=Chat 1=Workflow 2=Completion 3=Agent）',
    app_api_key     VARCHAR(500)  NOT NULL                 COMMENT '应用级API密钥（app-xxx）',
    dify_app_id     VARCHAR(100)  DEFAULT ''               COMMENT 'Dify平台上的应用ID',
    workflow_id     VARCHAR(100)  DEFAULT ''               COMMENT '工作流ID（Workflow类型专用）',
    model_name      VARCHAR(100)  DEFAULT ''               COMMENT '使用的模型名称',
    description     VARCHAR(1000) DEFAULT ''               COMMENT '应用描述',
    icon            VARCHAR(200)  DEFAULT ''               COMMENT '应用图标URL',
    sort_order      INT(4)        DEFAULT 0                COMMENT '排序号',
    status          CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=停用）',
    del_flag        CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by       VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME                               COMMENT '创建时间',
    update_by       VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time     DATETIME                               COMMENT '更新时间',
    remark          VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (app_id),
    KEY idx_config_id (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify应用管理表';
```

#### 10.2.3 ai_chat_conversation（AI对话会话表）

持久化用户的AI对话会话，支持会话历史回顾。

```sql
CREATE TABLE ai_chat_conversation (
    conversation_id     BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    app_id              BIGINT(20)    NOT NULL                 COMMENT '关联的Dify应用ID',
    user_id             BIGINT(20)    NOT NULL                 COMMENT '用户ID',
    dify_conversation_id VARCHAR(100) DEFAULT ''               COMMENT 'Dify平台返回的会话ID',
    title               VARCHAR(500)  DEFAULT '新对话'         COMMENT '会话标题（自动/手动）',
    conversation_type   CHAR(1)       DEFAULT '0'              COMMENT '会话类型（0=普通对话 1=需求分析 2=代码生成 3=代码评审）',
    status              CHAR(1)       DEFAULT '0'              COMMENT '状态（0=进行中 1=已完成 2=已归档）',
    message_count       INT(8)        DEFAULT 0                COMMENT '消息数量',
    last_message_time   DATETIME                               COMMENT '最后消息时间',
    del_flag            CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by           VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time         DATETIME                               COMMENT '创建时间',
    update_by           VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time         DATETIME                               COMMENT '更新时间',
    remark              VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (conversation_id),
    KEY idx_app_id (app_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话会话表';
```

#### 10.2.4 ai_chat_message（AI对话消息表）

持久化每一条对话消息，用于历史回顾和上下文管理。

```sql
CREATE TABLE ai_chat_message (
    message_id      BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
    conversation_id BIGINT(20)    NOT NULL                 COMMENT '关联的会话ID',
    role            CHAR(1)       DEFAULT '0'              COMMENT '角色（0=用户 1=AI助手 2=系统）',
    content         LONGTEXT                               COMMENT '消息内容',
    content_type    CHAR(1)       DEFAULT '0'              COMMENT '内容类型（0=文本 1=代码 2=图片 3=文件）',
    tokens_used     INT(8)        DEFAULT 0                COMMENT 'Token消耗数',
    dify_message_id VARCHAR(100)  DEFAULT ''               COMMENT 'Dify平台返回的消息ID',
    model_name      VARCHAR(100)  DEFAULT ''               COMMENT '使用的模型名称',
    cost_time       INT(8)        DEFAULT 0                COMMENT '响应耗时（毫秒）',
    status          CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=失败 2=已撤回）',
    create_time     DATETIME                               COMMENT '创建时间',
    PRIMARY KEY (message_id),
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话消息表';
```

### 10.3 Dify API对接规范

#### 10.3.1 Dify REST API端点清单

| API | 方法 | 端点 | 说明 | RuoYi对接 |
|-----|------|------|------|-----------|
| Chat | POST | `/v1/chat-messages` | 发送对话消息 | AiChatSessionService |
| Workflow | POST | `/v1/workflows/run` | 运行工作流 | DifyApiClient |
| Completion | POST | `/v1/completion-messages` | 单次文本生成 | DifyApiClient |
| 停止响应 | POST | `/v1/chat-messages/:task_id/stop` | 停止流式响应 | AiChatSessionService |
| 获取会话列表 | GET | `/v1/conversations` | 查询会话列表 | AiChatSessionService |
| 删除会话 | DELETE | `/v1/conversations/:id` | 删除会话 | AiChatSessionService |
| 获取消息列表 | GET | `/v1/messages` | 查询历史消息 | AiChatSessionService |
| 消息反馈 | POST | `/v1/messages/:id/feedbacks` | 点赞/踩 | AiChatSessionService |
| 应用信息 | GET | `/v1/info` | 获取应用元信息 | IAiDifyAppService |
| 应用参数 | GET | `/v1/parameters` | 获取应用配置参数 | IAiDifyAppService |
| 文件上传 | POST | `/v1/files/upload` | 上传文件给Dify | DifyApiClient |

#### 10.3.2 DifyApiClient（HTTP封装类）设计

```java
/**
 * Dify API统一HTTP客户端
 * 
 * 职责：
 * 1. 从数据库读取API配置（非hardcode在yml中）
 * 2. 封装所有Dify REST API调用
 * 3. 统一异常处理和日志
 * 4. 支持阻塞/流式两种模式
 */
@Service
public class DifyApiClient {
    // 从 IAiDifyConfigService 动态读取配置
    // 根据 appId 获取对应的 apiKey 和 baseUrl
    // POST /v1/chat-messages  → chatMessage(appId, query, user, conversationId)
    // POST /v1/workflows/run  → runWorkflow(appId, inputs, user)
    // GET  /v1/conversations  → listConversations(appId, user)
    // GET  /v1/messages       → listMessages(appId, conversationId, user)
    // POST /v1/files/upload   → uploadFile(appId, file)
    // GET  /v1/info           → getAppInfo(appId)
    // POST /v1/chat-messages/:task_id/stop → stopGeneration(appId, taskId)
}
```

#### 10.3.3 API密钥安全策略

- API Key在数据库中**加密存储**（AES对称加密，密钥在application.yml中配置）
- 前端展示时**脱敏显示**（只显示前4位+后4位，中间用****替代）
- 连接测试时通过后端发起请求，API Key**不传到前端**
- 使用RuoYi内置的`@RequiresPermissions`控制谁可以管理API配置

### 10.4 前端页面设计

#### 10.4.1 Dify配置管理页面（ai/dify/config.html）

```
┌──────────────────────────────────────────────────────────────┐
│ [搜索] 配置名称: [____] 状态: [全部▾]  [🔍搜索] [↻重置]     │
├──────────────────────────────────────────────────────────────┤
│ [+ 添加] [✎ 修改] [✖ 删除]                                  │
├────┬──────────┬──────────────────┬──────┬────────┬──────────┤
│ □  │ 配置名称  │ API地址           │ 类型 │ 状态   │ 操作     │
├────┼──────────┼──────────────────┼──────┼────────┼──────────┤
│ □  │ 开发环境  │ http://local:8180│ Chat │ ✓正常  │ 测试|编辑│
│ □  │ 生产环境  │ https://dify.xxx │ WF   │ ✗停用  │ 测试|编辑│
└────┴──────────┴──────────────────┴──────┴────────┴──────────┘

[添加配置弹窗]
┌──────────────────────────────────┐
│  配置名称: [________________]    │
│  API地址:  [________________]    │
│  API密钥:  [________________] 👁 │
│  配置类型: ○Chat ○Workflow ○Comp │
│  默认配置: □                     │
│  [🔌 测试连接]  [✓保存] [✗取消]  │
└──────────────────────────────────┘
```

#### 10.4.2 Dify应用管理页面（ai/dify/app.html）

```
┌──────────────────────────────────────────────────────────────┐
│ [搜索] 应用名称: [____] 类型: [全部▾]  [🔍搜索] [↻重置]     │
├──────────────────────────────────────────────────────────────┤
│ [+ 添加] [🔄 从Dify同步] [✎ 修改] [✖ 删除]                  │
├────┬──────────┬──────┬──────────────┬────────┬──────────────┤
│ □  │ 应用名称  │ 类型 │ API密钥(脱敏) │ 状态   │ 操作         │
├────┼──────────┼──────┼──────────────┼────────┼──────────────┤
│ □  │ 需求分析  │ Chat │ app-****b3f2 │ ✓正常  │ 对话|编辑    │
│ □  │ 代码生成  │ WF   │ app-****a1c5 │ ✓正常  │ 运行|编辑    │
│ □  │ 代码评审  │ Chat │ app-****d8e1 │ ✓正常  │ 对话|编辑    │
└────┴──────────┴──────┴──────────────┴────────┴──────────────┘

[添加应用弹窗]
┌──────────────────────────────────┐
│  关联配置: [开发环境Dify ▾]       │
│  应用名称: [________________]    │
│  应用类型: ○Chat ○Workflow ○Comp │
│  应用API密钥: [________________] │
│  工作流ID:    [________________] │
│  应用描述: [________________]    │
│  [🔌 验证密钥]  [✓保存] [✗取消]  │
└──────────────────────────────────┘
```

#### 10.4.3 增强版AI对话页面（chat.html改造）

```
┌─────────┬───────────────────────────────────────────────────┐
│ 步骤导航 │  头部: [选择应用: 需求分析▾] [新建对话] [历史会话] │
│         ├───────────────────────────────────────────────────┤
│ 1.上传  │                                                   │
│ 2.提取  │  [历史会话侧栏]    │    聊天区域                   │
│ 3.适配  │  ┌────────────┐   │  ┌─────────────────────┐     │
│ 4.评估  │  │ 今天        │   │  │ 🤖 欢迎...           │     │
│ 5.报告  │  │  需求分析#1 │   │  │ 👤 请分析这个需求... │     │
│ 6.JSON  │  │  代码评审#2 │   │  │ 🤖 分析结果如下...    │     │
│ ─────── │  │ 昨天        │   │  └─────────────────────┘     │
│ 7.代码  │  │  需求分析#3 │   │                               │
│ 8.测试  │  └────────────┘   │  [输入区域]                   │
│ 9.评审  │                    │  [上传] [___________] [发送]  │
│ ─────── │                    │  [快捷按钮...]                │
│ 自由聊天│                    │                               │
└─────────┴───────────────────┴───────────────────────────────┘
```

### 10.5 后端分层架构

```
ruoyi-system/
├── domain/ai/
│   ├── AiDifyConfig.java       -- Dify配置实体
│   ├── AiDifyApp.java          -- Dify应用实体
│   ├── AiChatConversation.java -- 对话会话实体
│   └── AiChatMessage.java      -- 对话消息实体
├── mapper/ai/
│   ├── AiDifyConfigMapper.java + xml
│   ├── AiDifyAppMapper.java    + xml
│   ├── AiChatConversationMapper.java + xml
│   └── AiChatMessageMapper.java + xml
└── service/ai/
    ├── IAiDifyConfigService.java + impl
    ├── IAiDifyAppService.java    + impl
    └── IAiChatSessionService.java + impl（核心：对话管理+Dify调用+消息持久化）

ruoyi-admin/
├── controller/ai/
│   ├── AiDifyConfigController.java -- 配置CRUD + 连接测试
│   ├── AiDifyAppController.java    -- 应用CRUD + 同步
│   ├── AiAssistantController.java  -- 对话(改造：使用数据库配置)
│   └── DifyApiClient.java         -- Dify HTTP客户端(改造)
└── resources/templates/ai/
    ├── dify/
    │   ├── config/config.html + add.html + edit.html
    │   └── app/app.html + add.html + edit.html
    └── assistant/chat.html (改造)
```

### 10.6 核心交互流程

#### 10.6.1 用户首次使用流程

```
1. 管理员进入「AI开发平台 → Dify配置管理」
2. 点击[添加]，输入 Base URL + API Key
3. 点击[测试连接]，后端调用 GET /v1/info 验证连通性
4. 保存配置（API Key加密入库）

5. 进入「AI开发平台 → Dify应用管理」
6. 点击[添加]，选择刚才的配置，输入应用API Key
7. 点击[验证密钥]，后端调用 GET /v1/parameters 验证
8. 保存应用

9. 进入「AI开发平台 → AI开发助手」
10. 顶部下拉选择Dify应用
11. 开始对话（消息通过后端转发到Dify，响应持久化到数据库）
```

#### 10.6.2 对话消息流转

```
用户输入消息
     │
     ▼
chat.html → Ajax POST /ai/assistant/chat
     │
     ▼
AiAssistantController.chat()
     │
     ├─ 1. 根据appId从数据库获取Dify应用配置
     ├─ 2. 保存用户消息到 ai_chat_message
     ├─ 3. 调用DifyApiClient.chatMessage(appApiKey, baseUrl, query, conversationId)
     ├─ 4. 保存AI回复到 ai_chat_message
     ├─ 5. 更新会话的 message_count 和 last_message_time
     └─ 6. 返回AI回复给前端
```

---

## 十一、后续演进路线

### 11.1 短期（1-2个月）

- [x] 建立RuoYi框架规范Prompt库
- [x] AI需求分析全流程（6步）
- [x] AI代码生成/测试/评审服务
- [ ] **Dify配置管理（数据库+CRUD页面）**
- [ ] **Dify应用管理（数据库+CRUD页面）**
- [ ] **对话会话持久化（数据库+历史回顾）**
- [ ] **DifyApiClient重构（从数据库读取配置）**

### 11.2 中期（3-6个月）

- [ ] Dify RAG知识库对接（RuoYi规范文档自动同步）
- [ ] 流式响应（SSE）支持
- [ ] 多模型切换（通过Dify应用管理不同模型）
- [ ] 评估迁移到RuoYi-Vue（前后端分离版）的可行性
- [ ] 集成SonarQube实现自动代码质量门禁

### 11.3 长期（6-12个月）

- [ ] 实现需求→设计→代码的全自动化流水线
- [ ] 建立业务模块模板市场（可复用的AI生成模板）
- [ ] Dify Plugin开发（RuoYi代码生成专用插件）
- [ ] 建立知识库，持续优化AI生成质量

---

*本设计文档基于RuoYi v4.8.2源码深度分析，Dify 1.13.0 API文档，所有代码规范均来自框架实际代码模式。*
*v2.0更新：新增Dify平台深度集成设计（第十章），包含数据库设计、API对接规范、前端页面设计、后端分层架构。*
