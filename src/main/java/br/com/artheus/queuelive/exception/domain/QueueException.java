package br.com.artheus.queuelive.exception.domain;

import br.com.artheus.queuelive.exception.ApiError;
import br.com.artheus.queuelive.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class QueueException extends BaseException {

    public QueueException(ApiError apiError, HttpStatus status) {
        super(apiError, status);
    }

    public static QueueException notFound() {
        return new QueueException(ApiError.QUEUE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static QueueException alreadyClosed() {
        return new QueueException(ApiError.QUEUE_ALREADY_CLOSED, HttpStatus.CONFLICT);
    }

    public static QueueException isClosed() {
        return new QueueException(ApiError.QUEUE_IS_CLOSED, HttpStatus.CONFLICT);
    }

    public static QueueException noUsersWaiting() {
        return new QueueException(ApiError.QUEUE_NO_USERS_WAITING, HttpStatus.NOT_FOUND);
    }
}