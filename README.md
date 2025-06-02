# 用友数据字典 MCP 服务 - 多版本架构

> ⚠️本项目基于用友对外公开的字典服务搭建，完全出于个人学习目的开发。

## 项目介绍

本项目是一个基于 Spring Boot 3.x 和 Spring AI 构建的 Model Context Protocol (MCP) 服务器，用于访问和查询用友数据字典服务。
**现已支持多版本架构，可根据不同的 default-app-code 自动适配不同的用友版本。**

## 🎯 架构升级亮点

### 多版本支持架构

项目采用**策略模式 + 工厂模式**实现了统一的多版本适配架构：

- **自动版本检测**：根据 `default-app-code` 自动识别用友版本类型
- **版本适配器**：每个版本有独立的适配器处理不同的解析逻辑
- **统一接口**：对外接口保持不变，内部根据版本选择不同策略
- **易于扩展**：新增版本只需实现对应的适配器即可

### 当前支持的版本

| 版本类型            | 应用代码示例            | 解析方式                    | 适配器                   | 状态    |
|-----------------|-------------------|-------------------------|-----------------------|-------|
| **YonBIP高级版系列** | `yonbip3ddc`      | 解析JS中的dataDictIndexData | YonBipAdvancedAdapter | ✅ 已实现 |
| **YonBIP旗舰版系列** | `yonbip-flagship` | 解析JS，但格式不同              | YonBipFlagshipAdapter | ✅ 已实现 |
| **NC65系列**      | `ncddc0065`       | 解析HTML页面                | NC65Adapter           | ✅ 已实现 |
| **NCCloud系列**   | `nccloud`         | 解析HTML页面                | NCCloudAdapter        | ✅ 已实现 |

## 功能特性

- 🔍 **智能搜索**：支持按名称模糊搜索用友数据字典条目
- 📋 **详情查询**：根据类ID获取完整的数据字典详情信息
- 🚀 **MCP 协议**：完全兼容 Model Context Protocol，可与各种 AI 客户端集成
- 💾 **缓存机制**：内置 LRU 缓存，提升查询性能
- 🐳 **容器化部署**：支持 Docker 和 Docker Compose 部署
- 📊 **健康检查**：集成 Spring Boot Actuator，提供服务健康监控
- 🎯 **多版本支持**：自动适配不同用友版本，支持JS解析、HTML解析等多种方式

## 技术栈

- **框架**：Spring Boot 3.5.0
- **Java 版本**：Java 17
- **MCP 支持**：Spring AI MCP Server
- **网络请求**：Spring WebMVC
- **HTML 解析**：Jsoup 1.17.2
- **JSON 处理**：FastJSON 2.0.51
- **容器化**：Docker & Docker Compose
- **架构模式**：策略模式 + 工厂模式

## 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.6 或更高版本
- Docker（可选，用于容器化部署）

### Docker 部署

```bash
# 下载仓库代码
git clone xxx

# 构建并启动服务
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```
### 手动启动/stdio

```bash
java -jar yonyou-data-dict.jar # 这里放打包后的jar包
```

## 配置说明

### 应用配置

主要配置位于 `src/main/resources/application.yml`：

```yaml
data-dict:
  base-url: https://media.oyonyou.com:18000/oyonyou/dict  # 用友数据字典服务地址
  static-path: /static/js/data-dict-tree.js              # 静态资源路径（高级版使用）
  default-app-code: yonbip3ddc                           # 默认应用代码（决定版本类型）
  cache-enabled: true                                    # 是否启用缓存
  cache-size: 100                                        # 缓存大小
```

### 生产环境配置

生产环境配置位于 `src/main/resources/application-prod.yml`，可根据需要调整。

### 多版本配置示例

不同版本的配置示例：

```yaml
# YonBIP高级版（当前默认）
data-dict:
  default-app-code: yonbip3ddc

# YonBIP旗舰版
data-dict:
  default-app-code: yonbip3r6bip2

# NC65
data-dict:
  default-app-code: ncddc0065
```

## 架构详解

### 核心组件

```
├── adapter/                    # 版本适配器
│   ├── VersionAdapter         # 适配器接口
│   ├── VersionAdapterFactory  # 适配器工厂
│   └── impl/                  # 具体适配器实现
│       ├── YonBipAdvancedAdapter    # YonBIP高级版
│       ├── YonBipFlagshipAdapter    # YonBIP旗舰版
│       └── NC65Adapter              # NC65版本
├── model/
│   └── YonyouVersion          # 版本枚举
└── util/
    └── DataDictDownloader     # 下载器
```

### 工作流程

1. **版本检测**：系统启动时根据 `default-app-code` 自动检测版本
2. **适配器选择**：工厂模式选择对应的版本适配器
3. **URL构建**：适配器根据版本特点构建正确的请求URL
4. **内容解析**：适配器使用对应的解析策略（JS/HTML/API）
5. **统一返回**：所有版本返回统一的数据格式

### 扩展新版本

要添加新版本支持，只需：

1. 在 `YonyouVersion` 枚举中添加新版本
2. 实现 `VersionAdapter` 接口
3. 使用 `@Component` 注解让Spring自动注册

## API接口

### 健康检查相关

- `GET /check` - 基础健康检查
- `GET /check/architecture/status` - 查看当前架构状态和支持版本
- `GET /check/architecture/detect?appCode=xxx` - 检测指定应用代码的版本

### 数据字典相关

- `GET /check/tool/items` - 获取所有数据字典条目
- `GET /check/tool/detail/{classId}` - 获取指定类的详情
- `GET /check/tool/search?name=xxx` - 按名称搜索数据字典

## 项目结构

```
src/
├── main/
│   ├── java/win/ixuni/yonyoudatadict/
│   │   ├── YonyouDataDictApplication.java    # 主启动类
│   │   ├── adapter/                          # 多版本适配器架构
│   │   │   ├── VersionAdapter.java          # 适配器接口
│   │   │   ├── VersionAdapterFactory.java   # 适配器工厂
│   │   │   └── impl/                        # 具体实现
│   │   │       ├── YonBipAdvancedAdapter.java
│   │   │       ├── YonBipFlagshipAdapter.java
│   │   │       └── NC65Adapter.java
│   │   ├── cache/
│   │   │   └── LRUCache.java                # LRU缓存实现
│   │   ├── config/
│   │   │   └── DataDictConfig.java          # 配置类
│   │   ├── controller/
│   │   │   └── healthCheckController.java   # 健康检查控制器（已扩展）
│   │   ├── model/
│   │   │   ├── DataDictDetail.java          # 数据字典详情模型
│   │   │   ├── DataDictItem.java            # 数据字典项模型
│   │   │   └── YonyouVersion.java           # 版本枚举（新增）
│   │   ├── processor/
│   │   │   ├── DataDictProcessor.java       # 数据字典处理器接口
│   │   │   └── ...                          # 处理器实现
│   │   ├── service/
│   │   │   └── DataDictService.java         # 核心业务服务
│   │   └── util/
│   │       └── DataDictDownloader.java      # 多版本下载工具（重构）
│   └── resources/
│       ├── application.yml                  # 主配置文件
│       └── application-prod.yml             # 生产环境配置
```

## MCP 客户端集成

本服务实现了SSE和STDIO两种模式，可以与以下客户端集成：

- Claude Desktop
- VS Code (通义灵码、GitHub Copilot等)
- 其他支持 MCP 的 AI 客户端

### MCP 客户端配置示例

#### VS Code配置示例

```json
{
  "mcp": {
    "servers": {
      "yonyou": {
        "type": "sse",
        "url": "http://127.0.0.1:8080/sse"
      }
    }
  }
}
```

#### 通义灵码配置示例

```json
{
  "mcp": {
    "servers": {
      "yonYouDataDict": {
        "type": "stdio",
        "command": "java",
        "args": [
          "-Dspring.ai.mcp.server.stdio=true",
          "-Dfile.encoding=UTF-8",
          "-jar",
          "E:\\your_path\\yonyouDataDict-0.0.3-SNAPSHOT.jar"
        ]
      }
    }
  }
}
```

## SSE 与 STDIO 模式

本项目提供了基于 HTTP 的 SSE (Server-Sent Events) 以及基于本机进程通信的 STDIO 模式：

- **SSE 模式**：可快速通过 Docker 启动，但网络不稳定时需要重启 MCP 客户端。
- **STDIO 模式**：依赖本机进程通信，连接更稳定，推荐使用。需注意此模式,依然需要占用端口，可通过 `-Dserver.port=xxxx` 进行调整。

### 启动示例

启动stdio需要提前将代码通过maven打成jar包,并记录路径,下面以42661端口,路径yourApp.jar为例

```bash
java -Dfile.encoding=UTF-8 \
     -Dspring.ai.mcp.server.stdio=true \
     -Dserver.port=42661 \
     -jar yourApp.jar
```

> 如果搜索英文有返回但中文搜索无结果，请务必添加 `-Dfile.encoding=UTF-8` 选项。

## 常见问题

### Q: 如何切换到不同的用友版本？

A: 在 `application.yml` 中修改 `data-dict.default-app-code` 配置即可：
比如
- YonBIP高级版：`yonbip3ddc`
- YonBIP旗舰版：`yonbip3r6bip2`
- NC65：`ncddc0065`

### Q: 如何验证当前支持的版本？

A: 访问 `http://localhost:8080/check/architecture/status` 查看当前架构状态和所有支持的版本。

### Q: 如何添加新的版本支持？

A:

1. 在 `YonyouVersion` 枚举中添加新版本定义
2. 创建对应的适配器类实现 `VersionAdapter` 接口
3. 使用 `@Component` 注解让Spring自动注册

### Q: 服务启动失败怎么办？
A: 请检查：
- Java 版本是否为 17 或更高
- 网络连接是否正常（需要访问用友服务）
- 端口是否被占用
- `default-app-code` 配置是否正确

### Q: 缓存如何清理？

A: 重启应用会自动清理内存缓存。系统运行中如果超过了允许的最大缓存个数，会自动清理最久未使用的缓存。

### Q: 如何修改数据源？

A: 目前基于 www.oyonyou.com 抓包的url，如果情况特殊，可以修改 `application.yml` 中的 `data-dict.base-url` 配置。

## 贡献

欢迎提交 Issue 和 Pull Request！特别是新版本适配器的贡献。

## 联系方式

如有问题，请通过 GitHub Issues 联系。

如有建议，请通过邮箱：maolei01@yonyou.com 联系。
> 邮箱联系时，请使用yonyou邮箱联系