package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.dto.request.TaskCategoryRequest;
import com.example.TaskManagement.dto.response.TaskCategoryResponse;
import com.example.TaskManagement.exception.BadRequestException;
import com.example.TaskManagement.exception.ForbiddenException;
import com.example.TaskManagement.exception.ResourceNotFoundException;
import com.example.TaskManagement.model.TaskCategory;
import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import com.example.TaskManagement.repository.TaskCategoryRepository;
import com.example.TaskManagement.repository.TaskRepository;
import com.example.TaskManagement.security.CustomUserDetails;
import com.example.TaskManagement.service.TaskCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskCategoryServiceImpl implements TaskCategoryService {

    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskCategoryResponse> getAll() {
        return taskCategoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskCategoryResponse getById(Long id) {
        return mapToResponse(findCategoryById(id));
    }

    @Override
    @Transactional
    public TaskCategoryResponse create(TaskCategoryRequest request) {
        requireManager();
        validateUniqueName(request.getName(), null);

        TaskCategory category = TaskCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        return mapToResponse(taskCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public TaskCategoryResponse update(Long id, TaskCategoryRequest request) {
        requireManager();
        TaskCategory category = findCategoryById(id);
        validateUniqueName(request.getName(), id);

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        return mapToResponse(taskCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireManager();
        TaskCategory category = findCategoryById(id);

        if (taskRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category that is assigned to tasks");
        }

        taskCategoryRepository.delete(category);
    }

    private TaskCategory findCategoryById(Long id) {
        return taskCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task category not found with id: " + id));
    }

    private void validateUniqueName(String name, Long excludeId) {
        taskCategoryRepository.findByNameIgnoreCase(name.trim())
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Category name already exists");
                });
    }

    private void requireManager() {
        if (getCurrentUser().getRole() != Role.MANAGER) {
            throw new ForbiddenException("Only managers can manage task categories");
        }
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private TaskCategoryResponse mapToResponse(TaskCategory category) {
        return TaskCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
