import edu.webframework.WebController;
import edu.webframework.annotations.*;

@UrlPathController(path = "/")
public class HomeController extends WebController {

    @HttpMethod(type = HttpMethodType.GET)
    public void index() throws Exception {
        view("index");
    }

}