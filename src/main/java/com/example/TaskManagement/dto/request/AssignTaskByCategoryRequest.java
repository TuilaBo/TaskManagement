package com.example.TaskManagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssignTaskByCategoryRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Assigned staff ID is required")
    private Long assignedToId;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @FutureOrPresent(message = "Start date must not be in the past")
    private LocalDateTime startDate;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;
}
