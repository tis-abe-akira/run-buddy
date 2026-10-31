package com.example.runbuddy.advisor;

/**
 * Port to one LLM provider (Gemini, Claude, OpenAI, Bedrock...). See ADR-0008.
 *
 * <p>Implementations must not hold a database transaction while waiting for the model: data is
 * read through {@link AdvisorTools}, each call in its own short transaction.
 */
public interface TrainingAdvisor {

    AdvisorInfo info();

    /**
     * @throws AdvisorUnavailableException when the provider fails or refuses to answer
     */
    Advice consult(Consultation consultation, String model);
}
