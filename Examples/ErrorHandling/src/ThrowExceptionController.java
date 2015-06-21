import edu.webframework.WebController;
import edu.webframework.annotations.*;

@UrlPathController(path = "/throwException")
public class ThrowExceptionController extends WebController {

    @HttpMethod(type = HttpMethodType.GET)
    public void index() throws Exception {
        throw new Exception("I never asked for this");
    }

}