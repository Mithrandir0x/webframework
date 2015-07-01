package com.todomvc;

import edu.webframework.ServiceController;
import edu.webframework.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TodoListService extends ServiceController {

    private Map<Integer, ThingTodo> todos;
    private int nextId;

    @Override
    public void initialize() {
        ThingTodo thing;

        nextId = 0;
        todos = new ConcurrentHashMap<>();

        thing = createThing("Taste WebFramework", true);
        todos.put(thing.getId(), thing);

        thing = createThing("Buy a unicorn", false);
        todos.put(thing.getId(), thing);
    }

    @Override
    public void shutdown() {
    }

    public List<ThingTodo> getAllThings() {
        List<ThingTodo> list = new ArrayList<>();

        for ( ThingTodo thing : todos.values() ) {
            list.add(thing);
        }

        return list;
    }

    public List<ThingTodo> getActiveThingsTodo() {
        List<ThingTodo> list = new ArrayList<>();

        for ( ThingTodo thing : todos.values() ) {
            if ( !thing.isCompleted() ) {
                list.add(thing);
            }
        }

        return list;
    }

    public List<ThingTodo> getCompletedThingsTodo() {
        List<ThingTodo> list = new ArrayList<>();

        for ( ThingTodo thing : todos.values() ) {
            if ( thing.isCompleted() ) {
                list.add(thing);
            }
        }

        return list;
    }

    public void addThingTodo(ThingTodo thing) {
        int newId;

        synchronized (this) {
            newId = ++nextId;
        }

        thing.setId(newId);
        todos.put(thing.getId(), thing);
    }

    public void removeThingTodo(Integer thingId) {
        if ( todos.containsKey(thingId) ) {
            todos.remove(thingId);
        }
    }

    private ThingTodo createThing(String description, boolean done) {
        ThingTodo thing = new ThingTodo();
        thing.setId(++nextId);
        thing.setDescription(description);
        thing.setCompleted(done);
        return thing;
    }

}
