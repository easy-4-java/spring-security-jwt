# security-jwt-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/security-jwt-extension) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](https://www.apache.org/licenses/LICENSE-2.0.txt)

面向 Spring Security 的 JWT 认证与授权工具。本模块为 Spring Security 认证模型补充 JWT 感知的 Token 与过滤器，同时覆盖 Servlet（Web）与响应式（WebFlux）技术栈。

## 目录

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**是什么**

`security-jwt-extension` 为 Spring Security 应用提供面向 JWT 的构建块：携带 JWT 主体/凭证以及可选请求元数据（签名、经度、纬度）的认证 Token、响应式 JWT 认证 Web 过滤器、Servlet 侧授权成功处理器，以及一个 Token 刷新 REST 端点占位。

**不是什么**

- 它不是 JWT 库——Token 的签发与解析由你的应用或底层 Spring Security 基础设施负责。
- 它不是完整的 Spring Security Starter：本模块不提供自动配置；组件需要你在自己的 `SecurityFilterChain` / `SecurityWebFilterChain` 中装配。

**典型场景**

| 场景 | 说明 |
| :--- | :--- |
| Servlet + JWT 授权 | 在授权流程中接入 `JwtAuthorizationSuccessHandler`，在 JWT 校验成功后构造响应。 |
| WebFlux + JWT 认证 | 使用 `JwtAuthenticationWebFilter` 配合校验 JWT 凭证的 `ReactiveAuthenticationManager`。 |
| Token 刷新端点（开发中） | `RefreshTokenEndpoint` 标识 refresh-token REST 端点的预期位置。 |

## 2. Features & Status

| 能力 | 状态 | 说明 |
| :--- | :--- | :--- |
| `JwtAuthenticationToken` | 可用 | Servlet 认证 Token，含主体/凭证与 `sign`、`longitude`、`latitude` 元数据。 |
| `JwtAuthorizationToken` | 可用 | 授权阶段 Token，元数据字段相同。 |
| `JwtAuthenticationWebFilter` | 可用 | 基于 `ReactiveAuthenticationManager` 的 WebFlux 过滤器。 |
| `JwtAuthorizationSuccessHandler` | 可用 | Servlet `AuthenticationSuccessHandler`，用于授权成功响应。 |
| `RefreshTokenEndpoint` | 开发中 | `@RestController` 占位，尚未实现任何端点。 |

> 状态以 `feature/1.0.x` 分支上的 `1.0.x.20260630-SNAPSHOT` 为准。

## 3. Requirements & Compatibility

| 项目 | 版本 |
| :--- | :--- |
| JDK | 8+ |
| Maven | 3.9.16（内置 Maven Wrapper） |
| Spring Security | 5.7.11（`spring-security-core`、`spring-security-web`） |
| Spring Framework | 5.3.31（`spring-web`、`spring-webflux`） |
| Jackson | 2.17.2（`jackson-databind`） |
| easy4j 依赖 | `io.github.easy4j:spring-security-extension` |

**版本线**

| 分支 | JDK 基线 | 版本模式 |
| :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

## 4. Architecture & Modules

```text
 Servlet / WebFlux 客户端（Bearer JWT）
        |
        +--(servlet)--> JwtAuthorizationSuccessHandler
        |
        +--(webflux)--> JwtAuthenticationWebFilter
                           | ReactiveAuthenticationManager
                           v
                     JwtAuthenticationToken / JwtAuthorizationToken
                           |  (sign, longitude, latitude)
                           v
                     Spring Security（core）
                           |
                           +-- RefreshTokenEndpoint（开发中，REST）
```

本项目为**单模块**工程（packaging 为 `jar`）：

| 模块 / 构件 | 职责 |
| :--- | :--- |
| `security-jwt-extension` | 面向 Spring Security 的 JWT Token、过滤器与处理器（Servlet + WebFlux）。 |

## 5. Installation

该构件尚未发布到 Maven Central。请从项目配置的制品仓库（阿里云制品仓库）获取，或从源码本地安装；`feature/1.0.x` 分支当前使用的快照版本为 `1.0.x.20260630-SNAPSHOT`。

**Maven**

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>security-jwt-extension</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.easy4j:security-jwt-extension:1.0.x.20260630-SNAPSHOT'
```

## 6. Quick Start

构造 JWT 认证 Token，并交给响应式过滤器转发给你的 `ReactiveAuthenticationManager`：

```java
import org.springframework.security.boot.jwt.authentication.JwtAuthenticationToken;
import org.springframework.security.boot.jwt.authentication.JwtAuthenticationWebFilter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;

// 1. Token
JwtAuthenticationToken token =
        new JwtAuthenticationToken(principal, jwtCredentials);
token.setSign("optional-request-sign");
token.setLongitude(116.397128d);
token.setLatitude(39.916527d);

// 2. 用你的 JWT 校验管理器装配响应式过滤器
ReactiveAuthenticationManager manager = /* 你的管理器 */;
JwtAuthenticationWebFilter filter = new JwtAuthenticationWebFilter(manager);
```

**预期结果：** 过滤器匹配到的请求将使用 `JwtAuthenticationToken` 携带的 JWT 凭证交由管理器完成认证。

## 7. Configuration

这是纯库：没有配置属性、没有属性前缀、没有自动配置。所有组件由应用自行实例化并装配。

## 8. Core Usage / API

| 类 | 包 | 职责 |
| :--- | :--- | :--- |
| `JwtAuthenticationToken` | `org.springframework.security.boot.jwt.authentication` | 认证阶段的 `AbstractAuthenticationToken` 子类。 |
| `JwtAuthorizationToken` | `org.springframework.security.boot.jwt.authorization` | 授权阶段的 Token（元数据字段相同）。 |
| `JwtAuthenticationWebFilter` | `org.springframework.security.boot.jwt.authentication` | 基于 `ReactiveAuthenticationManager` 的 WebFlux `AuthenticationWebFilter`。 |
| `JwtAuthorizationSuccessHandler` | `org.springframework.security.boot.jwt.authorization` | Servlet `AuthenticationSuccessHandler`；另暴露 `clearAuthenticationAttributes`。 |
| `RefreshTokenEndpoint` | `org.springframework.security.boot.jwt.endpoint` | `@RestController` 占位（尚无端点）。 |

Servlet 风格授权成功处理：

```java
import org.springframework.security.boot.jwt.authorization.JwtAuthorizationSuccessHandler;

JwtAuthorizationSuccessHandler handler = new JwtAuthorizationSuccessHandler();
// 由你的授权流程在 JWT 校验成功后调用
handler.onAuthenticationSuccess(request, response, authentication);
```

## 9. Testing & Build

```bash
# 完整构建（含 JaCoCo 覆盖率报告/检查）
./mvnw clean verify

# 安装到本地仓库
./mvnw install
```

测试与门禁事实（以 pom 配置为准）：

- 本模块暂无单元测试。
- JaCoCo 绑定 `prepare-agent` / `report` / `check`；`check` 规则要求**行覆盖率不低于 90%**（配置了 `haltOnFailure=false`）。

## 10. Versioning & Branches

| 分支 | JDK 基线 | 版本模式 | 状态 |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | 活跃；当前快照 `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | 维护中 |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | 维护中 |

维护策略：1.0.x 版本线保持 JDK 8 兼容，服务于存量部署；2.0.x 与 3.0.x 版本线为现代 JDK 基线。发布制品发布到项目配置的制品仓库（阿里云制品仓库）与 GitHub Releases；项目尚未发布到 Maven Central。

## 11. Contributing & License

欢迎参与贡献——请在 [GitHub 仓库](https://github.com/easy-4-java/security-jwt-extension) 提交 Issue 或 Pull Request。

本项目基于 **Apache License 2.0** 开源。详见 [LICENSE](LICENSE)。
