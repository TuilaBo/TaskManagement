package com.example.TaskManagement.service;

import com.example.TaskManagement.dto.TaskUpdateEmailContext;
import com.example.TaskManagement.model.Task;

public interface EmailService {

    void sendVerificationCodeEmail(String email, String code);

    void sendTaskAssignmentEmail(Task task);

    void sendTaskUpdatedEmail(Task task, TaskUpdateEmailContext context);

    void sendTaskReminderEmail(Task task);
}
