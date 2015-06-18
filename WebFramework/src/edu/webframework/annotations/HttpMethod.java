package edu.webframework.annotations;

import edu.webframework.ServletDispatcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpMethod {

    String type();
    String action() default ServletDispatcher.DEFAULT;

}
