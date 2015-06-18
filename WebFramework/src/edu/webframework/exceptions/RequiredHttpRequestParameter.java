package edu.webframework.exceptions;

public class RequiredHttpRequestParameter extends Exception {

    public RequiredHttpRequestParameter(String requiredParameter) {
        super(String.format("Required HTTP Request Parameter [%s]", requiredParameter));
    }

}
