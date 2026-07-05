package com.example.TaskManagement.service.impl;

import com.example.TaskManagement.dto.TaskUpdateEmailContext;
import com.example.TaskManagement.dto.request.AssignTaskByCategoryRequest;
import com.example.TaskManagement.dto.request.TaskRequest;
import com.example.TaskManagement.dto.response.PageResponse;
import com.example.TaskManagement.dto.response.TaskResponse;
import com.example.TaskManagement.exception.BadRequestException;
import com.example.TaskManagement.exception.ForbiddenException;
import com.example.TaskManagement.exception.ResourceNotFoundException;
import com.example.TaskManagement.model.Task;
import com.example.TaskManagement.model.TaskCategory;
import com.example.TaskManagement.model.User;
import com.example.TaskManagement.model.enums.Role;
import com.example.TaskManagement.model.enums.TaskSource;
import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.repository.TaskCategoryRepository;
import com.example.TaskManagement.repository.TaskRepository;
import com.example.TaskManagement.repository.UserRepository;
import com.example.TaskManagement.security.CustomUserDetails;
import com.example.TaskManagement.service.CloudinaryService;
import com.example.TaskManagement.service.EmailService;
import com.example.TaskManagement.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "deadline", "title", "startDate"
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getAllTasks(
            TaskStatus status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        User currentUser = getCurrentUser();
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<Task> taskPage = findTasks(currentUser, status, keyword, pageable);
        Page<TaskResponse> responsePage = taskPage.map(this::mapToResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = findTaskById(id);
        validateTaskAccess(task);
        return mapToResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();
        User assignee = resolveAssignee(request.getAssignedToId(), currentUser);
        validateSchedule(request, currentUser, assignee);

        TaskCategory category = resolveCategory(request.getCategoryId());

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.PENDING)
                .assignedTo(assignee)
                .createdBy(currentUser)
                .category(category)
                .startDate(request.getStartDate())
                .deadline(request.getDeadline())
                .source(TaskSource.MANUAL)
                .reminderSent(false)
                .build();

        Task saved = taskRepository.save(task);

        if (shouldNotifyAssignee(currentUser, assignee)) {
            emailService.sendTaskAssignmentEmail(saved);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse assignTaskByCategory(AssignTaskByCategoryRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.MANAGER) {
            throw new ForbiddenException("Only managers can assign tasks by category");
        }

        TaskCategory category = resolveCategory(request.getCategoryId());
        User assignee = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getAssignedToId()));

        if (assignee.getRole() != Role.STAFF) {
            throw new BadRequestException("Tasks can only be assigned to staff members");
        }

        if (assignee.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Cannot assign category-based task to yourself");
        }

        validateCategorySchedule(request.getStartDate(), request.getDeadline());

        Task task = Task.builder()
                .title(category.getName())
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : category.getDescription())
                .status(TaskStatus.PENDING)
                .assignedTo(assignee)
                .createdBy(currentUser)
                .category(category)
                .startDate(request.getStartDate())
                .deadline(request.getDeadline())
                .source(TaskSource.CATEGORY)
                .reminderSent(false)
                .build();

        Task saved = taskRepository.save(task);
        emailService.sendTaskAssignmentEmail(saved);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTaskById(id);
        validateTaskAccess(task);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a completed task");
        }

        User currentUser = getCurrentUser();
        User assignee = task.getAssignedTo();
        TaskCategory category = task.getCategory();

        TaskUpdateEmailContext emailContext = new TaskUpdateEmailContext(currentUser.getUsername());
        captureChange(emailContext, "Tiêu đề", task.getTitle(), request.getTitle());
        captureChange(emailContext, "Mô tả",
                nullToDefault(task.getDescription()), nullToDefault(request.getDescription()));

        if (request.getAssignedToId() != null
                && !request.getAssignedToId().equals(assignee.getId())) {
            User newAssignee = resolveAssignee(request.getAssignedToId(), currentUser);
            captureChange(emailContext, "Người nhận", assignee.getUsername(), newAssignee.getUsername());
            assignee = newAssignee;
            task.setAssignedTo(assignee);
        }

        if (request.getCategoryId() != null) {
            TaskCategory newCategory = resolveCategory(request.getCategoryId());
            String oldCategory = category != null ? category.getName() : "Chưa có";
            captureChange(emailContext, "Danh mục", oldCategory, newCategory.getName());
            category = newCategory;
            task.setCategory(category);
        }

        validateSchedule(request, currentUser, assignee);
        captureChange(emailContext, "Ngày bắt đầu",
                formatDate(task.getStartDate()), formatDate(request.getStartDate()));
        captureChange(emailContext, "Hạn hoàn thành",
                formatDate(task.getDeadline()), formatDate(request.getDeadline()));

        boolean deadlineChanged = !Objects.equals(task.getDeadline(), request.getDeadline());

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate());
        task.setDeadline(request.getDeadline());

        if (deadlineChanged) {
            task.setReminderSent(false);
        }

        Task saved = taskRepository.save(task);

        if (shouldNotifyAssignee(currentUser, assignee) && emailContext.hasChanges()) {
            emailService.sendTaskUpdatedEmail(saved, emailContext);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        validateTaskAccess(task);
        taskRepository.delete(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatus status) {
        Task task = findTaskById(id);
        validateTaskAccess(task);

        if (task.getStatus() == TaskStatus.COMPLETED && status == TaskStatus.PENDING) {
            throw new BadRequestException("Completed tasks cannot be changed back to pending status");
        }

        task.setStatus(status);
        return mapToResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse completeTask(Long id, String note, MultipartFile image) {
        Task task = findTaskById(id);
        User currentUser = getCurrentUser();

        if (!task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the assigned staff can complete this task");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BadRequestException("Task is already completed");
        }

        if (note != null && note.length() > 2000) {
            throw new BadRequestException("Completion note must not exceed 2000 characters");
        }

        String proofUrl = null;
        if (image != null && !image.isEmpty()) {
            proofUrl = cloudinaryService.uploadImage(image);
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletionNote(note);
        task.setProofImageUrl(proofUrl);
        task.setCompletedAt(LocalDateTime.now());

        return mapToResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void sendReminder(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.MANAGER) {
            throw new ForbiddenException("Only managers can send task reminders");
        }

        Task task = findTaskById(id);
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BadRequestException("Cannot send reminder for a completed task");
        }

        emailService.sendTaskReminderEmail(task);
        task.setReminderSent(true);
        taskRepository.save(task);
    }

    private Page<Task> findTasks(User currentUser, TaskStatus status, String keyword, Pageable pageable) {
        boolean isManager = currentUser.getRole() == Role.MANAGER;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null;

        if (isManager) {
            if (hasKeyword && hasStatus) {
                return taskRepository.searchByKeywordAndStatus(keyword.trim(), status, pageable);
            }
            if (hasKeyword) {
                return taskRepository.searchByKeyword(keyword.trim(), pageable);
            }
            if (hasStatus) {
                return taskRepository.findByStatus(status, pageable);
            }
            return taskRepository.findAll(pageable);
        }

        if (hasKeyword && hasStatus) {
            return taskRepository.searchByKeywordStatusAndAssignedTo(
                    keyword.trim(), status, currentUser.getId(), pageable);
        }
        if (hasKeyword) {
            return taskRepository.searchByKeywordAndAssignedTo(keyword.trim(), currentUser.getId(), pageable);
        }
        if (hasStatus) {
            return taskRepository.findByAssignedToIdAndStatus(currentUser.getId(), status, pageable);
        }
        return taskRepository.findByAssignedToId(currentUser.getId(), pageable);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private void validateTaskAccess(Task task) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.STAFF
                && !task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this task");
        }
    }

    private void validateSchedule(TaskRequest request, User currentUser, User assignee) {
        boolean managerAssigningToStaff = currentUser.getRole() == Role.MANAGER
                && !assignee.getId().equals(currentUser.getId());

        if (managerAssigningToStaff && request.getDeadline() == null) {
            throw new BadRequestException("Deadline is required when assigning task to staff");
        }

        validateCategorySchedule(request.getStartDate(), request.getDeadline());
    }

    private void validateCategorySchedule(LocalDateTime startDate, LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && startDate.isBefore(now)) {
            throw new BadRequestException("Start date cannot be in the past");
        }

        if (deadline != null && deadline.isBefore(now)) {
            throw new BadRequestException("Deadline cannot be in the past");
        }

        if (startDate != null && deadline != null && !startDate.isBefore(deadline)) {
            throw new BadRequestException("Start date must be before the deadline");
        }
    }

    private TaskCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return taskCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Task category not found with id: " + categoryId));
    }

    private boolean shouldNotifyAssignee(User currentUser, User assignee) {
        return currentUser.getRole() == Role.MANAGER
                && !assignee.getId().equals(currentUser.getId());
    }

    private void captureChange(TaskUpdateEmailContext context, String field, String oldVal, String newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            context.addChange(field, oldVal, newVal);
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : "Chưa có";
    }

    private String nullToDefault(String value) {
        return value != null ? value : "Không có";
    }

    private User resolveAssignee(Long assignedToId, User currentUser) {
        if (assignedToId == null) {
            return currentUser;
        }

        if (currentUser.getRole() == Role.STAFF && !assignedToId.equals(currentUser.getId())) {
            throw new ForbiddenException("Staff can only assign tasks to themselves");
        }

        return userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assignedToId));
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private TaskResponse mapToResponse(Task task) {
        TaskCategory category = task.getCategory();
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedToId(task.getAssignedTo().getId())
                .assignedToUsername(task.getAssignedTo().getUsername())
                .createdById(task.getCreatedBy().getId())
                .createdByUsername(task.getCreatedBy().getUsername())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .startDate(task.getStartDate())
                .deadline(task.getDeadline())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .source(task.getSource())
                .sourceLabel(task.getSource() != null ? task.getSource().getLabel() : null)
                .completionNote(task.getCompletionNote())
                .proofImageUrl(task.getProofImageUrl())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
