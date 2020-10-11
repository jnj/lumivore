package org.joshjoyce.lumivore;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ReverseIteratorTest {

    @Test
    public void none() {
        var iter = new ReverseIterator<Integer>(List.of());
        assertFalse(iter.hasNext());
    }

    @Test
    public void one() {
        var iter = new ReverseIterator<>(List.of(1));
        assertEquals(Integer.valueOf(1), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void some() {
        var iter = new ReverseIterator<>(List.of(1, 2, 3));
        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(1), iter.next());
        assertFalse(iter.hasNext());
    }
}