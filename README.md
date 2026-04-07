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

**SurvLink 是首个支持徕卡 Captivate 系列（TS60/TS16/TS50/MS60）GeoCOM 双向通信协议的开源 Android 应用。**

### 核心优势

- 🎯 **真正的实时测量** - 支持 GeoCOM 双向协议，TS60 可实时触发测量并显示结果
- 📊 **专业数据导出** - CSV 导出包含弧度、度、gon 三种角度单位
- 🔄 **完全向后兼容** - FlexLine 系列（TS02-TS13）继续使用 GSI-Online 协议
- 🧪 **高质量代码** - 80%+ 测试覆盖率，经过严格的单元测试和集成测试
- 🆓 **完全开源** - MIT 许可证，代码透明，社区驱动

### 解决的痛点

❌ **之前**: TS60 用户无法实时预览测量数据，文件导出不可用  
✅ **现在**: 完整的 GeoCOM 协议支持，连续轮询或单次测量，实时角度显示

---

## 🚀 快速开始

### 下载安装

1. 前往 [Releases 页面](https://github.com/unf0rgettable-h/blue_tooth/releases/latest)
2. 下载 `survlink.apk`
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

#### Captivate 仪器（TS60/TS16/TS50/MS60）⭐ 新功能
1. 打开应用 → 蓝牙页面
2. 选择 Captivate 仪器（如 TS60）
3. 连接配对的设备
4. 数据页面 → 看到 GeoCOM 控制面板：
   - **连续轮询**: 点击"Start Auto"，每 2 秒自动测量
   - **单次测量**: 点击"Measure Once"，按需触发
5. 实时显示：水平角、垂直角、斜距
6. 导出 CSV：包含 rad/deg/gon 三种单位

---

## 📱 功能特性

### GeoCOM 协议支持 ✨ v1.4.0 新增

<table>
<tr>
<td width="50%">

**双向通信**
- App 可发送命令到仪器
- 支持连续轮询模式（2秒间隔）
- 支持单次测量模式
- Mutex 保护的命令序列化

</td>
<td width="50%">

**实时显示**
- 水平角（Hz）精确到 0.0001°
- 垂直角（V）精确到 0.0001°
- 斜距精确到 0.001m
- 格式化显示，易于阅读

</td>
</tr>
<tr>
<td width="50%">

**多单位导出**
- CSV 包含三种角度单位
- 弧度（radians）
- 度（degrees）
- gon（百分度）

</td>
<td width="50%">

**协议抽象**
- ProtocolHandler 接口
- 自动协议选择
- 向后兼容 FlexLine
- 易于扩展新协议

</td>
</tr>
</table>

### 支持的仪器

| 品牌 | 型号 | 固件 | 协议 | 蓝牙 |
|------|------|------|------|------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | GSI-Online | SPP ~15m |
| Leica | **TS16, TS50, TS60** ⭐ | Captivate | **GeoCOM** ✨ | SPP + RH16 400m |
| Leica | **MS60** ⭐ | Captivate | **GeoCOM** ✨ | SPP + RH16 400m |
| Leica | iCR80 | iCON | GSI-Online | SPP + CCD6 400m |
| Sokkia | SX/CX/iM/iX 系列 | — | 被动流 | SPP |
| Topcon | ES/MS/GT 系列 | — | 被动流 | SPP |
| South | NTS 系列 | — | 被动流 | SPP |
| CHC | HTS 系列 | — | 被动流 | SPP |
| Hi-Target | ZTS/iTrack/iAngle 系列 | — | 被动流 | SPP |

---

## 🏗️ 技术架构

### 协议抽象层

```
BluetoothConnectionManager (读 + 写)
    ↓
ProtocolHandler (接口)
    ├─ PassiveStreamProtocolHandler (GSI-Online)
    └─ GeoComProtocolHandler (GeoCOM RPC)
        ↓
    GeoComClient (Mutex + 超时保护)
        ↓
CollectorViewModel (协议感知)
    ↓
MeasurementRecord (扩展 GeoCOM 字段)
    ↓
ExportWriter (多格式导出)
```

### 技术栈

- **语言**: Kotlin 100%
- **UI**: Jetpack Compose
- **架构**: MVVM + 单 Activity
- **数据库**: Room (SQLite)
- **并发**: Kotlin Coroutines + StateFlow
- **蓝牙**: Classic Bluetooth SPP
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

**Professional Total Station Bluetooth Data Collector**

## 🌟 Why SurvLink?

**SurvLink is the first open-source Android app supporting Leica Captivate series (TS60/TS16/TS50/MS60) GeoCOM bidirectional communication protocol.**

### Key Advantages

- 🎯 **True Real-Time Measurement** - GeoCOM bidirectional protocol support, TS60 can trigger measurements and display results in real-time
- 📊 **Professional Data Export** - CSV export includes angles in radians, degrees, and gon
- 🔄 **Fully Backward Compatible** - FlexLine series (TS02-TS13) continues using GSI-Online protocol
- 🧪 **High-Quality Code** - 80%+ test coverage with rigorous unit and integration tests
- 🆓 **Fully Open Source** - MIT License, transparent code, community-driven

### Problem Solved

❌ **Before**: TS60 users couldn't preview measurements in real-time, file export unavailable  
✅ **Now**: Full GeoCOM protocol support, continuous polling or single-shot measurement, real-time angle display

---

## 🚀 Quick Start

### Download & Install

1. Go to [Releases page](https://github.com/unf0rgettable-h/blue_tooth/releases/latest)
2. Download `survlink.apk`
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

#### Captivate Instruments (TS60/TS16/TS50/MS60) ⭐ New Feature
1. Open app → Bluetooth page
2. Select Captivate instrument (e.g., TS60)
3. Connect to paired device
4. Data page → See GeoCOM control panel:
   - **Continuous Polling**: Click "Start Auto", auto-measure every 2 seconds
   - **Single-Shot**: Click "Measure Once", trigger on demand
5. Real-time display: Horizontal angle, Vertical angle, Slope distance
6. Export CSV: Includes rad/deg/gon units

---

## 📱 Features

### GeoCOM Protocol Support ✨ New in v1.4.0

<table>
<tr>
<td width="50%">

**Bidirectional Communication**
- App can send commands to instrument
- Continuous polling mode (2s interval)
- Single-shot measurement mode
- Mutex-protected command serialization

</td>
<td width="50%">

**Real-Time Display**
- Horizontal angle (Hz) to 0.0001°
- Vertical angle (V) to 0.0001°
- Slope distance to 0.001m
- Formatted display, easy to read

</td>
</tr>
<tr>
<td width="50%">

**Multi-Unit Export**
- CSV includes three angle units
- Radians
- Degrees
- Gon (gradians)

</td>
<td width="50%">

**Protocol Abstraction**
- ProtocolHandler interface
- Automatic protocol selection
- Backward compatible with FlexLine
- Easy to extend new protocols

</td>
</tr>
</table>

### Supported Instruments

| Brand | Models | Firmware | Protocol | Bluetooth |
|-------|--------|----------|----------|-----------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | GSI-Online | SPP ~15m |
| Leica | **TS16, TS50, TS60** ⭐ | Captivate | **GeoCOM** ✨ | SPP + RH16 400m |
| Leica | **MS60** ⭐ | Captivate | **GeoCOM** ✨ | SPP + RH16 400m |
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
