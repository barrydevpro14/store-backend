package org.store.common.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class Error implements Comparable<Error> {
    private String title;
    private String description;
    public Error(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public int compareTo(Error error) {
        return this.title.compareTo(error.getTitle());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Error error)) return false;
        return Objects.equals(this.title, error.getTitle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

}
