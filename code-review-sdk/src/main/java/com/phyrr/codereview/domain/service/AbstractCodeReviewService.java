package com.phyrr.codereview.domain.service;

import com.phyrr.codereview.infrastructure.git.GitCommand;
import com.phyrr.codereview.infrastructure.openai.IOpenAI;
import com.phyrr.codereview.infrastructure.feishu.FeishuBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractCodeReviewService implements ICodeReviewService {

    private final Logger logger = LoggerFactory.getLogger(AbstractCodeReviewService.class);

    protected final GitCommand gitCommand;
    protected final IOpenAI openAI;
    protected final FeishuBot feishuBot;

    protected AbstractCodeReviewService(GitCommand gitCommand, IOpenAI openAI, FeishuBot feishuBot) {
        this.gitCommand = gitCommand;
        this.openAI = openAI;
        this.feishuBot = feishuBot;
    }

    @Override
    public void exec() throws Exception {
        logger.info("code-review start, project={}, branch={}", gitCommand.getProject(), gitCommand.getBranch());

        String diffCode = getDiffCode();
        if (diffCode == null || diffCode.trim().isEmpty()) {
            logger.warn("no diff found, skip review");
            return;
        }

        String review = codeReview(diffCode);
        String logUrl = recordCodeReview(review);
        pushMessage(logUrl);

        logger.info("code-review finished, log={}", logUrl);
    }

    protected abstract String getDiffCode() throws IOException, InterruptedException;

    protected abstract String codeReview(String diffCode) throws Exception;

    protected abstract String recordCodeReview(String review) throws Exception;

    protected abstract void pushMessage(String logUrl) throws Exception;

}
