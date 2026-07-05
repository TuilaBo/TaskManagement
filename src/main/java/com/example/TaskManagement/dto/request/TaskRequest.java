package com.example.TaskManagement.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Long assignedToId;

    private Long categoryId;

    @FutureOrPresent(message = "Start date must not be in the past")
    private LocalDateTime startDate;

    @Future(message = "Deadline must be in the future")
    private LocalDateTime deadline;
}
