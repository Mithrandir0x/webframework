package edu.webframework.exceptions;

public class WebControllerDefinitionException extends Exception {

    public WebControllerDefinitionException(String reason) {
        super(String.format("Invalid error on web controller's definition. Reason [%s]", reason));
    }

}
