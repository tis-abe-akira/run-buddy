package com.example.runbuddy.advisor;

import java.util.List;
import java.util.Objects;

/** What the user can pick: a provider and one of its models. The first model is the default. */
public record AdvisorInfo(String id, String displayName, List<String> models) {

    public AdvisorInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        models = List.copyOf(models);
        if (models.isEmpty()) {
            throw new IllegalArgumentException("advisor " + id + " offers no models");
        }
    }

    public boolean offers(String model) {
        return models.contains(model);
    }
}
