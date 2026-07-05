package com.example.TaskManagement.controller;

import com.example.TaskManagement.dto.request.AssignTaskByCategoryRequest;
import com.example.TaskManagement.dto.request.TaskRequest;
import com.example.TaskManagement.dto.response.ApiResponse;
import com.example.TaskManagement.dto.response.PageResponse;
import com.example.TaskManagement.dto.response.TaskResponse;
import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Quản lý công việc — cần JWT token")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Danh sách task (phân trang)",
            description = "Lọc theo status/keyword. Manager thấy tất cả, Staff chỉ thấy task của mình.")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAllTasks(
            @Parameter(description = "PENDING hoặc COMPLETED") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Tìm trong title và description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sắp xếp theo: createdAt, updatedAt, deadline, title, startDate")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc hoặc desc") @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<TaskResponse> tasks = taskService.getAllTasks(status, keyword, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết task")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable Long id) {
        TaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task));
    }

    @PostMapping
    @Operation(summary = "Tạo task thủ công", description = "Nhập tiêu đề/mô tả tự do. Manager giao staff: bắt buộc assignedToId + deadline.")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created successfully", 201, task));
    }

    @PostMapping("/assign-by-category")
    @Operation(summary = "Giao task theo danh mục có sẵn",
            description = "Chỉ MANAGER. Chọn categoryId + staff + deadline. Tiêu đề tự lấy từ tên danh mục.")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTaskByCategory(
            @Valid @RequestBody AssignTaskByCategoryRequest request) {
        TaskResponse task = taskService.assignTaskByCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task assigned by category successfully", 201, task));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa task")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái task", description = "PENDING = chưa hoàn thành, COMPLETED = đã hoàn thành")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        TaskResponse task = taskService.updateTaskStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", task));
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Staff hoàn thành task",
            description = "Chỉ staff được giao task. Ghi chú và hình ảnh bằng chứng (upload Cloudinary) là tùy chọn.")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) MultipartFile image) {
        TaskResponse task = taskService.completeTask(id, note, image);
        return ResponseEntity.ok(ApiResponse.success("Task completed successfully", task));
    }

    @PostMapping("/{id}/remind")
    @Operation(summary = "Gửi email nhắc nhở", description = "Chỉ MANAGER. Gửi email nhắc staff hoàn thành task.")
    public ResponseEntity<ApiResponse<Void>> sendReminder(@PathVariable Long id) {
        taskService.sendReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder email sent successfully", null));
    }
}
