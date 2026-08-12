package com.phyrr.codereview.infrastructure.feishu;

import com.alibaba.fastjson2.JSON;
import com.phyrr.codereview.Env;
import com.phyrr.codereview.types.utils.FeishuSignUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeishuBot {

    private static final Logger logger = LoggerFactory.getLogger(FeishuBot.class);

    private final String webhookUrl;
    private final String secret;

    public FeishuBot(String webhookUrl, String secret) {
        this.webhookUrl = webhookUrl;
        this.secret = secret;
    }

    public static FeishuBot fromEnv() {
        String webhookUrl = Env.getOptional("FEISHU_WEBHOOK_URL");
        if (webhookUrl == null) {
            logger.info("feishu not configured, skip notification");
            return null;
        }
        return new FeishuBot(webhookUrl, Env.getOptional("FEISHU_SECRET"));
    }

    public void sendReviewNotification(String logUrl, String project, String branch,
                                       String author, String commitMessage) throws Exception {
        Map<String, Object> body = buildCardBody(logUrl, project, branch, author, commitMessage);
        if (secret != null && !secret.isEmpty()) {
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", FeishuSignUtils.sign(timestamp, secret));
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);

        byte[] payload = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            connection.disconnect();
        }

        if (status >= 400) {
            throw new RuntimeException("Feishu webhook error, status=" + status + ", body=" + response);
        }

        logger.info("feishu notification sent: {}", response);
    }

    private Map<String, Object> buildCardBody(String logUrl, String project, String branch,
                                              String author, String commitMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("msg_type", "interactive");
        body.put("card", buildCard(logUrl, project, branch, author, commitMessage));
        return body;
    }

    private Map<String, Object> buildCard(String logUrl, String project, String branch,
                                          String author, String commitMessage) {
        Map<String, Object> card = new HashMap<>();

        Map<String, Object> title = new HashMap<>();
        title.put("tag", "plain_text");
        title.put("content", "AI 代码评审完成");

        Map<String, Object> header = new HashMap<>();
        header.put("title", title);
        header.put("template", "blue");
        card.put("header", header);

        List<Map<String, Object>> elements = new ArrayList<>();

        Map<String, Object> contentText = new HashMap<>();
        contentText.put("tag", "lark_md");
        contentText.put("content", String.format(
                "**项目**：%s\n**分支**：%s\n**作者**：%s\n**提交说明**：%s",
                project, branch, author, commitMessage));

        Map<String, Object> contentDiv = new HashMap<>();
        contentDiv.put("tag", "div");
        contentDiv.put("text", contentText);
        elements.add(contentDiv);

        Map<String, Object> buttonText = new HashMap<>();
        buttonText.put("tag", "plain_text");
        buttonText.put("content", "查看评审报告");

        Map<String, Object> button = new HashMap<>();
        button.put("tag", "button");
        button.put("text", buttonText);
        button.put("url", logUrl);
        button.put("type", "primary");

        Map<String, Object> action = new HashMap<>();
        action.put("tag", "action");
        action.put("actions", new ArrayList<Map<String, Object>>() {{
            add(button);
        }});
        elements.add(action);

        card.put("elements", elements);
        return card;
    }
}
