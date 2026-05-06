package br.com.artheus.queuelive.enums;

public enum QueueStatus {
    OPEN,
    CLOSED;

    public boolean isClosed() {
        return this == CLOSED;
    }
}
