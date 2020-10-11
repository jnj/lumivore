package org.joshjoyce.lumivore;

import org.jetlang.fibers.Fiber;

public class FiberLifecycle implements HasLifecycle {

    private final Fiber fiber;

    public FiberLifecycle(Fiber fiber) {
        this.fiber = fiber;
    }

    @Override
    public void start() {
        fiber.start();
    }

    @Override
    public void stop() {
        fiber.dispose();
    }
}
