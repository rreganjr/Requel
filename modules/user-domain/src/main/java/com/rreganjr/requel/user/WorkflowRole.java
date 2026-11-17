package com.rreganjr.requel.user;

/** A user's responsibility in the requirement workflow. */
public enum WorkflowRole {
    PRODUCT_MANAGER("Product Manager"),
    ENGINEER("Engineer"),
    TEST_ENGINEER("Test Engineer"),
    ANALYST("Analyst");

    private final String displayName;

    WorkflowRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
