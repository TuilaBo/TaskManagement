package com.example.TaskManagement.scheduler;

import com.example.TaskManagement.model.Task;
import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.repository.TaskRepository;
import com.example.TaskManagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskReminderScheduler {

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    @Value("${task.reminder.hours-before-deadline}")
    private int hoursBeforeDeadline;

    @Scheduled(cron = "${task.reminder.cron}")
    @Transactional
    public void sendDeadlineReminders() {
        LocalDateTime threshold = LocalDateTime.now().plusHours(hoursBeforeDeadline);
        List<Task> tasks = taskRepository.findTasksNeedingReminder(TaskStatus.PENDING, threshold);

        if (tasks.isEmpty()) {
            log.debug("No tasks need reminder at this time");
            return;
        }

        log.info("Sending reminders for {} task(s)", tasks.size());
        for (Task task : tasks) {
            try {
                emailService.sendTaskReminderEmail(task);
                task.setReminderSent(true);
                taskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to send reminder for task id={}: {}", task.getId(), e.getMessage());
            }
        }
    }
}
