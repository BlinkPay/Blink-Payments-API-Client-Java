package nz.co.blink.debit.service.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import nz.co.blink.debit.exception.BlinkInvalidValueException;
import nz.co.blink.debit.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Jakarta Bean Validation 3.0 implementation of {@link ValidationService}.
 */
@Service
@Slf4j
public class JakartaValidationServiceImpl implements ValidationService {

    private final Validator validator;

    /**
     * Default constructor.
     *
     * @param validator the {@link Validator}
     */
    @Autowired
    public JakartaValidationServiceImpl(Validator validator) {
        this.validator = validator;
    }

    @Override
    public void validateRequest(final String type, Object payload) throws BlinkInvalidValueException {
        Set<ConstraintViolation<Object>> violations = new HashSet<>(validator.validate(payload));
        if (!violations.isEmpty()) {
            String constraintViolations = violations.stream()
                    .map(cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage())
                    .collect(Collectors.joining(", "));
            log.error("Validation failed for {} request: {}", type, constraintViolations);
            throw new BlinkInvalidValueException(String.format("Validation failed for %s request: %s", type,
                    violations));
        }
    }
}
