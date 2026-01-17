package com.taskreminder.app.enums;

public enum TaskPriority {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
