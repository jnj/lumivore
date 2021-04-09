package org.joshjoyce.lumivore;

import org.jetlang.fibers.Fiber;

public record FiberLifecycle(Fiber fiber) implements HasLifecycle {

    @Override
    public void start() {
        fiber.start();
    }

    @Override
    public void stop() {
        fiber.dispose();
    }
}
