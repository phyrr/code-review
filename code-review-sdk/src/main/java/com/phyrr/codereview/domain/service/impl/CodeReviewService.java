package com.phyrr.codereview.domain.service.impl;

import com.phyrr.codereview.domain.service.AbstractCodeReviewService;
import com.phyrr.codereview.infrastructure.git.GitCommand;
import com.phyrr.codereview.infrastructure.openai.IOpenAI;
import com.phyrr.codereview.infrastructure.openai.LlmClientFactory;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.phyrr.codereview.infrastructure.feishu.FeishuBot;

import java.io.IOException;
import java.util.ArrayList;

public class CodeReviewService extends AbstractCodeReviewService {

    private static final String REVIEW_PROMPT =
            "你是一名负责 Pull Request 审查的 Tech Lead。请基于用户提供的 git diff，"
                    + "输出结构化、可执行的代码审查报告。\n"
                    + "\n"
                    + "审查优先级：\n"
                    + "1. 正确性与逻辑漏洞\n"
                    + "2. 安全与数据风险\n"
                    + "3. 性能与资源使用\n"
                    + "4. 可读性、命名与模块边界\n"
                    + "5. 测试覆盖与异常处理\n"
                    + "\n"
                    + "输出必须使用 Markdown，并严格遵循以下结构：\n"
                    + "\n"
                    + "# Code Review Report\n"
                    + "## 总览\n"
                    + "- 变更摘要：（一句话概括本次改动）\n"
                    + "- 综合评分：（0-100）\n"
                    + "- 合并建议：（Approve / Request Changes / Needs Discussion）\n"
                    + "\n"
                    + "## 变更理解\n"
                    + "（说明这次 diff 的业务意图、影响范围与潜在副作用）\n"
                    + "\n"
                    + "## 发现的问题\n"
                    + "| 级别 | 位置/模块 | 问题描述 | 影响 |\n"
                    + "| --- | --- | --- | --- |\n"
                    + "（级别仅可使用：Critical / Major / Minor / Suggestion）\n"
                    + "\n"
                    + "## 改进建议\n"
                    + "（针对 Major 及以上问题给出具体、可落地的修改建议）\n"
                    + "\n"
                    + "## 参考实现\n"
                    + "（仅针对最关键的问题给出修改示例，不要重写整个 diff）\n"
                    + "\n"
                    + "约束：\n"
                    + "- 只评审 diff 中出现的变更，不要臆测未提供的代码\n"
                    + "- 问题描述要具体，避免空泛评价\n"
                    + "- 不要输出模板说明、占位符或额外解释\n"
                    + "- 全文使用中文\n"
                    + "\n"
                    + "待审查 diff：";

    public CodeReviewService(GitCommand gitCommand, IOpenAI openAI, FeishuBot feishuBot) {
        super(gitCommand, openAI, feishuBot);
    }

    @Override
    protected String getDiffCode() throws IOException, InterruptedException {
        return gitCommand.diff();
    }

    @Override
    protected String codeReview(String diffCode) throws Exception {
        ChatCompletionRequestDTO request = new ChatCompletionRequestDTO();
        request.setModel(LlmClientFactory.resolveModel());
        request.setMessages(new ArrayList<ChatCompletionRequestDTO.Prompt>() {
            {
                add(new ChatCompletionRequestDTO.Prompt("user", REVIEW_PROMPT));
                add(new ChatCompletionRequestDTO.Prompt("user", diffCode));
            }
        });

        ChatCompletionSyncResponseDTO response = openAI.completions(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    @Override
    protected String recordCodeReview(String review) throws Exception {
        return gitCommand.commitAndPush(review);
    }

    @Override
    protected void pushMessage(String logUrl) throws Exception {
        if (feishuBot == null) {
            return;
        }

        feishuBot.sendReviewNotification(
                logUrl,
                gitCommand.getProject(),
                gitCommand.getBranch(),
                gitCommand.getAuthor(),
                gitCommand.getMessage()
        );
    }

}
