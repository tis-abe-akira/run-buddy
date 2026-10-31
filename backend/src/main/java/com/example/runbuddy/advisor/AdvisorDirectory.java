package com.example.runbuddy.advisor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** The advisors configured in this environment, and routing a consultation to the chosen one. */
@Service
public class AdvisorDirectory {

    private final Map<String, TrainingAdvisor> advisorsById = new LinkedHashMap<>();

    public AdvisorDirectory(List<TrainingAdvisor> advisors) {
        for (TrainingAdvisor advisor : advisors) {
            if (advisorsById.putIfAbsent(advisor.info().id(), advisor) != null) {
                throw new IllegalStateException("two advisors share the id " + advisor.info().id());
            }
        }
    }

    public List<AdvisorInfo> available() {
        return advisorsById.values().stream().map(TrainingAdvisor::info).toList();
    }

    /**
     * @throws IllegalArgumentException if the provider is not configured or does not offer the model
     */
    public Advice consult(String provider, String model, Consultation consultation) {
        TrainingAdvisor advisor = advisorsById.get(provider);
        if (advisor == null) {
            throw new IllegalArgumentException("unknown or unconfigured provider: " + provider);
        }
        if (!advisor.info().offers(model)) {
            throw new IllegalArgumentException(provider + " does not offer the model " + model);
        }
        return advisor.consult(consultation, model);
    }
}
