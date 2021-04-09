package org.joshjoyce.lumivore;

import org.jetlang.fibers.Fiber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A container for components that all need to be started and/or stopped
 * in a predictable order.
 */
public class Module implements HasLifecycle {
    protected final List<HasLifecycle> components = new ArrayList<>();

    /**
     * Registers a component to be started and stopped with the
     * lifecycle of this module.
     *
     * @return the object that was passed in
     */
    public <T extends HasLifecycle> T register(T component) {
        components.add(component);
        return component;
    }

    /**
     * Registers a component to be started when this module
     * is started. Items registered are started in the order
     * that they were registered.
     *
     * @return the object that was passed in
     */
    public <T extends Startable> T register(T component) {
        components.add(new HasLifecycle() {
            @Override
            public void start() {
                component.start();
            }
        });
        return component;
    }

    /**
     * Registers a component to be stopped when this module
     * is stopped. Items registered are stopped in the reverse
     * order that they were registered.
     *
     * @return the object that was passed in
     */
    public <T extends Stoppable> T register(T component) {
        components.add(new HasLifecycle() {
            @Override
            public void stop() {
                component.stop();
            }
        });
        return component;
    }

    public Fiber register(Fiber fiber) {
        register(new FiberLifecycle(fiber));
        return fiber;
    }

    @Override
    public void start() {
        components.forEach(Startable::start);
    }

    @Override
    public void stop() {
        final var toStop = new ArrayList<>(components);
        Collections.reverse(toStop);
        toStop.forEach(Stoppable::stop);
    }
}
