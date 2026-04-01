#!/usr/bin/env bash
set -euo pipefail

ps_script=$(cat <<'POWERSHELL'
$ErrorActionPreference = 'Stop'

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Content
    )

    $directory = Split-Path -Parent $Path
    if ($directory -and -not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function New-VaultId {
    -join (1..16 | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
}

$vaultPath = 'D:\obsidian_claude\服务器总网关'
$appDataDir = 'C:\Users\91776\AppData\Roaming\obsidian'
$obsidianJsonPath = Join-Path $appDataDir 'obsidian.json'

$directories = @(
    $vaultPath,
    (Join-Path $vaultPath '.obsidian'),
    (Join-Path $vaultPath '00-总控台'),
    (Join-Path $vaultPath '01-服务器'),
    (Join-Path $vaultPath '01-服务器\在役'),
    (Join-Path $vaultPath '01-服务器\归档'),
    (Join-Path $vaultPath '02-Agent规则'),
    (Join-Path $vaultPath '03-操作记录'),
    (Join-Path $vaultPath '03-操作记录\2026'),
    (Join-Path $vaultPath '04-模板'),
    (Join-Path $vaultPath '05-索引与看板'),
    (Join-Path $vaultPath '90-归档')
)

foreach ($dir in $directories) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$corePlugins = @'
{
  "file-explorer": true,
  "global-search": true,
  "switcher": true,
  "graph": true,
  "backlink": true,
  "canvas": true,
  "outgoing-link": true,
  "tag-pane": true,
  "properties": true,
  "page-preview": true,
  "daily-notes": true,
  "templates": true,
  "command-palette": true,
  "bookmarks": true,
  "outline": true,
  "word-count": true,
  "file-recovery": true,
  "bases": true,
  "sync": false,
  "publish": false,
  "workspaces": false,
  "webviewer": false
}
'@

$appJson = @'
{
  "useMarkdownLinks": false,
  "showInlineTitle": true,
  "attachmentFolderPath": "90-归档/附件"
}
'@

Write-Utf8NoBom -Path (Join-Path $vaultPath '.obsidian\app.json') -Content $appJson
Write-Utf8NoBom -Path (Join-Path $vaultPath '.obsidian\appearance.json') -Content "{}`n"
Write-Utf8NoBom -Path (Join-Path $vaultPath '.obsidian\community-plugins.json') -Content "[]`n"
Write-Utf8NoBom -Path (Join-Path $vaultPath '.obsidian\core-plugins.json') -Content $corePlugins

$obsidianState = Get-Content -LiteralPath $obsidianJsonPath -Raw | ConvertFrom-Json
if (-not $obsidianState.vaults) {
    $obsidianState | Add-Member -NotePropertyName vaults -NotePropertyValue ([ordered]@{})
}

$existingVaultId = $null
foreach ($prop in $obsidianState.vaults.PSObject.Properties) {
    if ($prop.Value.path -eq $vaultPath) {
        $existingVaultId = $prop.Name
        break
    }
}

if (-not $existingVaultId) {
    do {
        $existingVaultId = New-VaultId
    } while ($obsidianState.vaults.PSObject.Properties.Name -contains $existingVaultId)
}

$vaultState = [ordered]@{
    path = $vaultPath
    ts   = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    open = $false
}

if ($obsidianState.vaults.PSObject.Properties.Name -contains $existingVaultId) {
    $obsidianState.vaults.$existingVaultId = $vaultState
} else {
    $obsidianState.vaults | Add-Member -NotePropertyName $existingVaultId -NotePropertyValue $vaultState
}

$obsidianStateJson = $obsidianState | ConvertTo-Json -Depth 8
Write-Utf8NoBom -Path $obsidianJsonPath -Content ($obsidianStateJson + "`n")

$windowStatePath = Join-Path $appDataDir "$existingVaultId.json"
if (-not (Test-Path -LiteralPath $windowStatePath)) {
    Write-Utf8NoBom -Path $windowStatePath -Content '{"x":220,"y":120,"width":1600,"height":980,"isMaximized":false,"devTools":false,"zoom":0}'
}

$notes = @{
    '00-总控台\首页总控台.md' = @'
---
aliases:
  - 总控台
  - 运维总控台
类型: MOC
状态: 启用
最后更新: 2026-03-30
tags:
  - 总控台
  - MOC
  - 运维
---
# 首页总控台

这是多服务器、多 Agent 的统一运维知识仓库。目录只承担承载作用，真正的导航入口是本页、索引页和每台服务器主页。

## 快速入口

- [[服务器索引]]
- [[风险与待办索引]]
- [[最近操作总览]]
- [[Agent 工作准则]]
- [[记录规范]]
- [[安全边界与凭据规则]]
- [[模板-服务器主页]]
- [[模板-操作记录]]

## 当前重点服务器

- [[服务器-118.145.222.97-主机档案]]

## 当前工作面

- 当前已完成：总网关仓库初始化、中文目录骨架、索引页、规则页、模板页、首台服务器主页。
- 当前待进行：连接 [[服务器-118.145.222.97-主机档案]] 做首次巡检和基线建设。

## 建议接手顺序

1. 先看 [[Agent 工作准则]]
2. 再看 [[服务器索引]]
3. 打开目标服务器主页
4. 任何动作都要补一条操作记录，优先套用 [[模板-操作记录]]

## 图谱使用提示

- 观察 [[首页总控台]]、[[服务器索引]]、服务器主页之间的主干关系。
- 新笔记至少链接到一个索引页和一个对象页，避免图谱出现孤点。
- 涉及单台服务器的风险、待办、变更和事件，优先落到对应服务器主页，再向外扩散。

## 当前仓库边界

- 允许记录低敏运维信息，例如账号名、端口、指纹、密钥路径、访问方式变化。
- 禁止记录明文密码、私钥全文、恢复码全文和可直接复用的高敏秘密。
- 若不确定 Obsidian 用法，先查官方帮助或高质量案例，再动手修改结构。
'@
    '05-索引与看板\服务器索引.md' = @'
---
aliases:
  - 服务器总索引
类型: 索引
状态: 启用
最后更新: 2026-03-30
tags:
  - 索引
  - 服务器
---
# 服务器索引

返回入口：[[首页总控台]]

## 在役服务器

- [[服务器-118.145.222.97-主机档案]]

## 归档服务器

- 暂无

## 使用规则

- 每台服务器必须有且仅有一张服务器主页。
- 服务器主页必须链接回本页和 [[首页总控台]]。
- 具体动作写入 [[最近操作总览]] 所链接的操作记录，不要把过程直接堆在索引页。
'@
    '05-索引与看板\风险与待办索引.md' = @'
---
类型: 索引
状态: 启用
最后更新: 2026-03-30
tags:
  - 索引
  - 风险
  - 待办
---
# 风险与待办索引

返回入口：[[首页总控台]]

## 当前风险

- [[服务器-118.145.222.97-主机档案#当前风险]]

## 当前待办

- [[服务器-118.145.222.97-主机档案#当前待办]]

## 使用规则

- 风险先写到服务器主页，再由本页做汇总。
- 待办要尽量写成下一步能执行的动作，而不是模糊目标。
'@
    '05-索引与看板\最近操作总览.md' = @'
---
类型: 索引
状态: 启用
最后更新: 2026-03-30
tags:
  - 索引
  - 操作记录
---
# 最近操作总览

返回入口：[[首页总控台]]

## 最近记录

- [[记录-2026-03-30-118.145.222.97-接管准备]]

## 使用规则

- 新记录按日期和目标对象命名。
- 记录里必须写清背景、动作、验证、影响和后续事项。
- 记录完成后，要把链接补回相关服务器主页。
'@
    '02-Agent规则\Agent 工作准则.md' = @'
---
aliases:
  - Agent 规则
  - 接手准则
类型: 规则
状态: 启用
最后更新: 2026-03-30
tags:
  - Agent
  - 规则
  - 交接
---
# Agent 工作准则

返回入口：[[首页总控台]]

## 接手顺序

1. 先看 [[首页总控台]]
2. 再看 [[服务器索引]]
3. 打开目标服务器主页
4. 补读相关操作记录、风险和待办
5. 动手前先明确要补哪一条记录

## 写作规则

- 优先使用 `[[内部链接]]` 组织上下文。
- 需要结构化信息时使用 Properties，不要把所有元信息塞到正文里。
- 每条关键记录至少链接到一个索引页和一个对象页。
- 标题尽量稳定、可检索、可审计，避免使用含糊命名。

## Obsidian 使用基准

- 积极使用 `[[链接]]`、别名、属性、反向链接和图谱视图。
- 若不确定用法，先查官方帮助或高质量案例，再修改结构或模板。
- 图谱视图应该能清楚看到总控台、索引页、服务器主页和记录之间的关系。

## 记录纪律

- 任何会影响服务器状态、登录方式、安全边界或后续交接的动作，都必须留操作记录。
- 任何记录都应包含背景、动作、验证、影响、风险与回滚点、后续事项。
- 同一台服务器的长期事实优先写进服务器主页，短期过程写进操作记录。

## 安全纪律

- 遵守 [[安全边界与凭据规则]]。
- 可以写低敏信息，不要写明文密码和私钥全文。
- 如果必须提凭据，只记录位置、状态、责任人或指纹。
'@
    '02-Agent规则\记录规范.md' = @'
---
类型: 规则
状态: 启用
最后更新: 2026-03-30
tags:
  - 规则
  - 记录
  - 模板
---
# 记录规范

返回入口：[[首页总控台]]

## 命名规范

- 服务器主页：`服务器-<IP或主机名>-主机档案`
- 操作记录：`记录-YYYY-MM-DD-<目标>-<动作>`
- 变更记录：`变更-YYYY-MM-DD-<目标>-<动作>`
- 事件记录：`事件-YYYY-MM-DD-<目标>-<问题>`

## 属性规范

- 默认 `tags` 使用英文键名，其他字段可用中文。
- 涉及 Obsidian 内部链接的属性值要写成带引号的 `[[链接]]`。
- 字段只保留对检索、交接、审计真正有用的部分，避免过度元数据化。

## 链接规范

- 每台服务器主页必须链接回 [[服务器索引]] 和 [[首页总控台]]。
- 每条操作记录必须链接回对应服务器主页。
- 风险与待办优先沉淀在服务器主页，再由索引页做汇总。

## 推荐字段

### 服务器主页

- `类型`
- `状态`
- `环境`
- `系统`
- `公网地址`
- `最后更新`
- `责任Agent`

### 操作记录

- `类型`
- `日期`
- `目标服务器`
- `执行Agent`
- `操作类别`
- `状态`
'@
    '02-Agent规则\安全边界与凭据规则.md' = @'
---
类型: 规则
状态: 启用
最后更新: 2026-03-30
tags:
  - 安全
  - 凭据
  - 规则
---
# 安全边界与凭据规则

返回入口：[[首页总控台]]

## 允许记录

- 主机地址、域名、系统类型、端口
- 账号名
- SSH 指纹
- 私钥或密码的存放位置说明
- 登录方式是否已变更
- 哪些高敏秘密已经停用、轮换或迁移

## 禁止记录

- 明文密码
- 私钥全文
- 恢复码全文
- Token、Cookie、会话串等可直接复用的高敏秘密

## 写法示例

- 可以写：`当前入口为 SSH，root 通道待下线`
- 可以写：`管理员私钥位于本地安全存储，指纹待补`
- 不要写：明文 root 密码
- 不要写：私钥全文

## 执行原则

- 工效与安全要平衡，但高敏秘密默认不落库。
- 若确有必要记录敏感信息，优先记录“在哪里、安全状态如何、谁负责”，而不是记录秘密本体。
'@
    '02-Agent规则\Codex.md' = @'
---
aliases:
  - Agent-Codex
类型: Agent
状态: 在岗
最后更新: 2026-03-30
tags:
  - Agent
  - Codex
---
# Codex

这是当前用于搭建和维护该运维仓库的主要 Agent 身份页。

## 当前职责

- 维护总网关仓库结构
- 补齐服务器主页、操作记录和规则页
- 在执行真实运维动作前后补全记录与验证

## 相关入口

- [[首页总控台]]
- [[Agent 工作准则]]
- [[记录规范]]
'@
    '04-模板\模板-服务器主页.md' = @'
---
类型: 模板
模板用途: 服务器主页
tags:
  - 模板
  - 服务器
---
# 模板-服务器主页

```yaml
---
aliases:
  - <IP或别名>
类型: 服务器
状态: 在役
环境: 生产
系统: <系统>
公网地址: <IP>
责任Agent:
  - "[[Codex]]"
关联索引:
  - "[[服务器索引]]"
最后更新: YYYY-MM-DD
tags:
  - server
  - <系统>
---
```

## 当前结论

- 

## 访问方式

- 

## 当前风险

- 

## 当前待办

- 

## 相关记录

- 

## 相关规则

- [[Agent 工作准则]]
- [[记录规范]]
- [[安全边界与凭据规则]]
'@
    '04-模板\模板-操作记录.md' = @'
---
类型: 模板
模板用途: 操作记录
tags:
  - 模板
  - 操作记录
---
# 模板-操作记录

```yaml
---
类型: 操作记录
日期: YYYY-MM-DD
目标服务器:
  - "[[服务器-示例-主机档案]]"
执行Agent:
  - "[[Codex]]"
操作类别:
  - 巡检
状态: 已完成
是否含敏感信息: false
---
```

## 背景

- 

## 操作目标

- 

## 执行动作

- 

## 验证结果

- 

## 影响范围

- 

## 风险与回滚点

- 

## 后续事项

- 

## 相关链接

- [[首页总控台]]
- [[服务器索引]]
'@
    '04-模板\模板-变更记录.md' = @'
---
类型: 模板
模板用途: 变更记录
tags:
  - 模板
  - 变更
---
# 模板-变更记录

## 变更背景

- 

## 变更内容

- 

## 验证方式

- 

## 风险与回滚

- 

## 相关链接

- [[首页总控台]]
'@
    '04-模板\模板-事件复盘.md' = @'
---
类型: 模板
模板用途: 事件复盘
tags:
  - 模板
  - 事件
  - 复盘
---
# 模板-事件复盘

## 事件摘要

- 

## 影响范围

- 

## 时间线

- 

## 根因

- 

## 处置动作

- 

## 后续改进

- 

## 相关链接

- [[首页总控台]]
'@
    '04-模板\模板-Agent交接.md' = @'
---
类型: 模板
模板用途: Agent 交接
tags:
  - 模板
  - Agent
  - 交接
---
# 模板-Agent交接

## 当前状态

- 

## 已完成

- 

## 未完成

- 

## 关键风险

- 

## 推荐下一步

- 

## 相关链接

- [[首页总控台]]
'@
    '01-服务器\在役\服务器-118.145.222.97-主机档案.md' = @'
---
aliases:
  - 118.145.222.97
类型: 服务器
状态: 待接管
环境: 生产
系统: Ubuntu
公网地址: 118.145.222.97
网段: 118.145.222.97/22
责任Agent:
  - "[[Codex]]"
关联索引:
  - "[[服务器索引]]"
最后更新: 2026-03-30
tags:
  - server
  - ubuntu
  - 在役
---
# 服务器-118.145.222.97-主机档案

返回入口：[[首页总控台]] ｜ [[服务器索引]]

## 当前结论

- 该服务器目前被描述为一台仅安装了 Ubuntu 基础系统的空机。
- 当前尚未开始正式巡检和基线建设。
- 已收到可用 root 登录通道，但明文密码不在本仓库落库。

## 访问方式

- 当前入口：SSH
- 当前已知账号：root
- 当前凭据状态：仅记录“已收到”，不记录明文
- 目标状态：建立非 root 管理员、SSH key 登录、关闭高风险入口

## 当前风险

- 初始 root + 密码远程登录风险较高
- 尚未确认防火墙、自动更新、日志和 fail2ban 状态
- 尚未完成最小可恢复运维基线

## 当前待办

- 完成首次巡检
- 完成最小安全基线建设
- 补齐首批系统验证和访问说明

## 相关记录

- [[记录-2026-03-30-118.145.222.97-接管准备]]

## 相关规则

- [[Agent 工作准则]]
- [[记录规范]]
- [[安全边界与凭据规则]]
'@
    '03-操作记录\2026\记录-2026-03-30-118.145.222.97-接管准备.md' = @'
---
类型: 操作记录
日期: 2026-03-30
目标服务器:
  - "[[服务器-118.145.222.97-主机档案]]"
执行Agent:
  - "[[Codex]]"
操作类别:
  - 仓库初始化
  - 接管准备
状态: 已完成
是否含敏感信息: false
tags:
  - 操作记录
  - 初始化
---
# 记录-2026-03-30-118.145.222.97-接管准备

返回入口：[[首页总控台]] ｜ [[最近操作总览]]

## 背景

- 需要先建立一个独立于 `claude` 的新 Obsidian vault，作为多服务器、多 Agent 的统一运维总网关。
- 该仓库的目标是让未来任何 Agent 都能快速看懂现状、接手维护，并在图谱中看到工作关系。

## 操作目标

- 创建新的独立 vault
- 建立中文目录与入口页
- 写入规则、模板、索引和首台服务器主页
- 让后续真实服务器操作可以直接接入本仓库

## 执行动作

- 创建独立 vault：`D:\obsidian_claude\服务器总网关`
- 注册为 Obsidian 已知 vault
- 写入总控台、索引页、规则页和模板页
- 创建首台服务器主页：[[服务器-118.145.222.97-主机档案]]
- 明确凭据策略：只记录低敏信息，不记录明文高敏秘密

## 验证结果

- 新 vault 已生成基础目录与核心笔记
- 服务器主页、总控台、索引页和模板页之间已建立 `[[内部链接]]`
- 该仓库可以作为后续服务器接管记录的落点

## 影响范围

- Obsidian 运维知识仓库
- 后续多 Agent 接手路径
- 118.145.222.97 的首轮运维记录入口

## 风险与回滚点

- 当前尚未对真实服务器做任何配置修改
- 如果结构需要收缩，可从 [[首页总控台]]、[[服务器索引]] 和模板页开始调整

## 后续事项

- 连接 [[服务器-118.145.222.97-主机档案]] 做首次巡检
- 补齐系统版本、磁盘、网络、端口、防火墙和时间配置
- 形成首轮安全基线记录

## 相关链接

- [[首页总控台]]
- [[服务器索引]]
- [[风险与待办索引]]
- [[服务器-118.145.222.97-主机档案]]
'@
}

foreach ($entry in $notes.GetEnumerator()) {
    $noteContent = $entry.Value.TrimStart("`r", "`n") + "`n"
    Write-Utf8NoBom -Path (Join-Path $vaultPath $entry.Key) -Content $noteContent
}

Write-Output $existingVaultId
Write-Output $vaultPath
POWERSHELL
)

encoded_command=$(printf '%s' "$ps_script" | iconv -f UTF-8 -t UTF-16LE | base64 -w0)
powershell.exe -NoProfile -ExecutionPolicy Bypass -EncodedCommand "$encoded_command"
