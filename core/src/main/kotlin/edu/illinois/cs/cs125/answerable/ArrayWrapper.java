package edu.illinois.cs.cs125.answerable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Random;

/**
 * Wraps an array, either of primitive types or references.
 * Must be written in Java to get the magic of {@link MethodHandle#invoke(Object...)} compilation.
 */
class ArrayWrapper {

    private MethodHandle getter, setter;
    private Object theArray;
    private int length;

    ArrayWrapper(@NotNull Object arr) {
        getter = MethodHandles.arrayElementGetter(arr.getClass()).bindTo(arr);
        setter = MethodHandles.arrayElementSetter(arr.getClass()).bindTo(arr);
        theArray = arr;
        length = Array.getLength(arr);
    }

    @Nullable Object get(int index) throws Throwable {
        return getter.invoke(index);
    }

    void set(int index, @Nullable Object value) throws Throwable {
        setter.invoke(index, value);
    }

    int getSize() {
        return length;
    }

    @Nullable Object random(@NotNull Random random) throws Throwable {
        return get(random.nextInt(length));
    }

    @NotNull Object getArray() {
        return theArray;
    }

}
