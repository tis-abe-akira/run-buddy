package com.example.runbuddy.advisor.springai;

import com.example.runbuddy.advisor.AdvisorInfo;
import com.example.runbuddy.advisor.AdvisorTools;
import com.google.genai.Client;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini through the Gemini Developer API. Active only when an API key is configured
 * (GEMINI_API_KEY). Other providers get a configuration class like this one.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("!'${runbuddy.ai.gemini.api-key:}'.isBlank()")
public class GeminiAdvisorConfig {

    @Bean
    SpringAiTrainingAdvisor geminiAdvisor(
            @Value("${runbuddy.ai.gemini.api-key}") String apiKey,
            @Value("${runbuddy.ai.gemini.models}") String modelList,
            AdvisorTools tools,
            Clock clock) {
        List<String> models = Arrays.stream(modelList.split(",")).map(String::strip).filter(m -> !m.isEmpty()).toList();
        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(Client.builder().apiKey(apiKey).build())
                .options(GoogleGenAiChatOptions.builder().model(models.getFirst()).build())
                .build();
        return new SpringAiTrainingAdvisor(
                new AdvisorInfo("gemini", "Gemini", models),
                chatModel,
                model -> GoogleGenAiChatOptions.builder().model(model),
                tools,
                clock);
    }
}
