package com.example.TaskManagement.model.enums;

public enum TaskSource {

    MANUAL("Tạo thủ công"),
    CATEGORY("Giao theo danh mục");

    private final String label;

    TaskSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
