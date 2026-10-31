package com.example.runbuddy.shared;

/** The user the current request acts for. Row level security scopes all data to this id. */
@FunctionalInterface
public interface CurrentUser {

    String id();
}
