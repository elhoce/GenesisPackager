package com.tifires.mocks;

import java.io.Serializable;

public class TestResource implements Serializable {
    private static final long serialVersionUID = 633195075276020141L;
    private String name;
    private int number;

    public TestResource(String name, int number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
