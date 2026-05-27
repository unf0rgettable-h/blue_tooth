# SurvLink V1.6.0

## 重点更新

- TS60 / Captivate 新增 WLAN/FTP 完整项目文件接收。
- 手机端启动 FTP 服务后，TS60 可通过 `Settings > Tools > FTP data transfer` 上传 job/dbx/csv/xml/dxf/zip。
- TS09/FlexLine 继续走已验证的经典蓝牙导入 channel。
- TS60 的蓝牙 RFCOMM 保留为实验诊断，不再作为完整项目文件主路径。
- 接收到的 TS60 项目文件可停止后打包为 ZIP，并分享或保存到下载目录。

## 使用步骤

1. 手机开启热点。
2. TS60 连接该手机热点。
3. APP 选择 Leica TS60，进入数据页，点击 `启动WLAN项目接收`。
4. APP 显示 FTP 地址、端口、用户名和密码。
5. TS60 打开 `Settings > Tools > FTP data transfer`，输入 APP 显示的信息并上传项目文件。
6. APP 点击 `停止并打包项目`，生成 ZIP 后分享或保存。

## 安装包

- APK：`survlink-v1.6.0-signed.apk`
- Android：8.0+ / API 26+
- 版本：`versionName=1.6.0`，`versionCode=11`

## 验证

- 已通过：`:app:testDebugUnitTest`
- 已通过：`:app:compileDebugKotlin`
- 已通过：`:app:compileDebugAndroidTestKotlin`
- 已通过：`:app:assembleRelease`
- 待补：`:app:connectedDebugAndroidTest` 当前卡在 ADB 安装阶段，0 个测试执行，错误为 `ShellCommandUnresponsiveException`
- 待补：真机安装验证；当前 `adb devices` 为空，需要手机重新连接后执行
