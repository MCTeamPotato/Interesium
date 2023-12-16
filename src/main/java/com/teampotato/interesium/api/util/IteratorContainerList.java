package com.teampotato.interesium.api.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class IteratorContainerList<U> implements List<U> {
    public @Nullable Iterator<U> iterator;
    public final ObjectArrayList<U> elements = new ObjectArrayList<>();

    public final ObjectArrayList<U> iteratorList = new ObjectArrayList<>();

    public final ObjectArrayList<U> concatedList = new ObjectArrayList<>();

    public IteratorContainerList(@NotNull Iterator<U> iterator) {
        this.iterator = iterator;
    }

    private volatile boolean isIteratorListGenerated;

    private void concatList() {
        if (this.concatedList.isEmpty()) {
            this.generateListFromIterator();
            this.elements.iterator().forEachRemaining(this.concatedList::add);
            this.iteratorList.iterator().forEachRemaining(this.concatedList::add);
        }
    }

    private void generateListFromIterator() {
        if (this.isIteratorListGenerated) return;
        this.iteratorList.clear();
        if (this.iterator != null) {
            while (this.iterator.hasNext()) {
                this.iteratorList.add(this.iterator.next());
            }
        }
        this.isIteratorListGenerated = true;
    }

    @Override
    public int size() {
        this.generateListFromIterator();
        return this.iteratorList.size() + this.elements.size();
    }

    @Override
    public boolean isEmpty() {
        return this.iterator == null && this.elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        this.generateListFromIterator();
        return this.elements.contains(o) || this.iteratorList.contains(o);
    }

    @NotNull
    @Override
    public Iterator<U> iterator() {
        return this.iterator == null ? this.elements.iterator() : new MergedIterator<>(this.elements.iterator(), this.iterator);
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        this.generateListFromIterator();
        return Stream.concat(Arrays.stream(this.elements.toArray()), Arrays.stream(this.iteratorList.toArray())).toArray();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        this.generateListFromIterator();
        return Stream.concat(Arrays.stream(this.elements.toArray()), Arrays.stream(this.iteratorList.toArray())).toArray(value -> a);
    }

    @Override
    public boolean add(U u) {
        return this.elements.add(u);
    }

    @Override
    public boolean remove(Object o) {
        this.generateListFromIterator();
        boolean isRemovingFromIterator = this.iteratorList.remove(o);
        if (isRemovingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
        }
        return this.elements.remove(o) || isRemovingFromIterator;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        return this.iteratorList.containsAll(c) || this.elements.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends U> c) {
        return this.elements.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends U> c) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        boolean isRemovingFromIterator = this.iteratorList.removeAll(c);
        if (isRemovingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
        }
        return this.elements.removeAll(c) || isRemovingFromIterator;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        boolean isRetainingFromIterator = this.iteratorList.retainAll(c);
        if (isRetainingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
        }
        return this.elements.retainAll(c) || isRetainingFromIterator;
    }

    @Override
    public void clear() {
        this.iterator = null;
        this.elements.clear();
        this.concatedList.clear();
        this.iteratorList.clear();
        this.isIteratorListGenerated = false;
    }

    @Override
    public U get(int index) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public U set(int index, U element) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public void add(int index, U element) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public U remove(int index) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator() {
        this.concatList();
        return this.concatedList.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator(int index) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @NotNull
    @Override
    public List<U> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }
}
