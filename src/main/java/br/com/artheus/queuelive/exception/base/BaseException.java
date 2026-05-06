package br.com.artheus.queuelive.exception.base;

import br.com.artheus.queuelive.exception.ApiError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ApiError apiError;
    private final HttpStatus status;

    protected BaseException(ApiError apiError, HttpStatus status) {
        super(apiError.getMessageKey());
        this.apiError = apiError;
        this.status = status;
    }
}