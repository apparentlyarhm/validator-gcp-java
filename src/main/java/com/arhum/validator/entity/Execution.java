package com.arhum.validator.entity;

import com.arhum.validator.entity.Base.Base;
import com.arhum.validator.model.enums.RconCommands;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "executions", uniqueConstraints = {
    @UniqueConstraint(columnNames = "executionId")
})
public class Execution extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, unique = true)
    private String executionId;

    @Column(name = "username", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "command", nullable = false)
    private RconCommands command;

    @Column(name = "parameter_count")
    private Integer parameterCount;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExecutionParameter> parameters = new ArrayList<>();

    @PrePersist
    private void onInsert() {
        this.executionId = UUID.randomUUID().toString();
    }

}
