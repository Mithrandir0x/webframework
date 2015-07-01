package edu.webframework;

import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContext;

// @TODO Fix logs so that they do not rely on System.out
public class MockServletDispatcher extends ServletDispatcher {

    @Override
    public void log(String str) {
        System.out.println(str);
    }

    @Override
    public void log(String str, Throwable t) {
        System.out.println(str);
        t.printStackTrace();
    }

    @Override
    public ServletContext getServletContext() {
        return new MockServletContext();
    }

}
