package com.sixsense.model.retention;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sixsense.model.interfaces.IEquatable;

import java.time.Instant;
import java.util.Objects;

public class DatabaseVariable implements IEquatable<DatabaseVariable> {
    private String name;
    private String value;
    private Instant collectedAt;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for complete constructors - where all arguments are known */
    public DatabaseVariable() {
        this.name = "";
        this.value = "";
        this.collectedAt = Instant.now();
    }

    public DatabaseVariable(String name, String value, Instant collectedAt) {
        this.name = name;
        this.value = value;
        this.collectedAt = collectedAt;
    }

    //Convenience method for declaring empty variables
    public static DatabaseVariable Empty(){
        return new DatabaseVariable();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseVariable withName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DatabaseVariable withValue(String value) {
        this.value = value;
        return this;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public DatabaseVariable withCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
        return this;
    }

    @JsonIgnore
    public boolean isEmpty(){
        return this.name.isBlank() && this.value.isBlank();
    }

    @Override
    public boolean weakEquals(DatabaseVariable other) {
        return this.name.equals(other.name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            return this.equals((DatabaseVariable) other);
        }
    }

    public boolean equals(DatabaseVariable other) {
        return this.weakEquals(other) && this.value.equals(other.value);
    }

    @Override
    public boolean strongEquals(DatabaseVariable other) {
        return this.equals(other) && this.collectedAt.equals(other.collectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, collectedAt);
    }

    @Override
    public String toString() {
        return "DatabaseVariable{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            ", collectedAt=" + collectedAt +
            '}';
    }
}
