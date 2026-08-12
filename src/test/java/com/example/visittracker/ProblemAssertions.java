package com.example.visittracker;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Services signal failures with {@link ResponseStatusException} rather than custom exception types,
 * so tests assert on the status the caller will actually receive.
 */
public final class ProblemAssertions {

    private ProblemAssertions() {
    }

    public static Consumer<Throwable> statusIs(HttpStatus expected) {
        return thrown -> assertThat(thrown)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(expected));
    }
}
