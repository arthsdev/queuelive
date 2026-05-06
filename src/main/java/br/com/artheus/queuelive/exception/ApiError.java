package br.com.artheus.queuelive.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiError {

    // Queue errors
    QUEUE_NOT_FOUND("error.queue.not_found"),
    QUEUE_ALREADY_CLOSED("error.queue.already_closed"),
    QUEUE_IS_CLOSED("error.queue.is_closed"),
    QUEUE_NO_USERS_WAITING("error.queue.no_users_waiting"),

    // Queue Entry errors
    USER_ALREADY_IN_QUEUE("error.queue_entry.user_already_in_queue"),
    STAFF_CANNOT_JOIN_QUEUE("error.queue_entry.staff_cannot_join"),

    // User errors
    USER_NOT_FOUND("error.user.not_found");

    private final String messageKey;
}