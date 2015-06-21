import edu.webframework.ServiceController;
import edu.webframework.annotations.*;

@Service
public class CounterService extends ServiceController {

    private int count;

    @Override
    public void initialize() {
        count = 0;
    }

    @Override
    public void shutdown() {
    }

    public int count() {
        int currentCount;

        synchronized (this) {
            currentCount = ++count;
        }

        return currentCount;
    }

}
