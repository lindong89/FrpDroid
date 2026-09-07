# FrpDroid

> 基于 [frp](https://github.com/fatedier/frp) 与 [cloudflared](https://github.com/cloudflare/cloudflared) 的 Android 内网穿透客户端，使用 Kotlin + Jetpack Compose 开发。

FrpDroid 是一个运行在 Android 上的内网穿透工具，内置 `frpc` 与 `cloudflared` 二进制，支持 FRP 隧道与 Cloudflare Tunnel 两种方式，将内网服务安全地暴露到公网。

## ✨ 功能特性

- **FRP 隧道**：内置 frpc，支持 TCP / UDP / HTTP / HTTPS / TCPMux / STCP / XTCP / Unix Socket / Static File / HTTPS→HTTP 等多种代理类型
- **Cloudflare Tunnel**：内置 cloudflared，填写 Tunnel Token 即可一键连接，支持开机自启
- **多服务器管理**：可保存多台 FRP 服务器（地址 / 端口 / 令牌 / 用户），支持新增、重命名、删除与导入导出
- **配置导入导出**：代理与访问者配置可导出为 `frpc.toml`，也可从配置文件导入，支持覆盖或作为新服务器导入
- **实时日志**：隧道运行日志实时输出，支持一键清空、自动滚动到底部
- **流量统计**：实时展示今日下载 / 上传流量与当前速率，支持一键重置
- **开机自启**：FRP 与 Cloudflare 隧道均可设置开机自启（前台服务 + 开机广播）
- **通知栏常驻**：运行期间显示前台服务通知
- **双语言**：中文 / English 一键切换
- **双主题**：深色 / 浅色主题一键切换（默认深色）
- **自定义图标**：内置全套手绘风格图标与品牌 Logo

## 🖥️ 界面预览

- 底部悬浮导航栏（毛玻璃效果），四个 Tab：主页 / 配置 / 日志 / 设置
- 主页：FRP 隧道状态卡、Cloudflare 隧道卡、流量仪表
- 配置：服务器信息、代理列表、访问者列表、保存 / 导入 / 导出
- 日志：实时输出窗口
- 设置：开机自启、通知、语言、主题、关于（frp / cloudflared 版本号）

## 📦 技术栈

| 组件 | 说明 |
|---|---|
| 语言 | Kotlin 2.1 + Jetpack Compose (Material 3) |
| 构建 | Gradle 8.13 / AGP 8.7.3，JDK 17 |
| minSdk / targetSdk | 24 / 34 |
| 隧道内核 | frpc、cloudflared（arm64-v8a 原生库） |
| 架构 | 单 Activity + Compose UI，前台服务承载隧道进程 |

## 🚀 构建

```bash
# 需要 JDK 17 与 Android SDK（local.properties 中配置 sdk.dir）
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 📁 项目结构

```
app/src/main/java/com/frpdroid/app/
├── MainActivity.kt      # 入口
├── MainUi.kt            # Compose UI（主页/配置/日志/设置/弹窗）
├── FrpService.kt        # 前台服务：frpc / cloudflared 进程管理
├── BootReceiver.kt      # 开机自启广播
├── ProxyConfig.kt       # 代理/访问者配置模型
├── ServerConfig.kt      # 服务器配置模型
├── ParsedConfig.kt      # frpc.toml 解析
├── TrafficInfo.kt       # 流量统计模型
└── T.kt                 # 中英文文案字典
```

## 📄 License

本项目仅供学习与个人使用，具体请参考 LICENSE 文件。

---

**注意**：本应用仅负责运行隧道连接；Cloudflare Tunnel 的路由（Public Hostname / 自定义域名）需在 Cloudflare 官网控制面板中配置。
