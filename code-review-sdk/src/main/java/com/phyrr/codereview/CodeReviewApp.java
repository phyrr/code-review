package com.phyrr.codereview;

import com.phyrr.codereview.domain.service.impl.CodeReviewService;
import com.phyrr.codereview.infrastructure.git.GitCommand;
import com.phyrr.codereview.infrastructure.openai.LlmClientFactory;
import com.phyrr.codereview.infrastructure.feishu.FeishuBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 代码评审入口：读取 git diff → 调用大模型评审 → 写入日志仓库 → 可选飞书通知。
 */
public class CodeReviewApp {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewApp.class);

    public static void main(String[] args) throws Exception {
        GitCommand gitCommand = new GitCommand(
                Env.getRequired("GITHUB_REVIEW_LOG_URI"),
                Env.getRequired("GITHUB_TOKEN"),
                Env.getRequired("COMMIT_PROJECT"),
                Env.getRequired("COMMIT_BRANCH"),
                Env.getRequired("COMMIT_AUTHOR"),
                Env.getRequired("COMMIT_MESSAGE")
        );

        FeishuBot feishuBot = FeishuBot.fromEnv();

        CodeReviewService service = new CodeReviewService(
                gitCommand,
                LlmClientFactory.create(),
                feishuBot
        );
        service.exec();

        logger.info("code-review done!");
    }

}
