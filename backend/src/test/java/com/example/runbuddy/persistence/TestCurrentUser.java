package com.example.runbuddy.persistence;

import com.example.runbuddy.shared.CurrentUser;
import java.util.UUID;

/** Lets a test act as different users. */
public class TestCurrentUser implements CurrentUser {

    private volatile String id = "test";

    @Override
    public String id() {
        return id;
    }

    public void switchTo(String id) {
        this.id = id;
    }

    /** Acting as a brand-new user gives a test empty tables without deleting anything. */
    public String switchToNewUser() {
        switchTo("user-" + UUID.randomUUID());
        return id;
    }
}
