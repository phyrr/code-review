package com.phyrr.codereview.infrastructure.openai;

import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.phyrr.codereview.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

public interface IOpenAI {

    ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception;

}
