package com.erp.billingservice.interfaces.rest.advice;

import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.exception.InvoiceConflictException;
import com.erp.billingservice.domain.exception.InvoiceNotFoundException;
import com.erp.billingservice.domain.exception.TenantContextMissingException;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);
    private static final String BASE_TYPE = "https://erp-automotive.pe/errors/";

    private final MessageSource messageSource;

    public GlobalRestExceptionHandler() {
        this(null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GlobalRestExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String getMessage(String code, String defaultMessage, Object... args) {
        if (messageSource == null) {
            return defaultMessage;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse(getMessage("error.billing.validation-error.detail", "Request failed validation"));

        return problem("BILLING_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex, WebRequest request) {
        return problem("BILLING_DOMAIN_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({InvoiceNotFoundException.class, com.erp.billingservice.domain.exception.QuoteNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex, WebRequest request) {
        String detail = getMessage("error.billing.not-found.detail", ex.getMessage());
        return problem("BILLING_NOT_FOUND", HttpStatus.NOT_FOUND, detail, request);
    }

    @ExceptionHandler({InvoiceConflictException.class, com.erp.billingservice.domain.exception.QuoteConflictException.class})
    public ProblemDetail handleConflict(RuntimeException ex, WebRequest request) {
        String detail = getMessage("error.billing.conflict.detail", ex.getMessage());
        return problem("BILLING_CONFLICT", HttpStatus.CONFLICT, detail, request);
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ProblemDetail handleTenantMissing(TenantContextMissingException ex, WebRequest request) {
        return problem("BILLING_TENANT_MISSING", HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String detail = getMessage("error.billing.access-denied.detail", "Access is denied");
        return problem("BILLING_ACCESS_DENIED", HttpStatus.FORBIDDEN, detail, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
        String detail = getMessage("error.billing.unauthenticated.detail", "Full authentication is required");
        return problem("BILLING_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, detail, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception in billing service: {}", ex.getMessage(), ex);
        String detail = getMessage("error.billing.internal-error.detail", "An unexpected internal error occurred");
        return problem("BILLING_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, detail, request);
    }

    private ProblemDetail problem(String code, HttpStatus status, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(BASE_TYPE + code.toLowerCase()));
        problem.setTitle(code);
        problem.setProperty("error_code", code);

        UUID tenantId = TenantContextHolder.getTenantId();
        problem.setProperty("tenant_id", tenantId != null ? tenantId.toString() : "-");

        return problem;
    }
}
