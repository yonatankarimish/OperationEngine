package com.sixsense.model.interfaces;

public interface IEquatable<T>{
    boolean weakEquals(T other);
    boolean strongEquals(T other);
}
