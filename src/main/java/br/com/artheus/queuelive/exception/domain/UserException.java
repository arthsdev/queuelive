package br.com.artheus.queuelive.exception.domain;

import br.com.artheus.queuelive.exception.ApiError;
import br.com.artheus.queuelive.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class UserException extends BaseException {

    public UserException(ApiError apiError, HttpStatus status) {
        super(apiError, status);
    }

    public static UserException notFound() {
        return new UserException(ApiError.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}