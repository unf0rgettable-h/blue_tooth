# 原生 Android 全站仪蓝牙采集

原仓库是一个 MIT App Inventor 项目，用于全站仪测量数据接收和检定计算。当前主线已经切换为 `Android Studio + Kotlin + Jetpack Compose` 的原生 Android 应用，V1 只保留现场采集真正需要的能力：

- 选择仪器品牌/型号（徕卡 FlexLine/Captivate/iCON 全系、索佳、拓普康、南方、华测、中海达）
- 搜索附近蓝牙设备
- 设备配对与已配对设备连接（支持断开后自由切换设备）
- 经典蓝牙文本流实时接收
- 仪器存储数据原始文件导入（自动检测 XML/GSI/DXF/CSV/TXT 格式，3 秒静默自动完成）
- 手机端当前会话持久化与预览（预览文本可选中复制）
- 当前会话导出为 `CSV` 或 `TXT`
- 导入的文件可直接分享
- 通过 Android 系统分享或保存到本地下载目录

以下旧能力已从原生方案中移除，不再作为产品目标：

- 加常数计算
- 重复性计算
- 真值输入
- 手动补录
- 文件导入
- 传统检定报告导出

## 适用对象

适合需要继续开发、构建、验证这个原生 Android 采集 App 的开发者。  
如果你只想查看旧 MIT App Inventor 参考源，请直接看 `src/appinventor/ai_xiakele341/blue_tooth/`。

## 当前状态

- 原生 Android 主工程已经可构建
- 单屏 UI 与状态机已经落地
- CSV/TXT 导出与系统分享链路已经落地
- legacy MIT App Inventor 源码仍保留在仓库中，仅作为行为参考

当前环境下已验证：

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"`
- `./gradlew :app:assembleDebugAndroidTest`

当前环境下未完成的运行级验证：

- `./gradlew :app:connectedDebugAndroidTest`
  原因：没有连接中的 Android 设备或模拟器

## 技术栈

- Kotlin
- Android Studio / Gradle
- Jetpack Compose
- Room
- Kotlin Coroutines / StateFlow
- Classic Bluetooth (`BluetoothAdapter`, `BluetoothDevice`, `BluetoothSocket`)
- FileProvider

当前 Android 配置：

- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 26`

## 快速开始

### 1. 环境要求

- JDK 17
- Android SDK
- Gradle Wrapper 可用

示例环境变量：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
```

### 2. 构建 Debug APK

```bash
./gradlew :app:assembleDebug
```

### 3. 运行关键验证

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"
./gradlew :app:assembleDebugAndroidTest
```

如果你有 Android 12+ 真机或模拟器，再执行：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 目录结构

```text
app/                                      原生 Android 主工程
  src/main/java/com/unforgettable/
    bluetoothcollector/
      data/                               蓝牙、存储、导出、分享
      domain/                             领域模型
      ui/                                 单屏 UI、状态机、主题

  src/test/                               JVM 单测
  src/androidTest/                        Compose / Android instrumentation tests

docs/superpowers/specs/                   原生方案设计文档
docs/superpowers/plans/                   原生方案实施计划

src/appinventor/ai_xiakele341/blue_tooth/ legacy MIT App Inventor 源码参考
youngandroidproject/                      legacy App Inventor 项目元数据
assets/                                   legacy 资源
```

## 单屏 UI 说明

当前界面收敛为一个主屏：

- 左上：仪器品牌与型号选择
- 右上：附近设备与已配对设备
- 中部：搜索、连接、开始/停止接收、清空、导出
- 下部：连接状态、接收状态、接收条数、当前会话预览列表

导出流程：

1. 当前会话在手机端持久化
2. 用户点击导出
3. 选择 `CSV` 或 `TXT`
4. 应用在手机端生成文件
5. 调起 Android 系统分享面板

## 关键行为约束

- 只允许连接已配对设备
- 蓝牙搜索与连接互斥
- 当前会话可在应用重启后恢复数据
- 应用重启后不会恢复“已连接”或“接收中”状态
- 清空当前会话仅允许在断开状态下执行
- 导出范围仅限当前会话

## 旧 MIT 源码的定位

旧 MIT App Inventor 源码仍保留，但它的角色已经变成：

- 仪器品牌/型号目录参考
- 旧蓝牙链路行为参考
- 遗留产品形态参考

它不再代表当前主产品架构，也不再是推荐的继续开发入口。

## 相关文档

- 设计文档：`docs/superpowers/specs/2026-03-25-android-native-total-station-collector-design.md`
- 实施计划：`docs/superpowers/plans/2026-03-30-native-android-total-station-collector.md`

## 当前已知限制

- 当前仓库环境没有可用 Android 设备或模拟器，`connectedDebugAndroidTest` 无法在本地完成
- 蓝牙搜索、系统配对、真实接收链路、系统分享面板，仍需要在 Android 12+ 真机或模拟器上做最终人工验收
