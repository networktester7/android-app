package com.example.hospitaldatabase; // Your package name

public class LocationItem {
    private int id;
    private String name;

    public LocationItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        // This method is important for the Spinner to display the name
        return name;
    }
}