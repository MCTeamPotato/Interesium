package com.teampotato.interesium.api.util;

import java.util.Iterator;

public class MergedIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator1;
    private final Iterator<T> iterator2;
    private boolean useIterator1;

    public MergedIterator(Iterator<T> iterator1, Iterator<T> iterator2) {
        this.iterator1 = iterator1;
        this.iterator2 = iterator2;
        this.useIterator1 = true;
    }

    @Override
    public boolean hasNext() {
        return (useIterator1 && iterator1.hasNext()) || iterator2.hasNext();
    }

    @Override
    public T next() {
        if (useIterator1) {
            if (iterator1.hasNext()) {
                return iterator1.next();
            } else {
                useIterator1 = false;
            }
        }
        return iterator2.next();
    }
}
