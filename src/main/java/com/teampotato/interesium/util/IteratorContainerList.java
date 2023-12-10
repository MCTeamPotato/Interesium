package com.teampotato.interesium;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IteratorContainerList<U> implements List<U> {
    public @Nullable Iterator<U> iterator;
    public final List<U> elements = new ReferenceArrayList<>();

    public IteratorContainerList(@NotNull Iterator<U> iterator) {
        this.iterator = iterator;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return this.iterator == null && this.elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<U> iterator() {
        return this.iterator == null ? this.elements.iterator() : new MergedIterator<>(this.elements.iterator(), this.iterator);
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(U u) {
        return this.elements.add(u);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends U> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends U> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        this.iterator = null;
        this.elements.clear();
    }

    @Override
    public U get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public U set(int index, U element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, U element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public U remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ListIterator<U> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<U> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }
}
