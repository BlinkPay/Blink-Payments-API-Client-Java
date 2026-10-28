package nz.co.blink.debit.service;

import nz.co.blink.debit.exception.BlinkInvalidValueException;

public interface ValidationService {
    /**
     * Validates the constructed request before sending to the API to reduce calls.
     *
     * @param type    the request type
     * @param payload the request to be validated
     * @throws BlinkInvalidValueException thrown if the request is invalid
     */
    void validateRequest(String type, Object payload) throws BlinkInvalidValueException;
}
