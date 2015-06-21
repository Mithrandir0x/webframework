import edu.webframework.WebController;
import edu.webframework.annotations.*;

@UrlPathController(path = "/count/")
public class CounterController extends WebController {

    @RequireService
    public CounterService counterService;

    @HttpMethod(type = HttpMethodType.GET)
    public void index() throws Exception {
        setModel("counter", counterService.count());
        view("index");
    }

}