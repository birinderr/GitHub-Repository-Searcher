package com.assignment.githubsearcher.exception;

public class GitHubApiException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public GitHubApiException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
