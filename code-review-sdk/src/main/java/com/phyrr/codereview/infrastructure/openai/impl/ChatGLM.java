package com.phyrr.codereview.infrastructure.openai.impl;

import com.alibaba.fastjson2.JSON;
import com.phyrr.codereview.infrastructure.openai.IOpenAI;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.phyrr.codereview.types.utils.BearerTokenUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatGLM implements IOpenAI {

    private final String apiHost;
    private final String apiKeySecret;

    public ChatGLM(String apiHost, String apiKeySecret) {
        this.apiHost = apiHost;
        this.apiKeySecret = apiKeySecret;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        String token = BearerTokenUtils.getToken(apiKeySecret);

        HttpURLConnection connection = (HttpURLConnection) new URL(apiHost).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
            os.write(input);
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
            throw new RuntimeException("ChatGLM API error, status=" + status + ", body=" + content);
        }

        return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
    }
}
