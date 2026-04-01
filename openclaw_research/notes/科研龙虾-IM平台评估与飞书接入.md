---
类型: 接入评估
状态: 草案
最后更新: 2026-03-31
关联方案:
  - "[[科研龙虾-总览索引]]"
  - "[[科研龙虾-第一阶段实施计划]]"
tags:
  - OpenClaw
  - 科研龙虾
  - 飞书
  - IM
---
# 科研龙虾-IM平台评估与飞书接入

返回入口：[[科研龙虾-总览索引]]

## 一、结论

如果优先考虑中国大学生、研究生的日常沟通习惯，以及 OpenClaw 官方当前的通道成熟度，`飞书 / Feishu` 是第一阶段最值得优先接入的 IM 平台。

## 二、为什么优先飞书

### 1. OpenClaw 官方已支持

截至 `2026-03-31`，本机已安装的 `OpenClaw 2026.3.28` 在运行时可以识别 Feishu 能力：

- `Support: chatTypes=direct,channel reactions edit reply threads media`
- `Actions: send, broadcast`
- `Status: not configured, enabled`

这说明飞书通道已经进入当前版本的可用能力面，而不是需要额外自行开发协议适配。

### 2. 连接方式适合服务器

官方 Feishu 文档说明该通道使用 `WebSocket long connection` 来接收事件，不需要暴露公网 webhook URL。这一点非常重要，因为科研龙虾第一阶段本来就应该采用：

- Gateway 仅绑定 loopback
- 通过 SSH / Tailscale 等受控方式管理

### 3. 更贴中国使用场景

对于中国学生群体而言：

- Telegram 并不是主流
- WhatsApp 在中国科研校园环境里并不自然
- WeChat 虽然用户基数更大，但 OpenClaw 当前公开文档里对应的是第三方插件形态，且描述为 `private chats only`

相比之下，飞书更适合：

- 一对一沟通
- 小组讨论
- 后续可能的团队试点
- 文档 / 协作生态联动

## 三、飞书相对其他通道的判断

### 飞书 vs Telegram

- Telegram 接入更轻，但不符合中国日常主流工作流
- 飞书更贴近真实校园 / 实验室沟通环境

### 飞书 vs WeChat

- WeChat 更常用，但 OpenClaw 官方文档里给的是外部插件入口，不是同等成熟的官方主通道
- 飞书官方文档更完整，权限、配对、群策略、WebSocket 接入都更清晰

### 飞书 vs Discord / Slack

- 这些更适合国际化开发者社区，不是中国大学生 / 研究生默认首选

## 四、已经确认的事实

### 环境状态

截至 `2026-03-31`：

- 服务器已装 `Node v24.14.1`
- 服务器已装 `OpenClaw 2026.3.28`
- APT 已切到阿里云 Ubuntu 镜像
- npm 已切到 `https://registry.npmmirror.com/`

### OpenClaw 官方版本核实

- GitHub 最新稳定版：`v2026.3.28`
- npm 包版本：`2026.3.28`

### 官方 Node 建议

OpenClaw 官方 getting started 文档写明：

- `Node 24 recommended`
- `Node 22.14+ also supported`

同时还发现一个细节：

- OpenClaw 官方 `install-cli.sh` 默认 Node 版本仍是 `22.22.0`
- 因此本次没有直接使用默认脚本，而是先显式安装 `Node 24.14.1`

## 五、飞书接入还缺什么

当前还不能完成真正连通，原因不是 OpenClaw 不支持，而是还缺飞书侧凭据与模型侧凭据。

### 飞书侧必须补齐

- `App ID`
- `App Secret`
- 飞书应用发布
- Bot capability 开启
- 事件订阅 `im.message.receive_v1`

### 模型侧必须补齐

科研龙虾最终要能回答问题，还需要至少一个模型 provider 的 API key。

## 六、推荐的第一阶段飞书安全配置

在拿到飞书凭据后，建议用以下默认原则：

- `connectionMode: websocket`
- `dmPolicy: pairing`
- `groupPolicy: disabled`
- `requireMention: true`
- `typingIndicator: false`
- `resolveSenderNames: false`

理由：

- 先把 DM 路径跑通
- 先避免群聊噪声与越权
- 先降低 Feishu API 配额消耗

## 七、当前建议

### 立即执行

- 保持飞书作为第一接入通道
- 暂不接 WeChat 主通道
- 等拿到飞书 app 凭据后再完成 `channels add`

### 后续再做

- 飞书群聊策略
- 飞书文档联动
- 多账号与备用 bot

## 八、相关链接

- [[科研龙虾-总览索引]]
- [[科研龙虾-第一阶段实施计划]]
- [[科研龙虾-接口与工具契约]]
