package hu.ogyh.voting.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * 2.4 Hibakezelés: minden hiba RFC 9457 szerinti {@code application/problem+json}, magyar címmel és a hiba
 * okával a {@code detail} mezőben. A keretrendszer kliens számára látható alapértelmezéseit is felülírja.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERRORS_PROPERTY = "hibak";

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Érvénytelen kérés", exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Nem található", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Belső hiba", "Váratlan hiba történt");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validációs hiba", "A kérés mezői hibásak");
        body.setProperty(ERRORS_PROPERTY, errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Olvashatatlan kérés", unreadableDetail(exception)));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String detail = "Hiányzik a(z) " + exception.getParameterName() + " paraméter";
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Hiányzó paraméter", detail));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail =
                "Érvénytelen érték a(z) " + exception.getPropertyName() + " paraméterben: " + exception.getValue();
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Érvénytelen paraméter", detail));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Nem létező útvonal: /" + exception.getResourcePath();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND, "Nem található", detail));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String detail = "A(z) " + exception.getMethod() + " metódus ezen az útvonalon nem támogatott";
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(problem(HttpStatus.METHOD_NOT_ALLOWED, "Nem támogatott metódus", detail));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String detail = "Nem támogatott tartalomtípus: " + exception.getContentType();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .headers(headers)
                .body(problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nem támogatott tartalomtípus", detail));
    }

    /** Ismeretlen kódnál és hibás értéknél megnevezi a mezőt és az értéket, egyébként általános üzenet. */
    private static String unreadableDetail(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof InvalidFormatException invalidFormat) {
            String field = jsonPath(invalidFormat.getPath());
            Object value = invalidFormat.getValue();
            return invalidFormat.getTargetType().isEnum()
                    ? "Ismeretlen kód a(z) " + field + " mezőben: " + value
                    : "Érvénytelen érték a(z) " + field + " mezőben: " + value;
        }
        return "A kérés törzse nem értelmezhető JSON";
    }

    /** A validációs hibákkal azonos formátum: {@code szavazatok[0].szavazat}. */
    private static String jsonPath(List<JacksonException.Reference> path) {
        StringBuilder result = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                if (!result.isEmpty()) {
                    result.append('.');
                }
                result.append(reference.getPropertyName());
            } else {
                result.append('[').append(reference.getIndex()).append(']');
            }
        }
        return result.toString();
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
