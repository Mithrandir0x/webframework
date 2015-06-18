package edu.webframework;

import edu.webframework.annotations.UrlPathController;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class WebController {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected ServletContext servletContext;

    public void setModel(String key, Object bean) {
        request.setAttribute(key, bean);
    }

    public Object getSessionAttribute(String key) {
        return request.getSession().getAttribute(key);
    }

    public void setSessionAttribute(String key, Object value) {
        request.getSession().setAttribute(key, value);
    }

    /* public Object getModel(String key) {
        return request.getAttribute(key);
    } */

    public void view(String view) throws ServletException, IOException {
        String jspView = getJspViewPath(view, false);
        log(String.format("jspView [%s] contextBaseUrl [%s]", jspView, request.getAttribute("contextBaseUrl")));
        RequestDispatcher dispatcher = request.getRequestDispatcher(jspView);
        dispatcher.forward(request, response);
    }

    public void view(String view, boolean publicJsp) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(getJspViewPath(view, publicJsp));
        dispatcher.forward(request, response);
    }

    public void redirect(Class controllerClass) throws Exception {
        if ( !controllerClass.isAnnotationPresent(UrlPathController.class) ) {
            throw new Exception("Class does not have UrlPathController annotation");
        }

        UrlPathController pathController = (UrlPathController) controllerClass.getAnnotation(UrlPathController.class);
        String path = getPath(pathController);
        log(String.format("Redirecting to [%s]", path));
        response.sendRedirect(path);
    }

    /* public void forward(Class controllerClass) throws Exception {
        if ( !controllerClass.isAnnotationPresent(UrlPathController.class) ) {
            throw new Exception("Class does not have UrlPathController annotation");
        }

        UrlPathController pathController = (UrlPathController) controllerClass.getAnnotation(UrlPathController.class);
        String path = getPath(pathController);
        log(String.format("Forwarding to [%s]", path));
        request.getRequestDispatcher(path).forward(request, response);
    } */

    protected String getPath(UrlPathController pathController) {
        return String.format("%s%s", request.getContextPath(), pathController.path());
    }

    protected String getJspViewPath(String viewPath, boolean publicJsp) {
        if ( publicJsp )
            return String.format("%s.jsp", viewPath);
        else
            return String.format("/WEB-INF/jsp/%s.jsp", viewPath);
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void log(String message) {
        servletContext.log(message);
    }

}
