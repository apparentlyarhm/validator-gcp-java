package com.arhum.validator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoggedInUser {

    private String username;
    private Boolean isAdmin;

}
