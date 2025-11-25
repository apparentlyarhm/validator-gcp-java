package com.arhum.validator.entity;

import com.arhum.validator.entity.Base.Base;
import com.arhum.validator.model.enums.RconCommands;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "executions")
public class Execution extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "command", nullable = false)
    private RconCommands command;

    @Column(name = "parameters")
    private String parameters;

}
