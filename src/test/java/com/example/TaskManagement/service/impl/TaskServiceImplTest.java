package com.example.TaskManagement.service.impl;

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
import com.example.TaskManagement.model.enums.TaskStatus;
import com.example.TaskManagement.repository.TaskCategoryRepository;
import com.example.TaskManagement.repository.TaskRepository;
import com.example.TaskManagement.repository.UserRepository;
import com.example.TaskManagement.security.CustomUserDetails;
import com.example.TaskManagement.service.CloudinaryService;
import com.example.TaskManagement.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskCategoryRepository taskCategoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User manager;
    private User staff;
    private Task task;

    @BeforeEach
    void setUp() {
        manager = User.builder()
                .id(1L)
                .username("manager")
                .email("manager@test.com")
                .role(Role.MANAGER)
                .active(true)
                .build();

        staff = User.builder()
                .id(2L)
                .username("staff")
                .email("staff@test.com")
                .role(Role.STAFF)
                .active(true)
                .build();

        task = Task.builder()
                .id(10L)
                .title("Test task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .assignedTo(staff)
                .createdBy(manager)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusDays(3))
                .reminderSent(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllTasks_asManager_returnsPaginatedResults() {
        authenticate(manager);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<TaskResponse> result = taskService.getAllTasks(null, null, 0, 10, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test task");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
        verify(taskRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllTasks_asStaff_filtersByAssignedUser() {
        authenticate(staff);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByAssignedToId(eq(2L), any(Pageable.class))).thenReturn(page);

        PageResponse<TaskResponse> result = taskService.getAllTasks(null, null, 0, 10, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByAssignedToId(eq(2L), any(Pageable.class));
        verify(taskRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllTasks_withStatusFilter_callsCorrectRepository() {
        authenticate(manager);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByStatus(eq(TaskStatus.PENDING), any(Pageable.class))).thenReturn(page);

        taskService.getAllTasks(TaskStatus.PENDING, null, 0, 10, "createdAt", "desc");

        verify(taskRepository).findByStatus(eq(TaskStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getAllTasks_withKeyword_callsSearchRepository() {
        authenticate(staff);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.searchByKeywordAndAssignedTo(eq("report"), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        taskService.getAllTasks(null, "report", 0, 10, "createdAt", "desc");

        verify(taskRepository).searchByKeywordAndAssignedTo(eq("report"), eq(2L), any(Pageable.class));
    }

    @Test
    void getTaskById_whenFound_returnsTask() {
        authenticate(manager);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAssignedToUsername()).isEqualTo("staff");
    }

    @Test
    void getTaskById_whenNotFound_throwsException() {
        authenticate(manager);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void getTaskById_asStaff_forOtherUsersTask_throwsForbidden() {
        authenticate(staff);
        Task otherTask = Task.builder()
                .id(11L)
                .title("Other")
                .status(TaskStatus.PENDING)
                .assignedTo(manager)
                .createdBy(manager)
                .build();
        when(taskRepository.findById(11L)).thenReturn(Optional.of(otherTask));

        assertThatThrownBy(() -> taskService.getTaskById(11L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignTaskByCategory_asStaff_throwsForbidden() {
        authenticate(staff);
        AssignTaskByCategoryRequest request = new AssignTaskByCategoryRequest();
        request.setCategoryId(1L);
        request.setAssignedToId(2L);
        request.setDeadline(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> taskService.assignTaskByCategory(request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignTaskByCategory_asManager_createsTaskAndSendsEmail() {
        authenticate(manager);
        TaskCategory category = TaskCategory.builder()
                .id(1L)
                .name("Báo cáo")
                .description("Báo cáo định kỳ")
                .build();

        AssignTaskByCategoryRequest request = new AssignTaskByCategoryRequest();
        request.setCategoryId(1L);
        request.setAssignedToId(2L);
        request.setDeadline(LocalDateTime.now().plusDays(5));

        when(taskCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        TaskResponse response = taskService.assignTaskByCategory(request);

        assertThat(response.getTitle()).isEqualTo("Báo cáo");
        assertThat(response.getCategoryName()).isEqualTo("Báo cáo");
        verify(emailService).sendTaskAssignmentEmail(any(Task.class));
    }

    @Test
    void assignTaskByCategory_withPastDeadline_throwsBadRequest() {
        authenticate(manager);
        TaskCategory category = TaskCategory.builder().id(1L).name("Báo cáo").build();

        AssignTaskByCategoryRequest request = new AssignTaskByCategoryRequest();
        request.setCategoryId(1L);
        request.setAssignedToId(2L);
        request.setDeadline(LocalDateTime.now().minusDays(1));

        when(taskCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> taskService.assignTaskByCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");
    }

    @Test
    void completeTask_asAssignedStaff_marksCompleted() {
        authenticate(staff);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.completeTask(10L, "Done", null);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getCompletionNote()).isEqualTo("Done");
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    void completeTask_asOtherStaff_throwsForbidden() {
        User otherStaff = User.builder().id(3L).username("other").role(Role.STAFF).build();
        authenticate(otherStaff);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.completeTask(10L, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateTaskStatus_completedToPending_throwsBadRequest() {
        authenticate(staff);
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, TaskStatus.PENDING))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createTask_asManager_assigningToStaff_requiresDeadline() {
        authenticate(manager);
        TaskRequest request = new TaskRequest();
        request.setTitle("New task");
        request.setAssignedToId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Deadline is required");
    }

    private void authenticate(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
