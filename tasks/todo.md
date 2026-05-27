# APP 1.6.0 TS60 WLAN Project Transfer

- [x] Spec 现状分析确认
- [x] Spec 功能点确认
- [x] Spec 风险与决策确认
- [x] HARD-GATE 用户确认后开始实现
- [x] 实现 TS60 WLAN/FTP 项目文件接收链路
- [x] 执行单元测试、编译和真机验证

## Implementation Plan

### Task 1: 传输能力模型改成独立 channel

**目标**：TS09/FlexLine 继续走经典蓝牙 channel；TS60 1.6.0 主入口改为 WLAN FTP project transfer channel，蓝牙 RFCOMM 只保留实验诊断。

**文件**
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/InstrumentTransferCapability.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportExecutionMode.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistry.kt`
- 修改：`app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`
- 修改：`app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiStateTest.kt`

- [ ] 新增 `TransferRoute.FTP_WLAN_PROJECT_TRANSFER`
- [ ] 新增 `TransferConfidence.VERIFIED_APP_FTP_RECEIVER`
- [ ] 新增 `ImportExecutionMode.FTP_SERVER`
- [ ] TS60 profile 改为 `SUPPORTED + FTP_SERVER + FTP_WLAN_PROJECT_TRANSFER`
- [ ] TS60 文案改为“启动WLAN项目接收”，明确 WLAN/FTP 是主路径，蓝牙完整文件传输待验证
- [ ] 更新 profile 和 UI state 单元测试，确保 TS09 仍是 `CLIENT_STREAM`

### Task 2: 文件格式与项目包元数据

**目标**：APP 能表示单个导出文件，也能表示 TS60 通过 FTP 上传的一组项目文件，并能把整组文件打包为 ZIP 后分享/保存。

**文件**
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedFileInfo.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStore.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ProjectTransferArchiveWriter.kt`
- 修改：`app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStoreTest.kt`
- 新建：`app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ProjectTransferArchiveWriterTest.kt`

- [ ] 给 `ImportedFileFormat` 增加 `ZIP`、`DBX`，并按扩展名优先识别
- [ ] `ImportedFileInfo` 增加可选字段：`sourceChannel`、`fileCount`、`totalSizeBytes`
- [ ] `ImportedArtifactStore` 继续兼容旧 metadata，没有新字段时按单文件恢复
- [ ] `ProjectTransferArchiveWriter` 使用 `ZipOutputStream` 将 FTP 根目录打成一个 ZIP
- [ ] 测试 ZIP 保留相对路径，且拒绝把根目录外文件写进包

### Task 3: 实现本地 FTP 接收 channel

**目标**：手机端开启一个标准 FTP server，TS60/Captivate 可通过手机热点连接并上传 job/dbx/csv/xml/dxf/zip 等项目文件。

**文件**
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/ftp/FtpReceiveState.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/ftp/FtpServerConfig.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/ftp/FtpServerController.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/ftp/NetworkAddressProvider.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/data/ftp/LocalFtpServerManager.kt`
- 新建：`app/src/test/java/com/unforgettable/bluetoothcollector/data/ftp/NetworkAddressProviderTest.kt`
- 新建：`app/src/test/java/com/unforgettable/bluetoothcollector/data/ftp/LocalFtpServerManagerTest.kt`

- [ ] `FtpReceiveState` 覆盖 Idle / Starting / Running / Receiving / Completed / Failed / Stopped
- [ ] 默认控制端口使用 `2121`，用户名 `survlink`，密码运行时生成 6 位数字
- [ ] `NetworkAddressProvider` 自动枚举非 loopback IPv4，并保留手动 IP 文案兜底
- [ ] `LocalFtpServerManager` 支持 `USER`、`PASS`、`SYST`、`FEAT`、`PWD`、`CWD`、`CDUP`、`MKD`、`TYPE`、`PASV`、`EPSV`、`PORT`、`LIST`、`NLST`、`STOR`、`APPE`、`SIZE`、`MDTM`、`NOOP`、`QUIT`
- [ ] 所有 FTP 路径都经过根目录约束，拒绝 `../` 越界写入
- [ ] 每次 `STOR/APPE` 成功后更新文件列表、总字节数和状态
- [ ] 单元测试用 JVM `Socket` 模拟 FTP 客户端，验证登录、PASV、STOR、LIST 和越界拒绝

### Task 4: ViewModel 接入 FTP channel

**目标**：TS60 点击主导入按钮时启动/停止 FTP server，而不是触发蓝牙 RFCOMM 实验监听；接收到的项目文件进入现有导入文件展示、分享、保存流程。

**文件**
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorContracts.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRouteDependencies.kt`
- 修改：`app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] 在 `CollectorUiState` 加入 `ftpReceiveState`、`ftpEndpointText`、`ftpReceivedFiles`
- [ ] 在 `CollectorViewModel` 注入 `FtpServerController` 和 `ProjectTransferArchiveWriter`
- [ ] `onStartImportRequested()` 对 `FTP_SERVER` 分支启动 FTP，不再要求蓝牙连接状态
- [ ] 新增 `onStopFtpReceiveRequested()`：停止 FTP，若接收了多文件则打包 ZIP 并保存 `ImportedFileInfo`
- [ ] `onShareImportedFile()` 对多文件项目优先分享 ZIP
- [ ] 单元测试覆盖 TS60 不要求蓝牙连接、FTP 状态进入 UI、停止后生成 ZIP、TS09 导入行为不变

### Task 5: UI 加入 TS60 WLAN 项目接收面板

**目标**：用户能按 APP 屏幕直接完成“手机热点 -> TS60 FTP data transfer -> 上传项目文件”的流程，不需要外部说明文档。

**文件**
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/DataCommandPanel.kt`
- 新建：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/FtpProjectTransferPanel.kt`
- 修改：`app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/MeasurementPreviewPanel.kt`
- 修改：`app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`

- [ ] 数据页显示 FTP 地址、端口、用户名、密码、接收目录摘要
- [ ] 显示 Captivate 操作步骤：`Settings > Tools > FTP data transfer`
- [ ] 显示已接收文件数量、总大小、最近文件名
- [ ] 提供“停止并打包项目”“分享项目包”“保存到下载”
- [ ] 蓝牙页面保留 TS09 使用习惯，TS60 不强迫用户配对蓝牙
- [ ] Compose 测试验证 TS60 数据页出现 FTP 面板，TS02/TS09 不出现该面板

### Task 6: Android 配置、版本和分享路径

**目标**：1.6.0 APK 能进行 WLAN/FTP 通信，并能分享/保存接收到的项目 ZIP。

**文件**
- 修改：`app/src/main/AndroidManifest.xml`
- 修改：`app/src/main/res/xml/file_paths.xml`
- 修改：`app/build.gradle.kts`

- [ ] 增加 `android.permission.INTERNET`
- [ ] 增加 `android.permission.ACCESS_NETWORK_STATE`
- [ ] `file_paths.xml` 覆盖 `imports/` 内项目 ZIP 和 FTP 接收目录
- [ ] `versionCode` 从 `10` 升到 `11`
- [ ] `versionName` 从 `1.5.0` 升到 `1.6.0`

### Task 7: 验证、安装、提交、推送

**命令**
- [x] `./gradlew :app:testDebugUnitTest`
- [x] `./gradlew :app:compileDebugKotlin`
- [x] `./gradlew :app:compileDebugAndroidTestKotlin`
- [x] `./gradlew :app:connectedDebugAndroidTest`
- [x] `./gradlew :app:assembleDebug`
- [x] `adb install -r app/build/outputs/apk/release/app-release.apk`
- [x] `adb shell dumpsys package com.unforgettable.bluetoothcollector | rg "versionName|versionCode|lastUpdateTime"`
- [x] `git status --short`
- [x] `git add ... && git commit -m "Add TS60 WLAN project transfer"`
- [x] `git push origin refactor/ui-ts60-agent-maintainability`

## Review

- 已完成 TS60 WLAN/FTP 项目文件接收实现、README/CHANGELOG/Release notes 草稿、1.6.0 版本号更新。
- 已验证 `:app:testDebugUnitTest :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` 通过。
- 已验证 `:app:assembleRelease` 通过，release APK metadata 为 `versionName=1.6.0`、`versionCode=11`。
- 设备重新授权后，已验证 `:app:connectedDebugAndroidTest` 通过，PJZ110 执行 6 个测试。
- 已重新安装 release APK 到 PJZ110，包信息为 `versionName=1.6.0`、`versionCode=11`。
