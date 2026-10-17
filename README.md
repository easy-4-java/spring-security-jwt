# security-jwt-extension

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-17-orange)](https://github.com/easy-4-java/security-jwt-extension) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](https://www.apache.org/licenses/LICENSE-2.0.txt)

JWT authentication and authorization utilities for Spring Security. This module extends the Spring Security authentication model with JWT-aware tokens and filters, for both the servlet (Web) and reactive (WebFlux) stacks.

## Table of Contents

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

**What it is**

`security-jwt-extension` provides JWT-oriented building blocks for Spring Security applications: authentication tokens carrying a JWT principal/credentials plus optional request metadata (signature, longitude, latitude), a reactive JWT authentication web filter, a servlet-side authorization success handler, and a token-refresh REST endpoint placeholder.

**What it is not**

- It is not a JWT library — token creation/parsing is delegated to your application or the underlying Spring Security infrastructure.
- It is not a complete Spring Security starter: no auto-configuration is shipped in this module; wire the components into your own `SecurityFilterChain` / `SecurityWebFilterChain`.

**Typical scenarios**

| Scenario | Description |
| :--- | :--- |
| Servlet + JWT authorization | Attach `JwtAuthorizationSuccessHandler` to your authorization flow to build the response after a successful JWT check. |
| WebFlux + JWT authentication | Use `JwtAuthenticationWebFilter` with a `ReactiveAuthenticationManager` that validates JWT credentials. |
| Token refresh endpoint (WIP) | `RefreshTokenEndpoint` marks the intended location of a refresh-token REST endpoint. |

## 2. Features & Status

| Capability | Status | Notes |
| :--- | :--- | :--- |
| `JwtAuthenticationToken` | Available | Servlet authentication token with principal/credentials, `sign`, `longitude`, `latitude` metadata. |
| `JwtAuthorizationToken` | Available | Authorization-phase token with the same metadata fields. |
| `JwtAuthenticationWebFilter` | Available | WebFlux filter over `ReactiveAuthenticationManager`. |
| `JwtAuthorizationSuccessHandler` | Available | Servlet `AuthenticationSuccessHandler` for authorization success responses. |
| `RefreshTokenEndpoint` | WIP | `@RestController` placeholder; no endpoints implemented yet. |

> Status is reported as of `2.0.x.20260630-SNAPSHOT` on the `feature/2.0.x` branch.

## 3. Requirements & Compatibility

| Item | Version |
| :--- | :--- |
| JDK | 17+ |
| Maven | 3.9.16 (bundled Wrapper) |
| Spring Security | 6.4.7 (`spring-security-core`, `spring-security-web`) |
| Spring Framework | 6.2.8 (`spring-web`, `spring-webflux`) |
| Servlet API | Jakarta Servlet 6.0.0 (provided) |
| Jackson | 2.17.2 (`jackson-databind`) |
| easy4j dependency | `io.github.easy4j:spring-security-extension` |

**Version lines**

| Branch | JDK baseline | Version pattern |
| :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

## 4. Architecture & Modules

```text
 Servlet / WebFlux client (Bearer JWT)
        |
        +--(servlet)--> JwtAuthorizationSuccessHandler
        |
        +--(webflux)--> JwtAuthenticationWebFilter
                           | ReactiveAuthenticationManager
                           v
                     JwtAuthenticationToken / JwtAuthorizationToken
                           |  (sign, longitude, latitude)
                           v
                     Spring Security (core)
                           |
                           +-- RefreshTokenEndpoint (WIP, REST)
```

This is a **single-module** project (packaging `jar`):

| Module / artifact | Role |
| :--- | :--- |
| `security-jwt-extension` | JWT tokens, filters and handlers for Spring Security (servlet + WebFlux). |

## 5. Installation

The artifact is not yet published to Maven Central. Resolve it from the project's configured artifact repository (Aliyun Packages) or install it locally from source; the snapshot version currently used on the `feature/2.0.x` branch is `2.0.x.x.20260630-SNAPSHOT`.

**Maven**

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>security-jwt-extension</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.easy4j:security-jwt-extension:2.0.x.x.20260630-SNAPSHOT'
```

## 6. Quick Start

Build a JWT authentication token and let the reactive filter hand it to your `ReactiveAuthenticationManager`:

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

// 2. Reactive filter wired with your JWT-validating manager
ReactiveAuthenticationManager manager = /* your manager */;
JwtAuthenticationWebFilter filter = new JwtAuthenticationWebFilter(manager);
```

**Expected result:** incoming requests matched by the filter are authenticated against the manager using the JWT credentials carried by `JwtAuthenticationToken`.

## 7. Configuration

This is a pure library: no configuration properties, no property prefix, no auto-configuration. All components are instantiated and wired by the application.

## 8. Core Usage / API

| Class | Package | Role |
| :--- | :--- | :--- |
| `JwtAuthenticationToken` | `org.springframework.security.boot.jwt.authentication` | `AbstractAuthenticationToken` subclass for the authentication phase. |
| `JwtAuthorizationToken` | `org.springframework.security.boot.jwt.authorization` | Token for the authorization phase (same metadata fields). |
| `JwtAuthenticationWebFilter` | `org.springframework.security.boot.jwt.authentication` | WebFlux `AuthenticationWebFilter` using a `ReactiveAuthenticationManager`. |
| `JwtAuthorizationSuccessHandler` | `org.springframework.security.boot.jwt.authorization` | Servlet `AuthenticationSuccessHandler`; also exposes `clearAuthenticationAttributes`. |
| `RefreshTokenEndpoint` | `org.springframework.security.boot.jwt.endpoint` | `@RestController` placeholder (no endpoints yet). |

Servlet-style authorization success handling:

```java
import org.springframework.security.boot.jwt.authorization.JwtAuthorizationSuccessHandler;

JwtAuthorizationSuccessHandler handler = new JwtAuthorizationSuccessHandler();
// Called by your authorization flow when the JWT check succeeds
handler.onAuthenticationSuccess(request, response, authentication);
```

## 9. Testing & Build

```bash
# Full build with JaCoCo coverage report/check
./mvnw clean verify

# Install into the local repository
./mvnw install
```

Test & gate facts (as configured in the pom):

- No unit tests exist in this module yet.
- JaCoCo is bound to `prepare-agent` / `report` / `check`; the `check` rule requires a **90% line coverage ratio** (configured with `haltOnFailure=false`).

## 10. Versioning & Branches

| Branch | JDK baseline | Version pattern | Status |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | Active; current snapshot `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | Maintained |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | Maintained |

Maintenance strategy: the 1.0.x line keeps JDK 8 compatibility for legacy deployments; the 2.0.x and 3.0.x lines are the modern JDK baselines. Release artifacts are published to the project's configured artifact repository (Aliyun Packages) and GitHub Releases; the project has not yet published to Maven Central.

## 11. Contributing & License

Contributions are welcome — please open an issue or a pull request on the [GitHub repository](https://github.com/easy-4-java/security-jwt-extension).

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.
