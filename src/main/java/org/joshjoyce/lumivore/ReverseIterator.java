package org.joshjoyce.lumivore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ReverseIterator<T> implements Iterator<T> {

    private final List<T> list;
    private int index;

    public ReverseIterator(Collection<T> collection) {
        this.list = new ArrayList<>(collection);
        this.index = list.size() - 1;
    }

    @Override
    public boolean hasNext() {
        return index > -1;
    }

    @Override
    public T next() {
        return list.get(index--);
    }
}
