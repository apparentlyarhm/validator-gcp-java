package com.arhum.validator.repository;

import com.arhum.validator.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRepo extends JpaRepository<Execution, Long> {

    // will add stuff as needed
}
