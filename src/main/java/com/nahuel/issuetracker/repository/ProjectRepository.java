package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByActiveTrueOrderByNameAsc();

    List<Project> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
