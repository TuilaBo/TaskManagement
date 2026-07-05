package com.example.TaskManagement.controller;

import com.example.TaskManagement.dto.request.TaskCategoryRequest;
import com.example.TaskManagement.dto.response.ApiResponse;
import com.example.TaskManagement.dto.response.TaskCategoryResponse;
import com.example.TaskManagement.service.TaskCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/task-categories")
@RequiredArgsConstructor
@Tag(name = "Task Categories", description = "Danh mục công việc — Manager quản lý, Staff xem để chọn khi giao task")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskCategoryController {

    private final TaskCategoryService taskCategoryService;

    @GetMapping
    @Operation(summary = "Danh sách danh mục task")
    public ResponseEntity<ApiResponse<List<TaskCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Task categories retrieved successfully",
                taskCategoryService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết danh mục task")
    public ResponseEntity<ApiResponse<TaskCategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Task category retrieved successfully",
                taskCategoryService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo danh mục task", description = "Chỉ MANAGER")
    public ResponseEntity<ApiResponse<TaskCategoryResponse>> create(
            @Valid @RequestBody TaskCategoryRequest request) {
        TaskCategoryResponse response = taskCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task category created successfully", 201, response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục task", description = "Chỉ MANAGER")
    public ResponseEntity<ApiResponse<TaskCategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task category updated successfully",
                taskCategoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục task", description = "Chỉ MANAGER. Không xóa được nếu đang có task sử dụng.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Task category deleted successfully", null));
    }
}
