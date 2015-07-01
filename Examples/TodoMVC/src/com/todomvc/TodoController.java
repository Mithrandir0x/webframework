package com.todomvc;

import edu.webframework.WebController;
import edu.webframework.annotations.*;

import java.util.List;

@UrlPathController(path = "/{filter}")
public class TodoController extends WebController {

    private static final String FILTER_ALL = "all";
    private static final String FILTER_ACTIVE = "active";
    private static final String FILTER_COMPLETED = "completed";

    @RequireService
    public TodoListService todoListService;

    @HttpMethod(type = HttpMethodType.GET)
    public void index(@HttpRequestParameter(name="filter", fromUrl = true) String filter) throws Exception {
        List<ThingTodo> thingsTodo;

        switch ( filter ) {
            case FILTER_ALL:
                thingsTodo = todoListService.getAllThings();
                break;
            case FILTER_ACTIVE:
                thingsTodo = todoListService.getActiveThingsTodo();
                break;
            case FILTER_COMPLETED:
                thingsTodo = todoListService.getCompletedThingsTodo();
                break;
            default:
                thingsTodo = todoListService.getAllThings();
                break;
        }

        filter = filter != null ? filter : FILTER_ALL;

        setModel("things", thingsTodo);
        setModel("filter", filter);

        view("index");
    }

    @HttpMethod(type = HttpMethodType.POST, action = "addThingTodo")
    public void addThingTodo(@HttpRequestParameter(name="thing") ThingTodo thing) throws Exception {
        todoListService.addThingTodo(thing);
        redirect(TodoController.class);
    }

    @HttpMethod(type = HttpMethodType.POST, action = "removeThingTodo")
    public void removeThingTodo(@HttpRequestParameter(name="thingId") Integer thingId) throws Exception {
        todoListService.removeThingTodo(thingId);
        redirect(TodoController.class);
    }



}