package com.example.TaskManagement.dto.response;

import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.model.enums.TaskSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assignedToId;
    private String assignedToUsername;
    private Long createdById;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    private Long categoryId;
    private String categoryName;
    private String completionNote;
    private String proofImageUrl;
    private LocalDateTime completedAt;
    private TaskSource source;
    private String sourceLabel;
}
