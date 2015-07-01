package edu.webframework.exceptions;

public class RequiredHttpRequestParameterException extends Exception {

    public RequiredHttpRequestParameterException(String requiredParameter) {
        super(String.format("Required HTTP Request Parameter [%s]", requiredParameter));
    }

}
