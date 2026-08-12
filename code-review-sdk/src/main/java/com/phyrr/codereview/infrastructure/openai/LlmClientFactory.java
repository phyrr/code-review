package com.phyrr.codereview.infrastructure.openai;

import com.phyrr.codereview.Env;
import com.phyrr.codereview.infrastructure.openai.impl.ChatGLM;
import com.phyrr.codereview.infrastructure.openai.impl.OpenAiClient;

public final class LlmClientFactory {

    private static final String DEFAULT_DEEPSEEK_HOST = "https://api.deepseek.com/chat/completions";
    private static final String DEFAULT_DEEPSEEK_MODEL = "deepseek-chat";
    private static final String DEFAULT_CHATGLM_MODEL = "glm-4-flash";

    private LlmClientFactory() {
    }

    public static IOpenAI create() {
        if (isChatGlmProvider()) {
            return new ChatGLM(
                    Env.getRequired("CHATGLM_APIHOST"),
                    Env.getRequired("CHATGLM_APIKEYSECRET")
            );
        }

        String apiHost = Env.getOptional("OPENAI_API_HOST");
        if (apiHost == null) {
            apiHost = DEFAULT_DEEPSEEK_HOST;
        }

        String apiKey = Env.getOptional("DEEPSEEK_API_KEY");
        if (apiKey == null) {
            apiKey = Env.getRequired("OPENAI_API_KEY");
        }

        return new OpenAiClient(apiHost, apiKey);
    }

    public static String resolveModel() {
        String model = Env.getOptional("LLM_MODEL");
        if (model != null) {
            return model;
        }
        return isChatGlmProvider() ? DEFAULT_CHATGLM_MODEL : DEFAULT_DEEPSEEK_MODEL;
    }

    private static boolean isChatGlmProvider() {
        return "chatglm".equalsIgnoreCase(Env.getOptional("LLM_PROVIDER"));
    }
}
