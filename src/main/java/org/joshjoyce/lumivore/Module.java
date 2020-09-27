package org.joshjoyce.lumivore;

import org.jetlang.fibers.Fiber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module implements HasLifecycle {
    protected final List<HasLifecycle> components = new ArrayList<>();

    public <T extends HasLifecycle> T register(T component) {
        components.add(component);
        return component;
    }

    public Fiber register(Fiber fiber) {
        register(new HasLifecycle() {
            @Override
            public void start() {
                fiber.start();
            }

            @Override
            public void stop() {
                fiber.dispose();
            }
        });
        return fiber;
    }

    @Override
    public void start() {
        components.forEach(Startable::start);
    }

    @Override
    public void stop() {
        final var reversed = new ArrayList<>(components);
        Collections.reverse(reversed);
        reversed.forEach(Stoppable::stop);
    }
}
