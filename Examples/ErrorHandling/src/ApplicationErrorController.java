import edu.webframework.*;
import edu.webframework.annotations.*;

import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;

@UrlPathController(path = "/error")
public class ApplicationErrorController extends WebController {

    @HttpMethod(type = HttpMethodType.GET)
    public void handleException() throws Exception {
        HttpSession session = request.getSession();

        Object o = session.getAttribute(ServletDispatcher.WebControllerExceptionAttr);
        ServletDispatcher.WebControllerError error = (ServletDispatcher.WebControllerError) session.getAttribute(ServletDispatcher.WebControllerErrorTypeAttr);

        Exception ex = o == null ? null : (Exception) o;
        String message = "Null";
        String stackTrace = "";
        if ( ex != null ) {
            message = ex.getMessage() == null ? message : ex.getMessage();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            stackTrace = sw.toString();
        }

        setModel("httpStatusCode", error.getStatusCode());
        setModel("message", message);
        setModel("stackTrace", stackTrace);

        view("error");
    }

}