package com.teampotato.interesium.api.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IteratorContainerList<U> implements List<U> {
    public @Nullable Iterator<U> iterator;
    public final ObjectArrayList<U> elements = new ObjectArrayList<>();

    public final ObjectArrayList<U> iteratorList = new ObjectArrayList<>();

    public final ObjectArrayList<U> concatedList = new ObjectArrayList<>();

    private volatile boolean isIteratorListGenerated;
    private volatile boolean isListConcated;

    public IteratorContainerList(@NotNull Iterator<U> iterator) {
        this.iterator = iterator;
    }

    /**
     * Allowed method, thank you ;)
     **/
    @Override
    public boolean isEmpty() {
        return this.iterator == null && this.elements.isEmpty();
    }

    /**
     * Allowed method, thank you ;)
     **/
    @NotNull
    @Override
    public Iterator<U> iterator() {
        return this.iterator == null ? this.elements.iterator() : new MergedIterator<>(this.elements.iterator(), this.iterator);
    }

    /**
     * Allowed method, thank you ;)
     **/
    @Override
    public void clear() {
        this.iterator = null;
        this.elements.clear();
        this.concatedList.clear();
        this.isListConcated = false;
        this.iteratorList.clear();
        this.isIteratorListGenerated = false;
    }

    /**
     * If you still have a conscience, refrain from using the method that calls this.
     **/
    private void concatList() {
        if (!this.isListConcated) {
            this.concatedList.clear();
            this.generateListFromIterator();
            this.elements.iterator().forEachRemaining(this.concatedList::add);
            this.iteratorList.iterator().forEachRemaining(this.concatedList::add);
            this.isListConcated = true;
        }
    }

    /**
     * If you still have a conscience, refrain from using the method that calls this.
     **/
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

    /**
     * If you still have a conscience, refrain from using the method ;)
     **/
    @Override
    public int size() {
        this.generateListFromIterator();
        return this.iteratorList.size() + this.elements.size();
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean contains(Object o) {
        this.generateListFromIterator();
        return this.elements.contains(o) || this.iteratorList.contains(o);
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        this.concatList();
        return this.concatedList.toArray();
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @NotNull
    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        this.concatList();
        return this.concatedList.toArray(a);
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean add(U u) {
        this.isListConcated = false;
        return this.elements.add(u);
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean remove(Object o) {
        this.generateListFromIterator();
        boolean isRemovingFromIterator = this.iteratorList.remove(o);
        if (isRemovingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
            this.isListConcated = false;
        }
        return this.elements.remove(o) || isRemovingFromIterator;
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        return this.iteratorList.containsAll(c) || this.elements.containsAll(c);
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean addAll(@NotNull Collection<? extends U> c) {
        return this.elements.addAll(c);
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        boolean isRemovingFromIterator = this.iteratorList.removeAll(c);
        if (isRemovingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
            this.isListConcated = false;
        }
        return this.elements.removeAll(c) || isRemovingFromIterator;
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        this.generateListFromIterator();
        boolean isRetainingFromIterator = this.iteratorList.retainAll(c);
        if (isRetainingFromIterator) {
            this.iterator = this.iteratorList.iterator();
            this.isIteratorListGenerated = false;
            this.concatedList.clear();
            this.isListConcated = false;
        }
        return this.elements.retainAll(c) || isRetainingFromIterator;
    }

    /**
     * If you still have a conscience, refrain from using the method.
     **/
    @NotNull
    @Override
    public ListIterator<U> listIterator() {
        this.concatList();
        return this.concatedList.listIterator();
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
    public ListIterator<U> listIterator(int index) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @NotNull
    @Override
    public List<U> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends U> c) {
        throw new UnsupportedOperationException("Index-based operations are not allowed by IteratorContainerList");
    }
}
