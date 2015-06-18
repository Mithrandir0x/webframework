package edu.webframework;

import javax.servlet.ServletContext;

public abstract class ServiceController {

    ServletContext servletContext;

    public abstract void initialize();

    public abstract void shutdown();

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void log(String message) {
        servletContext.log(message);
    }

    public void log(String message, Throwable t) {
        servletContext.log(message, t);
    }

}
