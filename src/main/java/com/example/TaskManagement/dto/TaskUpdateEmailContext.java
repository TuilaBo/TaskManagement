package com.example.TaskManagement.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TaskUpdateEmailContext {

    private final String editorUsername;
    private final List<String> changes = new ArrayList<>();

    public TaskUpdateEmailContext(String editorUsername) {
        this.editorUsername = editorUsername;
    }

    public void addChange(String field, String oldValue, String newValue) {
        changes.add(field + ": " + oldValue + " → " + newValue);
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }
}
