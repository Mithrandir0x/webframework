package edu.webframework;

import edu.webframework.annotations.*;
import edu.webframework.exceptions.RequiredHttpRequestParameterException;
import edu.webframework.exceptions.WebControllerDefinitionException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
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
        Class[] exceptionHandlerClass = getControllerClasses("errorhandler");

        initializeServices(serviceClasses);
        initializeWebControllers(controllerClasses);
        initializeErrorHandler(exceptionHandlerClass);
    }

    protected void initializeServices(Class[] serviceClasses) {
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
    }

    protected void initializeWebControllers(Class[] controllerClasses) {
        // @TODO Handle controller register exceptions in a nicer way...
        for ( Class controllerClass : controllerClasses ) {
            try {
                registerClassWebController(controllerClass);
            } catch ( Exception ex ) {
                log(ex.getMessage(), ex);
            }
        }
    }

    protected void initializeErrorHandler(Class[] exceptionHandlerClass) {
        if ( exceptionHandlerClass.length >= 1 ) {
            registerErrorHandlerController(exceptionHandlerClass[0]);
        } else {
            log("No controller available for error handling");
        }
    }

    protected Class[] getControllerClasses(String initParameter) {
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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        processRequest(HttpMethodType.GET, request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        processRequest(HttpMethodType.POST, request, response);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        processRequest(HttpMethodType.PUT, request, response);
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        processRequest(HttpMethodType.DELETE, request, response);
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        processRequest(HttpMethodType.HEAD, request, response);
    }

    public static String getContextBaseUrl(HttpServletRequest request) {
        String serverName = request.getServerName();
        String port = "" + request.getServerPort();
        String contextPath = request.getContextPath();
        return String.format("//%s:%s%s/", serverName, port, contextPath);
    }

    // @TODO Find better exception handling
    protected void processRequest(String method, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            String contextPath = request.getContextPath();
            String requestUri = request.getRequestURI();
            String processedUri = requestUri.replace(contextPath, "");
            String requestUrl = request.getRequestURL().toString();
            String servletPath = request.getServletPath();

            String action = request.getParameter(ACTION);
            action = action == null ? DEFAULT : action;

            log(String.format("method [%s] action [%s] contextPath [%s] requestUri [%s] processedUri [%s] requestUrl [%s] servletPath [%s]",
                    method, action, contextPath, requestUri, processedUri, requestUrl, servletPath));

            setContextBaseUrl(request);

            // @TODO Find a better way to group regular expressions, without having to iterate through multiple patterns
            for (Pattern uriRegEx : servletWebControllerProviders.keySet()) {
                Matcher m = uriRegEx.matcher(processedUri);
                if (m.matches()) {
                    try {
                        ControllerDescriptor descriptor = servletWebControllerProviders.get(uriRegEx);
                        HttpMethodActionMap actionMap = descriptor.providers.get(method);
                        HttpRequestArgumentMethodDescriptor argumentMethodDescriptor = actionMap.get(action);
                        if (argumentMethodDescriptor != null) {
                            WebController controller = descriptor.instantiateWebController(getServletContext(), request, response);
                            UrlRequestParameterDescriptor urlRequestParameterDescriptor = new UrlRequestParameterDescriptor(m, descriptor.urlRequestParameterIndex);

                            argumentMethodDescriptor.method.invoke(controller, getMethodArgumentsArray(request, argumentMethodDescriptor, urlRequestParameterDescriptor));
                            return;
                        }
                    } catch ( RequiredHttpRequestParameterException ex ) {
                        handleException(request, response, ex, WebControllerError.BadRequest);
                        return;
                    } catch ( InvocationTargetException ex ) {
                        handleException(request, response, ex.getCause(), WebControllerError.InternalServerError);
                        return;
                    }

                    break;
                }
            }

            log(String.format("Unknown url [%s]", requestUrl));
            handleException(request, response, null, WebControllerError.NotFound);
        } catch ( Throwable ex ) {
            throw new ServletException(ex);
        }
    }

    protected void handleException(HttpServletRequest request, HttpServletResponse response, Throwable exception, WebControllerError error) throws Throwable {
        if ( errorHandlerProvider == null && exception != null ) {
            throw exception;
        }

        if ( errorHandlerProvider != null && !response.isCommitted() ) {
            request.getSession().setAttribute(WebControllerErrorTypeAttr, error);
            request.getSession().setAttribute(WebControllerExceptionAttr, exception);

            if ( errorHandlerProvider.providers.containsKey(HttpMethodType.GET) ) {
                UrlPathController pathController = (UrlPathController) errorHandlerProvider.classController.getAnnotation(UrlPathController.class);
                String path = String.format("%s%s", request.getContextPath(), pathController.path());
                response.sendRedirect(path);
            }
        }
    }

    protected Object[] getMethodArgumentsArray(HttpServletRequest request,
                                             HttpRequestArgumentMethodDescriptor argumentMethodDescriptor,
                                             UrlRequestParameterDescriptor urlRequestParameterDescriptor)
            throws RequiredHttpRequestParameterException, UnsupportedEncodingException {
        Object[] arguments = new Object[argumentMethodDescriptor.requestArguments.size()];
        int i = 0;
        for ( RequestArgument requestArgument : argumentMethodDescriptor.requestArguments ) {
            String argumentName = requestArgument.name;
            if ( requestArgument.fromUrl ) {
                if ( urlRequestParameterDescriptor != null ) {
                    arguments[i] = castPrimitive(requestArgument.type, URLDecoder.decode(urlRequestParameterDescriptor.getValue(argumentName), "UTF-8"));
                } else {
                    arguments[i] = null;
                }
            } else {
                arguments[i] = cast(request, requestArgument.type, requestArgument.genericTypes, argumentName);
            }

            if ( arguments[i] == null && requestArgument.required ) {
                throw new RequiredHttpRequestParameterException(argumentName);
            }

            // log(String.format("Attribute name [%s] value [%s]", requestArgument.name, arguments[i]));

            i++;
        }
        return arguments;
    }

    private void setContextBaseUrl(HttpServletRequest request) {
        String contextBaseUrl = getContextBaseUrl(request);
        request.setAttribute("contextBaseUrl", contextBaseUrl);
    }

    protected <T> T cast(HttpServletRequest request, Class<T> clazz, Class[] genericTypes, String argumentName) {
        if ( List.class.equals(clazz) ) {
            return castList(request.getParameterValues(argumentName), genericTypes[0]);
        } else if ( isClassPrimitive(clazz) ) {
            return castPrimitive(clazz, request.getParameter(argumentName));
        } else {
            return castObject(request, clazz, argumentName);
        }
    }

    protected <T> T castPrimitive(Class<T> clazz, String value) {
        if ( String.class.equals(clazz) ) {
            return (T) value;
        } else if ( Integer.class.equals(clazz) ) {
            return (T) Integer.valueOf(value);
        } else if ( Long.class.equals(clazz) ) {
            return (T) Long.valueOf(value);
        } else if ( Float.class.equals(clazz) ) {
            return (T) Float.valueOf(value);
        } else if ( Double.class.equals(clazz) ) {
            return (T) Double.valueOf(value);
        }

        // @TODO Maybe, it should throw exception here...
        return null;
    }

    private <T> T castList(String[] parameterValues, Class genericType) {
        List list = new ArrayList<>();

        if ( parameterValues != null ) {
            for ( String value : parameterValues ) {
                list.add(castPrimitive(genericType, value));
            }
        }

        return (T) list;
    }

    protected <T> T castObject(HttpServletRequest request, Class<T> clazz, String argumentName) {
        try {
            T instance = clazz.newInstance();
            Field[] fields = clazz.getDeclaredFields();

            for ( Field field : fields ) {
                String fieldName = field.getName();

                String requestFieldName = argumentName + "." + fieldName;

                Class<?> fieldClass = field.getType();
                Method method = clazz.getMethod(getSetterMethodName(fieldName), new Class[]{fieldClass});

                if ( List.class.equals(fieldClass) ) {
                    String[] values = request.getParameterValues(requestFieldName);
                    if ( values != null && values.length > 0 ) {
                        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                        method.invoke(instance, castList(request.getParameterValues(requestFieldName), (Class) paramType.getActualTypeArguments()[0]));
                    } else {
                        method.invoke(instance, new ArrayList<>());
                    }
                } else if ( isClassPrimitive(fieldClass) ) {
                    String value = request.getParameter(requestFieldName);
                    if ( value != null ) {
                        method.invoke(instance, castPrimitive(fieldClass, value));
                    }
                }
                // @TODO Look for a possible solution for circular class dependencies when looking up the fields.
                /* else {
                    method.invoke(instance, castObject(request, fieldClass, requestFieldName));
                } */
            }

            return instance;

        } catch ( Exception ex ) {
            // @TODO Maybe, it should throw exception here...
            log(ex.getMessage(), ex);
            return null;
        }
    }

    protected String getSetterMethodName(String fieldName) {
        return String.format("set%s%s",
                fieldName.substring(0, 1).toUpperCase(),
                fieldName.substring(1));
    }

    protected boolean isClassPrimitive(Class clazz) {
        return String.class.equals(clazz)
                || Integer.class.equals(clazz)
                || Float.class.equals(clazz)
                || Double.class.equals(clazz);
    }

    protected void registerErrorHandlerController(Class clazz) {
        try {
            log(String.format("Registering error handling controller [%s]", clazz.getCanonicalName()));
            errorHandlerProvider = registerClassWebController(clazz);
        } catch ( Exception ex ) {
            log(ex.getMessage(), ex);
        }
    }

    protected ControllerDescriptor registerClassWebController(Class clazz) throws Exception {
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

    protected boolean isClassWebController(Class clazz) {
        return clazz != null
                && clazz.isAnnotationPresent(UrlPathController.class)
                && WebController.class.isAssignableFrom(clazz);
    }

    protected ControllerDescriptor generateDescriptor(Class clazz) throws Exception {
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

                Class[] paramClasses = classMethod.getParameterTypes();
                Type[] genTypes = classMethod.getGenericParameterTypes();
                ParameterizedType[] genericTypes;

                boolean doesMethodArgumentContainGenerics = genTypes != null && genTypes.length > 0
                        && genTypes[0] instanceof ParameterizedType;

                if ( doesMethodArgumentContainGenerics ) {
                    genericTypes = Arrays.copyOf(genTypes, genTypes.length, ParameterizedType[].class);
                } else {
                    genericTypes = new ParameterizedType[0];
                }

                Annotation[][] parameterAnnotationsArray = classMethod.getParameterAnnotations();

                for ( int i = 0 ; i < parameterAnnotationsArray.length ; i++ ) {
                    Annotation[] parameterAnnotations = parameterAnnotationsArray[i];
                    Class<?> paramClass = paramClasses[i];

                    if ( paramClass.isPrimitive() ) {
                        throw new WebControllerDefinitionException(String.format("Method [%s] argument [%s] cannot be primitive", classMethod.getName(), i));
                    }

                    for ( Annotation parameterAnnotation : parameterAnnotations ) {
                        if ( parameterAnnotation != null && parameterAnnotation instanceof HttpRequestParameter ) {
                            HttpRequestParameter httpRequestParameter = (HttpRequestParameter) parameterAnnotation;
                            Class[] genericClasses = genericTypes.length > 0 ? castTypeArray(genericTypes[i].getActualTypeArguments()) : new Class[0];
                            argumentDescriptor.requestArguments.add(new RequestArgument(httpRequestParameter, paramClass, genericClasses));
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

    protected Class[] castTypeArray(Type[] types) {
        Class[] classes = new Class[types.length];

        if ( types.length > 0 ) {
            for ( int i = 0; i < types.length; i++ ) {
                classes[i] = (Class) types[i];
            }
        }

        return classes;
    }

    protected void registerClassService(Class clazz) throws Exception {
        log(String.format("Loading service [%s]...", clazz.getCanonicalName()));
        if ( isClassServiceController(clazz) ) {
            ServiceController serviceController = (ServiceController) clazz.newInstance();
            serviceController.servletContext = getServletContext();
            servletServiceProviders.put(clazz, serviceController);
        } else {
            log(String.format("   Class [%s] does not have Service annotation", clazz.getCanonicalName()));
        }
    }

    protected void connectServiceDependencies(Class clazz) throws Exception {
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

    protected boolean isClassServiceController(Class clazz) {
        return clazz != null
                && clazz.isAnnotationPresent(Service.class)
                && ServiceController.class.isAssignableFrom(clazz);
    }

    protected void initializeService(Class clazz) {
        ServiceController controller = servletServiceProviders.get(clazz);
        if ( controller != null ) {
            controller.initialize();
        } else {
            log(String.format("Service [%s] is not available", clazz.getCanonicalName()));
        }
    }

    protected Pattern getUriPattern(UrlPathController pathController, ControllerDescriptor descriptor) {
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

    class ControllerDescriptor {

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

    class HttpMethodActionMap extends ConcurrentHashMap<String, HttpRequestArgumentMethodDescriptor> {}

    class HttpRequestArgumentMethodDescriptor {

        Method method;
        String httpMethod;
        List<RequestArgument> requestArguments = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("[HttpRequestArgumentMethodDescriptor method [%s] arguments [%s]]", httpMethod, requestArguments.size());
        }

    }

    class RequestArgument {

        String name;
        boolean required;
        boolean fromUrl;
        Class<?> type;
        Class<?>[] genericTypes;

        public RequestArgument(HttpRequestParameter httpRequestParameter, Class clazz, Class[] genericClasses) {
            name = httpRequestParameter.name();
            required = httpRequestParameter.required();
            fromUrl = httpRequestParameter.fromUrl();
            type = clazz;
            genericTypes = genericClasses;
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
