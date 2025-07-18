package com.arhum.validator.service.contract;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.LoginResponse;

public interface AuthService {

    CommonResponse getGitHubLoginUrl();

    LoginResponse issueJwtToken(String code) throws BaseException;
}
