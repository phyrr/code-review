# AI Code Review

基于 GitHub Actions 的自动代码评审工具：在 push / PR 时提取 `git diff`，调用 DeepSeek 生成评审报告，写入独立日志仓库，并可选发送飞书通知。

## 架构

```
GitHub Push/PR
    → GitHub Actions 触发
    → 提取最近一次 commit diff
    → 调用 DeepSeek 评审
    → Markdown 写入日志仓库
    → (可选) 飞书机器人通知
```

## DeepSeek 配置

默认使用 **DeepSeek API**（OpenAI 兼容协议）。

### GitHub Secrets（必填）

在业务仓库 `Settings → Secrets and variables → Actions` 中添加：

| Secret | 说明 |
|--------|------|
| `CODE_REVIEW_LOG_URI` | 日志仓库 URL，如 `https://github.com/you/code-review-log` |
| `CODE_TOKEN` | 有日志仓库写权限的 GitHub Token |
| `DEEPSEEK_API_KEY` | DeepSeek API Key，在 [platform.deepseek.com](https://platform.deepseek.com) 获取 |

### GitHub Secrets（可选）

| Secret | 说明 | 默认值 |
|--------|------|--------|
| `OPENAI_API_HOST` | API 地址 | `https://api.deepseek.com/chat/completions` |
| `LLM_MODEL` | 模型名 | `deepseek-chat` |
| `FEISHU_WEBHOOK_URL` | 飞书 Webhook | 不配则不发通知 |
| `FEISHU_SECRET` | 飞书签名校验密钥 | — |

### 本地运行

```powershell
$env:GITHUB_REVIEW_LOG_URI = "https://github.com/you/code-review-log"
$env:GITHUB_TOKEN = "ghp_xxxx"
$env:COMMIT_PROJECT = "my-app"
$env:COMMIT_BRANCH = "main"
$env:COMMIT_AUTHOR = "you <you@email.com>"
$env:COMMIT_MESSAGE = "test commit"
$env:DEEPSEEK_API_KEY = "sk-xxxx"

java -jar code-review-sdk/target/code-review-sdk-1.0.0.jar
```

### 可用模型

| 模型 | 说明 |
|------|------|
| `deepseek-chat` | 默认，通用对话（DeepSeek-V3） |
| `deepseek-reasoner` | 推理模型（DeepSeek-R1） |

切换模型：在 Secrets 中设置 `LLM_MODEL=deepseek-reasoner`。

## 本地构建

```bash
mvn clean package -DskipTests
```

产物：`code-review-sdk/target/code-review-sdk-1.0.0.jar`

## 飞书机器人配置

1. 在飞书群聊中添加「自定义机器人」。
2. 复制 Webhook 地址，填入 `FEISHU_WEBHOOK_URL`。
3. 若机器人开启了「签名校验」，将密钥填入 `FEISHU_SECRET`。

## 其他大模型（可选）

如需切换 provider，设置环境变量 `LLM_PROVIDER=chatglm` 并配置 `CHATGLM_*`；或使用 `OPENAI_API_KEY` + `OPENAI_API_HOST` 对接 OpenAI 及其他兼容服务。

## 项目结构

```
code-review/
├── code-review-sdk/
│   └── src/main/java/com/phyrr/codereview/
│       ├── CodeReviewApp.java
│       ├── domain/
│       └── infrastructure/   # Git / DeepSeek / 飞书
├── .github/workflows/
│   └── code-review.yml
└── pom.xml
```
