package com.arhum.validator.entity;

import com.arhum.validator.entity.Base.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "execution_parameter_map")
public class ExecutionParameter extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parameter_value")
    private String parameterValue;

    // Many parameters can belong to one execution
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "execution_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_execution_parameter_execution")
    )
    private Execution execution;
}
