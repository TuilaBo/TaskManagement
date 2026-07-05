package com.example.TaskManagement.service;

import com.example.TaskManagement.dto.request.AssignTaskByCategoryRequest;
import com.example.TaskManagement.dto.request.TaskRequest;
import com.example.TaskManagement.dto.response.PageResponse;
import com.example.TaskManagement.dto.response.TaskResponse;
import com.example.TaskManagement.model.enums.TaskStatus;
import org.springframework.web.multipart.MultipartFile;

public interface TaskService {

    PageResponse<TaskResponse> getAllTasks(
            TaskStatus status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir);

    TaskResponse getTaskById(Long id);

    TaskResponse createTask(TaskRequest request);

    TaskResponse assignTaskByCategory(AssignTaskByCategoryRequest request);

    TaskResponse updateTask(Long id, TaskRequest request);

    void deleteTask(Long id);

    TaskResponse updateTaskStatus(Long id, TaskStatus status);

    TaskResponse completeTask(Long id, String note, MultipartFile image);

    void sendReminder(Long id);
}
