package com.example.TaskManagement.service;

import com.example.TaskManagement.dto.request.TaskCategoryRequest;
import com.example.TaskManagement.dto.response.TaskCategoryResponse;

import java.util.List;

public interface TaskCategoryService {

    List<TaskCategoryResponse> getAll();

    TaskCategoryResponse getById(Long id);

    TaskCategoryResponse create(TaskCategoryRequest request);

    TaskCategoryResponse update(Long id, TaskCategoryRequest request);

    void delete(Long id);
}
