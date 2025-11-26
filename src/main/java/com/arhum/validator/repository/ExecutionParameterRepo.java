package com.arhum.validator.repository;

import com.arhum.validator.entity.ExecutionParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionParameterRepo extends JpaRepository<ExecutionParameter, Long> {
}
