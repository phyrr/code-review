package com.phyrr.codereview.infrastructure.openai.impl;

import com.alibaba.fastjson2.JSON;
import com.phyrr.codereview.infrastructure.openai.IOpenAI;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OpenAI 标准 Chat Completions 客户端。
 * 也适用于 DeepSeek、Azure OpenAI 等兼容该协议的 API。
 */
public class OpenAiClient implements IOpenAI {

    private final String apiHost;
    private final String apiKey;

    public OpenAiClient(String apiHost, String apiKey) {
        this.apiHost = apiHost;
        this.apiKey = apiKey;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiHost).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } finally {
            connection.disconnect();
        }

        if (status >= 400) {
            throw new RuntimeException("OpenAI API error, status=" + status + ", body=" + content);
        }

        return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
    }
}
