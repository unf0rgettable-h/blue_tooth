<div align="center">

# 📡 SurvLink

**专业的全站仪蓝牙数据采集工具 | Professional Total Station Bluetooth Data Collector**

[![Release](https://img.shields.io/github/v/release/unf0rgettable-h/blue_tooth?style=flat-square)](https://github.com/unf0rgettable-h/blue_tooth/releases)
[![Downloads](https://img.shields.io/github/downloads/unf0rgettable-h/blue_tooth/total?style=flat-square)](https://github.com/unf0rgettable-h/blue_tooth/releases)
[![License](https://img.shields.io/github/license/unf0rgettable-h/blue_tooth?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://www.android.com)
[![Stars](https://img.shields.io/github/stars/unf0rgettable-h/blue_tooth?style=flat-square)](https://github.com/unf0rgettable-h/blue_tooth/stargazers)

[English](#english) | [中文](#chinese)

</div>

---

<a name="chinese"></a>

## 🌟 为什么选择 SurvLink？

**SurvLink 是面向全站仪数据落地的开源 Android 应用：TS09/FlexLine 走经典蓝牙导入，TS60/Captivate 1.6.0 新增手机热点 + WLAN FTP 完整项目文件接收。**

### 核心优势

- 🎯 **TS60 完整项目文件接收** - 手机端开启 FTP 服务，Captivate 通过 WLAN/热点上传 job/dbx/csv/xml/dxf/zip
- 📡 **普通蓝牙稳定保留** - TS09/FlexLine 继续使用已验证的经典蓝牙导入 channel
- 📊 **专业数据导出** - CSV 导出包含弧度、度、gon 三种角度单位
- 🔄 **独立 channel 架构** - TS09 蓝牙 channel 与 TS60 WLAN/FTP channel 分离，避免协议假设互相污染
- 🧪 **高质量代码** - 80%+ 测试覆盖率，经过严格的单元测试和集成测试
- 🆓 **完全开源** - MIT 许可证，代码透明，社区驱动

### 解决的痛点

❌ **之前**: TS60 完整项目文件无法稳定从仪器落到普通 Android 手机
✅ **现在**: APP 可在手机热点/WLAN 上启动 FTP 接收服务，TS60 通过 Captivate FTP data transfer 上传完整项目文件

---

## 🚀 快速开始

### 下载安装

1. 前往 [Releases 页面](https://github.com/unf0rgettable-h/blue_tooth/releases/latest)
2. 下载 `survlink-v1.6.0-signed.apk`
3. 在 Android 设备上启用"允许安装未知应用"
4. 安装 APK

**系统要求**: Android 8.0+ (API 26+) | 蓝牙经典模式 | ~7 MB 存储空间

### 使用方法

#### FlexLine 仪器（TS02-TS13）
1. 打开应用 → 蓝牙页面
2. 选择品牌和型号
3. 连接配对的设备
4. 数据页面 → 开始接收
5. 在仪器上测量，数据自动显示

#### TS60 / Captivate 项目文件传输（v1.6.0）
1. 手机开启热点，并让 TS60 连接该热点
2. APP 中选择 Leica TS60 → 数据页面 → 点击 `启动WLAN项目接收`
3. APP 会显示 FTP 地址、端口、用户名和密码
4. TS60 打开 `Settings > Tools > FTP data transfer`
5. 在 TS60 中输入 APP 显示的 FTP 信息，选择 job/dbx/csv/xml/dxf/zip 上传
6. APP 接收后可 `停止并打包项目`，再分享或保存到下载目录

---

## 📱 功能特性

### TS60 WLAN/FTP 项目传输 ✨ v1.6.0 新增

<table>
<tr>
<td width="50%">

**完整项目接收**
- 手机端内置 FTP Server
- 支持手机热点/WLAN 局域网
- 支持 job/dbx/csv/xml/dxf/zip
- 多文件项目可归档为 ZIP

</td>
<td width="50%">

**独立传输 channel**
- TS09 继续经典蓝牙导入
- TS60 使用 WLAN/FTP 项目接收
- 蓝牙 RFCOMM 仅保留实验诊断
- 错误状态和接收状态分离

</td>
</tr>
<tr>
<td width="50%">

**项目包处理**
- 保留 Captivate 目录结构
- DBX 原始文件完整保存
- ZIP 项目包便于分享/归档
- 文件数量和总大小可见

</td>
<td width="50%">

**GeoCOM 能力保留**
- GeoCOM 协议层仍在代码中保留
- 后续可扩展实时测量控制
- 当前 1.6.0 主目标是完整项目文件落地
- 不把项目文件传输误判为测量轮询

</td>
</tr>
</table>

### 支持的仪器

| 品牌 | 型号 | 固件 | 协议 | 蓝牙 |
|------|------|------|------|------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | GSI-Online | SPP ~15m |
| Leica | **TS60** ⭐ | Captivate | **WLAN FTP项目文件接收** ✨ | 手机热点/WLAN |
| Leica | TS16, TS50, MS60 | Captivate | GeoCOM 代码层保留，项目传输待扩展 | SPP + WLAN |
| Leica | iCR80 | iCON | GSI-Online | SPP + CCD6 400m |
| Sokkia | SX/CX/iM/iX 系列 | — | 被动流 | SPP |
| Topcon | ES/MS/GT 系列 | — | 被动流 | SPP |
| South | NTS 系列 | — | 被动流 | SPP |
| CHC | HTS 系列 | — | 被动流 | SPP |
| Hi-Target | ZTS/iTrack/iAngle 系列 | — | 被动流 | SPP |

---

## 🏗️ 技术架构

### 独立数据 channel

```
TS09/FlexLine
    BluetoothConnectionManager
    → BluetoothClientImportManager
    → ImportedFileInfo

TS60/Captivate
    LocalFtpServerManager
    → Captivate FTP data transfer 上传项目文件
    → ProjectTransferArchiveWriter
    → ZIP 项目包 / ImportedFileInfo
```

### 技术栈

- **语言**: Kotlin 100%
- **UI**: Jetpack Compose
- **架构**: MVVM + 单 Activity
- **数据库**: Room (SQLite)
- **并发**: Kotlin Coroutines + StateFlow
- **蓝牙**: Classic Bluetooth SPP
- **WLAN**: 手机端本地 FTP Server（TS60 项目文件接收）
- **测试**: JUnit + Robolectric (80%+ 覆盖率)

### 代码质量

- ✅ 80%+ 测试覆盖率
- ✅ 11 个单元测试类
- ✅ 集成测试和 UI 测试
- ✅ 严格的代码审查
- ✅ TDD 开发流程

---

## 📖 文档

- [CHANGELOG.md](CHANGELOG.md) - 详细版本历史
- [CONTRIBUTING.md](CONTRIBUTING.md) - 贡献指南
- [TESTING.md](TESTING.md) - 硬件测试指南
- [ROADMAP.md](ROADMAP.md) - 产品路线图
- [PROJECT_STATUS.md](PROJECT_STATUS.md) - 项目状态报告

---

## 🤝 贡献

欢迎贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

### 贡献方式

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📝 改进文档
- 🔧 提交代码
- ⭐ 给项目加星

---

## 📜 许可证

本项目采用 [MIT License](LICENSE) 开源。

---

## 🙏 致谢

本项目通过 AI 协作开发完成：
- **架构设计**: Claude (Opus 4.6)
- **后端实现**: Codex (GPT-5.4)
- **前端实现**: Gemini (3.1 Pro)

感谢测绘社区的反馈和功能建议！

---

## 📞 联系方式

- **Issues**: [GitHub Issues](https://github.com/unf0rgettable-h/blue_tooth/issues)
- **Discussions**: [GitHub Discussions](https://github.com/unf0rgettable-h/blue_tooth/discussions)
- **Email**: 通过 GitHub 联系

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐ Star！**

Made with ❤️ for surveyors worldwide

</div>

---

<a name="english"></a>

# 📡 SurvLink

**Professional Total Station Bluetooth and WLAN Project Data Collector**

## 🌟 Why SurvLink?

**SurvLink gets total-station data onto Android phones: TS09/FlexLine keeps the classic Bluetooth import channel, while TS60/Captivate v1.6.0 adds phone-hotspot + WLAN FTP full project-file transfer.**

### Key Advantages

- 🎯 **TS60 Full Project Transfer** - Phone-hosted FTP receiver for Captivate job/dbx/csv/xml/dxf/zip uploads
- 📡 **Stable Bluetooth Path Preserved** - TS09/FlexLine remains on the verified classic Bluetooth channel
- 📊 **Professional Data Export** - CSV export includes angles in radians, degrees, and gon
- 🔄 **Independent Channel Architecture** - Bluetooth import and WLAN/FTP project transfer stay separated
- 🧪 **High-Quality Code** - 80%+ test coverage with rigorous unit and integration tests
- 🆓 **Fully Open Source** - MIT License, transparent code, community-driven

### Problem Solved

❌ **Before**: TS60 full project files could not reliably land on a regular Android phone
✅ **Now**: The app starts an FTP receiver over phone hotspot/WLAN, and Captivate uploads complete project files through FTP data transfer

---

## 🚀 Quick Start

### Download & Install

1. Go to [Releases page](https://github.com/unf0rgettable-h/blue_tooth/releases/latest)
2. Download `survlink-v1.6.0-signed.apk`
3. Enable "Install from unknown sources" on your Android device
4. Install the APK

**Requirements**: Android 8.0+ (API 26+) | Bluetooth Classic | ~7 MB storage

### Usage

#### FlexLine Instruments (TS02-TS13)
1. Open app → Bluetooth page
2. Select brand and model
3. Connect to paired device
4. Data page → Start Receiving
5. Take measurements on instrument, data appears automatically

#### TS60 / Captivate Project Files (v1.6.0)
1. Turn on the phone hotspot and connect the TS60 to it
2. In the app, select Leica TS60 → Data page → tap `启动WLAN项目接收`
3. The app shows FTP host, port, username, and password
4. On the TS60, open `Settings > Tools > FTP data transfer`
5. Enter the FTP details and upload job/dbx/csv/xml/dxf/zip files
6. Tap `停止并打包项目` in the app, then share or save the ZIP package

---

## 📱 Features

### TS60 WLAN/FTP Project Transfer ✨ New in v1.6.0

<table>
<tr>
<td width="50%">

**Complete Project Reception**
- Phone-hosted FTP server
- Phone hotspot / shared WLAN
- job/dbx/csv/xml/dxf/zip uploads
- Multi-file projects packaged as ZIP

</td>
<td width="50%">

**Independent Channels**
- TS09 keeps Bluetooth import
- TS60 uses WLAN/FTP project transfer
- Bluetooth RFCOMM remains diagnostic only
- Separate state and error handling

</td>
</tr>
<tr>
<td width="50%">

**Project Package Handling**
- Captivate folder structure preserved
- DBX files saved intact
- ZIP package for share/archive
- File count and total size visible

</td>
<td width="50%">

**GeoCOM Code Retained**
- GeoCOM protocol layer remains in code
- Future live measurement expansion stays possible
- v1.6.0 focuses on full project files
- File transfer is not treated as measurement polling

</td>
</tr>
</table>

### Supported Instruments

| Brand | Models | Firmware | Protocol | Bluetooth |
|-------|--------|----------|----------|-----------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | GSI-Online | SPP ~15m |
| Leica | **TS60** ⭐ | Captivate | **WLAN FTP project transfer** ✨ | Phone hotspot/WLAN |
| Leica | TS16, TS50, MS60 | Captivate | GeoCOM code retained, project transfer pending | SPP + WLAN |
| Leica | iCR80 | iCON | GSI-Online | SPP + CCD6 400m |
| Sokkia | SX/CX/iM/iX Series | — | Passive | SPP |
| Topcon | ES/MS/GT Series | — | Passive | SPP |
| South | NTS Series | — | Passive | SPP |
| CHC | HTS Series | — | Passive | SPP |
| Hi-Target | ZTS/iTrack/iAngle Series | — | Passive | SPP |

---

## 🏗️ Architecture

### Protocol Abstraction Layer

```
BluetoothConnectionManager (read + write)
    ↓
ProtocolHandler (interface)
    ├─ PassiveStreamProtocolHandler (GSI-Online)
    └─ GeoComProtocolHandler (GeoCOM RPC)
        ↓
    GeoComClient (Mutex + timeout protection)
        ↓
CollectorViewModel (protocol-aware)
    ↓
MeasurementRecord (extended with GeoCOM fields)
    ↓
ExportWriter (multi-format export)
```

### Tech Stack

- **Language**: Kotlin 100%
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Single Activity
- **Database**: Room (SQLite)
- **Concurrency**: Kotlin Coroutines + StateFlow
- **Bluetooth**: Classic Bluetooth SPP
- **Testing**: JUnit + Robolectric (80%+ coverage)

### Code Quality

- ✅ 80%+ test coverage
- ✅ 11 unit test classes
- ✅ Integration and UI tests
- ✅ Rigorous code review
- ✅ TDD development process

---

## 📖 Documentation

- [CHANGELOG.md](CHANGELOG.md) - Detailed version history
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [TESTING.md](TESTING.md) - Hardware testing guide
- [ROADMAP.md](ROADMAP.md) - Product roadmap
- [PROJECT_STATUS.md](PROJECT_STATUS.md) - Project status report

---

## 🤝 Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Ways to Contribute

- 🐛 Report bugs
- 💡 Suggest new features
- 📝 Improve documentation
- 🔧 Submit code
- ⭐ Star the project

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

## 🙏 Acknowledgments

This project was developed through AI-assisted collaboration:
- **Architecture & Planning**: Claude (Opus 4.6)
- **Backend Implementation**: Codex (GPT-5.4)
- **Frontend Implementation**: Gemini (3.1 Pro)

Thanks to the surveying community for feedback and feature requests!

---

## 📞 Contact

- **Issues**: [GitHub Issues](https://github.com/unf0rgettable-h/blue_tooth/issues)
- **Discussions**: [GitHub Discussions](https://github.com/unf0rgettable-h/blue_tooth/discussions)
- **Email**: Contact via GitHub

---

<div align="center">

**If this project helps you, please give it a ⭐ Star!**

Made with ❤️ for surveyors worldwide

</div>
