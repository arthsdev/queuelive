package br.com.artheus.queuelive.exception.domain;

import br.com.artheus.queuelive.exception.ApiError;
import br.com.artheus.queuelive.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class QueueEntryException extends BaseException {

    public QueueEntryException(ApiError apiError, HttpStatus status) {
        super(apiError, status);
    }

    public static QueueEntryException userAlreadyInQueue() {
        return new QueueEntryException(ApiError.USER_ALREADY_IN_QUEUE, HttpStatus.CONFLICT);
    }

    public static QueueEntryException staffCannotJoin() {
        return new QueueEntryException(ApiError.STAFF_CANNOT_JOIN_QUEUE, HttpStatus.FORBIDDEN);
    }
}