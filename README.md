# Web Framework
---------------

This is an implementation of a web framework based on Spring MVC's web annotations
for educational purposes.

The aim of this library is to have a minimalistic approach to an implementation of
Spring's architecture, and provide the required tools to create a web application
where the user can decouple all the routing stuff from the business logic.

**WARNING! It's NOT INTENDED to be production-ready.**

---

## Getting started

#### Initializing the framework

You have a clean web application based on Java, ready to be messed up. After
including this library in the classpath of the application, the `web.xml`
has to be modified to include this servlet mapping:

```xml
<servlet-mapping>
    <servlet-name>ServletDispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
<servlet>
    <servlet-name>ServletDispatcher</servlet-name>
    <servlet-class>edu.webframework.ServletDispatcher</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
```

This allows the web application to route all URL paths to the servlet dispatcher.

#### Creating a controller

Next step will be creating a controller that will handle the root path of your
web application, a `HomeController`:

```java
import edu.webframework.WebController;
import edu.webframework.annotations.*;

@UrlPathController(path = "/")
public class HomeController extends WebController {

    @HttpMethod(type = HttpMethodType.GET)
    public void index() throws Exception {
        view("index");
    }

}
```

#### Creating the controller's view

As you can see, it relies on a view to show content. To create a view, just go
to the folder `WEB-INF` and create a new folder named `jsp`. Inside, create the
file `index.jsp`, and put the following code inside:

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Web Framework Infamous Hello World</title>
    <meta charset="utf-8">
  </head>
  <body>
    Yet another Hello World for a web framework
  </body>
</html>
```

#### Hinting the Servlet Dispatcher

After creating the controller, we have to put this controller in the list of
controllers to be handled by the servlet dispatcher. To do so, edit again the
`web.xml` and add the followings code:

```xml
<context-param>
    <param-name>webcontrollers</param-name>
    <param-value>
        HomeController
    </param-value>
</context-param>
```

And you're ready to fire up the web application and go to the root.


