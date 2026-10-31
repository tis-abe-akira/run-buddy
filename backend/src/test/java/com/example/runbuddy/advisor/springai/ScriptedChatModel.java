package com.example.runbuddy.advisor.springai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * A ChatModel that answers with prepared texts, in order, and remembers every prompt it got.
 * It does not run tools: tool calling is the provider's job and is covered by the live test.
 */
class ScriptedChatModel implements ChatModel {

    private final Deque<String> answers;
    final List<Prompt> prompts = new ArrayList<>();

    ScriptedChatModel(String... answers) {
        this.answers = new ArrayDeque<>(List.of(answers));
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        if (answers.isEmpty()) {
            throw new IllegalStateException("the script ran out of answers");
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(answers.removeFirst()))));
    }
}
