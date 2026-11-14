package com.assignment.githubsearcher.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.assignment.githubsearcher.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "searchRequest");
        bindingResult.addError(new FieldError("searchRequest", "query", "query is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details()).containsExactly("query: query is required");
    }

    @Test
    void handleConstraintViolation_returnsDetails() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("minStars");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be >= 0");

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getBody().details()).containsExactly("minStars: must be >= 0");
    }

    @Test
    void handleBadRequest_coversCommonCases() {
        assertThat(handler.handleBadRequest(new InvalidSortException("bad sort")).getBody().message())
                .isEqualTo("bad sort");
        assertThat(handler.handleBadRequest(new IllegalArgumentException("illegal")).getBody().message())
                .isEqualTo("illegal");
        assertThat(handler.handleBadRequest(
                new MethodArgumentTypeMismatchException("x", Integer.class, "minStars", null, null)
        ).getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(handler.handleBadRequest(
                new HttpMessageNotReadableException("bad json", mock(HttpInputMessage.class))
        ).getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleGitHubApi_usesResolvedStatusOrBadGateway() {
        ResponseEntity<ErrorResponse> known = handler.handleGitHubApi(
                new GitHubApiException("GITHUB_RATE_LIMIT", "limited", 403));
        assertThat(known.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ErrorResponse> unknown = handler.handleGitHubApi(
                new GitHubApiException("GITHUB_API_ERROR", "weird", 999));
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handleGeneric_returnsInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().details()).isEqualTo(List.of());
    }
}
