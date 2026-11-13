package dev.local.ai.core.models;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.local.ai.core.connections.OpenAICompatibleConnection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingChatModelsProviderTest {

    @Test
    void should_create_openai_compatible_model_without_openai_only_options() {
        var connection = new OpenAICompatibleConnection(
            "id",
            "local",
            "LM Studio",
            "http://localhost:1234/v1/",
            "  "
        );
        var modelInfo = new ModelInfo(
            "qwen2.5",
            "Qwen 2.5",
            "local model",
            ModelInfo.NOT_SPECIFIED,
            ModelInfo.NOT_SPECIFIED
        );

        var model = new StreamingChatModelsProvider()
            .createStreamingChatModel(new LLMInfoAndConnection(modelInfo, connection));

        assertThat(model).isInstanceOf(OpenAiStreamingChatModel.class);
        var parameters = ((OpenAiStreamingChatModel) model).defaultRequestParameters();
        assertThat(parameters.modelName()).isEqualTo("qwen2.5");
        assertThat(parameters.reasoningEffort()).isNull();
        assertThat(parameters.promptCacheOptions()).isNull();
    }

    @Test
    void should_treat_blank_api_key_as_absent_and_keep_a_real_key() {
        var withoutKey = new OpenAICompatibleConnection("local", "LM Studio");
        var withKey = new OpenAICompatibleConnection(
            "id",
            "remote",
            "OpenRouter",
            "https://openrouter.ai/api/v1",
            "secret"
        );

        assertThat(withoutKey.baseUrl()).isEqualTo("http://localhost:1234/v1");
        assertThat(withoutKey.apiKeyOrNull()).isNull();
        assertThat(withKey.apiKeyOrNull()).isEqualTo("secret");
    }
}
