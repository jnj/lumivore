package org.joshjoyce.lumivore;

import org.jetlang.fibers.Fiber;

import java.util.ArrayList;
import java.util.List;

public class Module implements HasLifecycle {
    protected final List<HasLifecycle> components = new ArrayList<>();

    public <T extends HasLifecycle> T register(T component) {
        components.add(component);
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
        final var iter = new ReverseIterator<>(new ArrayList<>(components));

        while (iter.hasNext()) {
            final var next = iter.next();
            next.stop();
        }
    }
}
