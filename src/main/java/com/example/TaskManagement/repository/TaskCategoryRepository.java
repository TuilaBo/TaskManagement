package com.example.TaskManagement.repository;

import com.example.TaskManagement.model.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<TaskCategory> findByNameIgnoreCase(String name);
}
