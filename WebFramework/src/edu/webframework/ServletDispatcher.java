package edu.webframework;

import edu.webframework.annotations.*;
import edu.webframework.exceptions.RequiredHttpRequestParameter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServletDispatcher extends HttpServlet {

    public static final String WebControllerErrorTypeAttr = "_WEB_CONTROLLER_ERROR_TYPE_";
    public static final String WebControllerExceptionAttr = "_WEB_CONTROLLER_EXCEPTION_";

    public static final String DEFAULT = "_DEFAULT_";
    public static final String ACTION = "_action_";

    public enum WebControllerError {

        NotFound(404),
        InternalServerError(500),
        BadRequest(400);

        private int statusCode;

        WebControllerError(int httpStatusCode) {
            statusCode = httpStatusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    Map<Pattern, ControllerDescriptor> servletWebControllerProviders = new ConcurrentHashMap<>();
    Map<Class, ServiceController> servletServiceProviders = new ConcurrentHashMap<>();

    ControllerDescriptor errorHandlerProvider = null;

    @Override
    public void init() {
        Class[] controllerClasses = getControllerClasses("webcontrollers");
        Class[] serviceClasses = getControllerClasses("services");

        for ( Class serviceClass : serviceClasses ) {
            try {
                registerClassService(serviceClass);
            } catch ( Exception ex ) {
                log(ex.getMessage(), ex);
            }
        }

        for ( Class serviceClass : serviceClasses ) {
            try {
                connectServiceDependencies(serviceClass);
            } catch ( Exception ex ) {
                log(ex.getMessage(), ex);
            }
        }

        for ( Class serviceClass : serviceClasses ) {
            try {
                initializeService(serviceClass);
            } catch ( Exception ex ) {
                log(ex.getMessage(), ex);
            }
        }

        // @TODO Handle controller register exceptions in a nicer way...
        for ( Class controllerClass : controllerClasses ) {
            try {
                registerClassWebController(controllerClass);
            } catch ( Exception ex ) {
                log(ex.getMessage(), ex);
            }
        }

        registerErrorHandlerController();

        log("Loaded application controllers");
    }

    private Class[] getControllerClasses(String initParameter) {
        String initParam = getServletContext().getInitParameter(initParameter);
        if ( initParam != null ) {
            String[] controllerClassNames = initParam.trim().split(",");
            Class[] controllers = new Class[controllerClassNames.length];

            for ( int i = 0; i < controllerClassNames.length; i++ ) {
                String className = controllerClassNames[i].trim();
                try {
                    controllers[i] = Class.forName(className);
                } catch ( Exception ex ) {
                    log(String.format("Could not find class [%s]", className));
                    return new Class[0];
                }
            }

            return controllers;
        } else {
            return new Class[0];
        }
    }

    @Override
    public void destroy() {
        Collection<ServiceController> services = servletServiceProviders.values();
        for ( ServiceController service : services ) {
            service.shutdown();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(HttpMethodType.GET, request, response);
        } catch ( Exception ex ) {
            log("Error while processing GET", ex);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(HttpMethodType.POST, request, response);
        } catch ( Exception ex ) {
            log("Error while processing POST", ex);
        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(HttpMethodType.PUT, request, response);
        } catch ( Exception ex ) {
            log("Error while processing POST", ex);
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(HttpMethodType.DELETE, request, response);
        } catch ( Exception ex ) {
            log("Error while processing POST", ex);
        }
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(HttpMethodType.HEAD, request, response);
        } catch ( Exception ex ) {
            log("Error while processing POST", ex);
        }
    }

    public static String getContextBaseUrl(HttpServletRequest request) {
        String serverName = request.getServerName();
        String port = "" + request.getServerPort();
        String contextPath = request.getContextPath();
        return String.format("//%s:%s%s/", serverName, port, contextPath);
    }

    public static void setContextUrl(HttpServletRequest request) {
        String contextBaseUrl = getContextBaseUrl(request);
        request.setAttribute("contextBaseUrl", contextBaseUrl);
    }

    private void processRequest(String method, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String processedUri = requestUri.replace(contextPath, "");
        String requestUrl = request.getRequestURL().toString();
        String servletPath = request.getServletPath();

        String action = request.getParameter(ACTION);
        action = action == null ? DEFAULT : action;

        log(String.format("method [%s] action [%s] contextPath [%s] requestUri [%s] processedUri [%s] requestUrl [%s] servletPath [%s]",
                method, action, contextPath, requestUri, processedUri, requestUrl, servletPath));

        setContextUrl(request);

        // @TODO Find a better way to group regular expressions, without having to iterate through multiple patterns
        for ( Pattern uriRegEx : servletWebControllerProviders.keySet() ) {
            Matcher m = uriRegEx.matcher(processedUri);
            if ( m.matches() ) {
                try {
                    ControllerDescriptor descriptor = servletWebControllerProviders.get(uriRegEx);
                    HttpMethodActionMap actionMap = descriptor.providers.get(method);
                    HttpRequestArgumentMethodDescriptor argumentMethodDescriptor = actionMap.get(action);
                    if ( argumentMethodDescriptor != null ) {
                        WebController controller = descriptor.instantiateWebController(getServletContext(), request, response);
                        UrlRequestParameterDescriptor urlRequestParameterDescriptor = new UrlRequestParameterDescriptor(m, descriptor.urlRequestParameterIndex);

                        try {
                            argumentMethodDescriptor.method.invoke(controller, getMethodArgumentsArray(request, argumentMethodDescriptor, urlRequestParameterDescriptor));
                            return;
                        } catch ( RequiredHttpRequestParameter ex ) {
                            log(ex.getMessage(), ex);
                            handleException(request, response, ex, WebControllerError.BadRequest);
                            return;
                        }
                    }
                } catch ( Exception ex ) {
                    log(ex.getMessage(), ex);
                    handleException(request, response, ex, WebControllerError.InternalServerError);
                    return;
                }

                break;
            }
        }

        log(String.format("Unknown url [%s]", requestUrl));
        handleException(request, response, null, WebControllerError.NotFound);
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response, Exception exception, WebControllerError error) throws IOException, ServletException {
        if ( !response.isCommitted() ) {
            request.getSession().setAttribute(WebControllerErrorTypeAttr, error);
            request.getSession().setAttribute(WebControllerExceptionAttr, exception);

            if ( errorHandlerProvider != null && errorHandlerProvider.providers.containsKey(HttpMethodType.GET) ) {
                UrlPathController pathController = (UrlPathController) errorHandlerProvider.classController.getAnnotation(UrlPathController.class);
                String path = String.format("%s%s", request.getContextPath(), pathController.path());
                response.sendRedirect(path);
            }
        }
    }

    private Object[] getMethodArgumentsArray(HttpServletRequest request,
                                             HttpRequestArgumentMethodDescriptor argumentMethodDescriptor,
                                             UrlRequestParameterDescriptor urlRequestParameterDescriptor)
            throws RequiredHttpRequestParameter, UnsupportedEncodingException {
        Object[] arguments = new Object[argumentMethodDescriptor.requestArguments.size()];
        int i = 0;
        for ( RequestArgument requestArgument : argumentMethodDescriptor.requestArguments ) {
            String argumentName = requestArgument.name;
            if ( requestArgument.fromUrl ) {
                if ( urlRequestParameterDescriptor != null ) {
                    arguments[i] = URLDecoder.decode(urlRequestParameterDescriptor.getValue(argumentName), "UTF-8");
                } else {
                    arguments[i] = null;
                }
            } else {
                arguments[i] = request.getParameter(argumentName);
            }

            if ( arguments[i] == null && requestArgument.required ) {
                throw new RequiredHttpRequestParameter(argumentName);
            }

            // log(String.format("Attribute name [%s] value [%s]", requestArgument.name, arguments[i]));

            i++;
        }
        return arguments;
    }

    private void registerErrorHandlerController() {
        String errorHandlerControllerClassName = getServletContext().getInitParameter("errorhandler");

        try {
            if ( errorHandlerControllerClassName != null ) {
                Class clazz = Class.forName(errorHandlerControllerClassName);

                log(String.format("Registering error handling controller [%s]", clazz.getCanonicalName()));
                errorHandlerProvider = registerClassWebController(clazz);
            }
        } catch ( Exception ex ) {
            log(ex.getMessage(), ex);
        } finally {
            if ( errorHandlerProvider == null ) {
                log("No controller available for error handling");
            }
        }
    }

    private ControllerDescriptor registerClassWebController(Class clazz) throws Exception {
        log(String.format("Loading controller [%s]...", clazz.getCanonicalName()));
        ControllerDescriptor descriptor = null;
        if ( isClassWebController(clazz) ) {
            descriptor = generateDescriptor(clazz);
            UrlPathController pathController = (UrlPathController) clazz.getAnnotation(UrlPathController.class);
            Pattern pattern = getUriPattern(pathController, descriptor);
            servletWebControllerProviders.put(pattern, descriptor);
        } else {
            log(String.format("   Class [%s] does not have UrlPathController annotation or does not extend from WebController", clazz.getCanonicalName()));
        }
        return descriptor;
    }

    private boolean isClassWebController(Class clazz) {
        return clazz != null
                && clazz.isAnnotationPresent(UrlPathController.class)
                && WebController.class.isAssignableFrom(clazz);
    }

    private ControllerDescriptor generateDescriptor(Class clazz) throws Exception {
        ControllerDescriptor descriptor = new ControllerDescriptor();
        descriptor.classController = clazz;

        Method[] classMethods = clazz.getDeclaredMethods();

        descriptor.providers.put(HttpMethodType.GET, new HttpMethodActionMap());
        descriptor.providers.put(HttpMethodType.POST, new HttpMethodActionMap());

        for ( Method classMethod : classMethods ) {
            if ( classMethod.isAnnotationPresent(HttpMethod.class) ) {
                HttpMethod httpMethod = classMethod.getAnnotation(HttpMethod.class);

                HttpRequestArgumentMethodDescriptor argumentDescriptor = new HttpRequestArgumentMethodDescriptor();
                argumentDescriptor.httpMethod = httpMethod.type();
                argumentDescriptor.method = classMethod;

                Annotation[][] parameterAnnotationsArray = classMethod.getParameterAnnotations();
                for (Annotation[] parameterAnnotations : parameterAnnotationsArray) {
                    for (Annotation parameterAnnotation : parameterAnnotations) {
                        if ( parameterAnnotation != null ) {
                            if ( parameterAnnotation instanceof HttpRequestParameter ) {
                                HttpRequestParameter httpRequestParameter = (HttpRequestParameter) parameterAnnotation;
                                argumentDescriptor.requestArguments.add(new RequestArgument(httpRequestParameter));
                            }
                        }
                    }
                }

                log(String.format("   Method [%s] action [%s] will handle [%s]", classMethod.getName(), httpMethod.action(), argumentDescriptor));
                HttpMethodActionMap actionMap = descriptor.providers.get(argumentDescriptor.httpMethod);
                actionMap.put(httpMethod.action(), argumentDescriptor);
            }
        }

        return descriptor;
    }

    private void registerClassService(Class clazz) throws Exception {
        log(String.format("Loading service [%s]...", clazz.getCanonicalName()));
        if ( isClassServiceController(clazz) ) {
            ServiceController serviceController = (ServiceController) clazz.newInstance();
            serviceController.servletContext = getServletContext();
            servletServiceProviders.put(clazz, serviceController);
        } else {
            log(String.format("   Class [%s] does not have Service annotation", clazz.getCanonicalName()));
        }
    }

    private void connectServiceDependencies(Class clazz) throws Exception {
        log(String.format("Loading service dependencies [%s]...", clazz.getCanonicalName()));
        if ( isClassServiceController(clazz) ) {
            Field[] classFields = clazz.getDeclaredFields();
            ServiceController controller = servletServiceProviders.get(clazz);

            for ( Field field : classFields ) {
                if ( field.isAnnotationPresent(RequireService.class) ) {
                    Class fieldClass = field.getType();
                    if ( fieldClass.equals(clazz) ) {
                        log("   TRYING TO RUSTLE MY JIMMIES? Not a chance, bro...");
                        continue;
                    }

                    if ( servletServiceProviders.containsKey(fieldClass) ) {
                        log(String.format("   Setting service [%s] to service [%s]", fieldClass.getCanonicalName(), clazz.getCanonicalName()));
                        field.set(controller, servletServiceProviders.get(fieldClass));
                    } else {
                        log(String.format("   Field class service [%s] is not available", fieldClass.getCanonicalName()));
                    }
                }
            }
        } else {
            log(String.format("   Class [%s] does not have Service annotation", clazz.getCanonicalName()));
        }
    }

    private boolean isClassServiceController(Class clazz) {
        return clazz != null
                && clazz.isAnnotationPresent(Service.class)
                && ServiceController.class.isAssignableFrom(clazz);
    }

    private void initializeService(Class clazz) {
        ServiceController controller = servletServiceProviders.get(clazz);
        controller.initialize();
    }

    private Pattern getUriPattern(UrlPathController pathController, ControllerDescriptor descriptor) {
        String path = pathController.path();

        Pattern p = Pattern.compile("\\{[a-zA-Z0-9]*\\}");
        Matcher m = p.matcher(path);

        int i = 1;

        while ( m.find() ) {
            int start = m.start() + 1;
            int end = m.end() - 1;

            descriptor.urlRequestParameterIndex.put(path.substring(start, end), i);

            i++;

            log(String.format("   i [%s] URL Argument [%s]", i, path.substring(start, end)));
        }

        path = "^" + path.replaceAll("\\{[a-zA-Z0-9]*\\}", "([a-z-A-Z-0-9-_~\\.\\'\\;\\:\\@\\&\\=\\+\\%\\#\\$]*)") + "$";

        log(String.format("   path [%s]", path));

        return Pattern.compile(path);
    }

    private class ControllerDescriptor {

        Class classController;
        Map<String, HttpMethodActionMap> providers = new ConcurrentHashMap<>();
        Map<String, Integer> urlRequestParameterIndex = new ConcurrentHashMap<>();

        public WebController instantiateWebController(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws Exception {
            Object o = classController.newInstance();

            WebController controller = (WebController) o;
            controller.servletContext = servletContext;
            controller.request = request;
            controller.response = response;

            Field[] classFields = classController.getDeclaredFields();

            for ( Field field : classFields ) {
                if ( field.isAnnotationPresent(RequireService.class) ) {
                    Class fieldClass = field.getType();
                    if ( servletServiceProviders.containsKey(fieldClass) ) {
                        //log(String.format("   Setting service [%s] to controller [%s]", fieldClass.getCanonicalName(), classController.getCanonicalName()));
                        field.set(controller, servletServiceProviders.get(fieldClass));
                    } else {
                        log(String.format("   Field class service [%s] is not available", fieldClass.getCanonicalName()));
                    }
                }
            }

            return controller;
        }

    }

    private class HttpMethodActionMap extends ConcurrentHashMap<String, HttpRequestArgumentMethodDescriptor> {}

    private class HttpRequestArgumentMethodDescriptor {

        Method method;
        String httpMethod;
        List<RequestArgument> requestArguments = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("[HttpRequestArgumentMethodDescriptor method [%s] arguments [%s]]", httpMethod, requestArguments.size());
        }

    }

    private class RequestArgument {

        String name;
        boolean required;
        boolean fromUrl;

        public RequestArgument(HttpRequestParameter httpRequestParameter) {
            name = httpRequestParameter.name();
            required = httpRequestParameter.required();
            fromUrl = httpRequestParameter.fromUrl();
        }

    }

    private class UrlRequestParameterDescriptor {

        Matcher matcher;
        Map<String, Integer> urlRequestParameterIndex = new HashMap<>();

        public UrlRequestParameterDescriptor(Matcher matcher, Map<String, Integer> urlRequestParameterIndex) {
            this.matcher = matcher;
            this.urlRequestParameterIndex = urlRequestParameterIndex;
        }

        public String getValue(String urlRequestParameterName) {
            int group = urlRequestParameterIndex.get(urlRequestParameterName);
            return matcher.group(group);
        }

    }

}
