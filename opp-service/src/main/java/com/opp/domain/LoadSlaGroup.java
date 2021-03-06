package com.opp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Created by ctobe on 6/28/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoadSlaGroup {
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadSlaGroup(String name) {
        this.name = name;
    }

    public LoadSlaGroup() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadSlaGroup that = (LoadSlaGroup) o;
        return id == that.id &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
