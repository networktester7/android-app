package com.example.hospitaldatabase;

public class InventoryItem {
    private int id;
    private String name;
    private int quantity;

    public InventoryItem(int id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    // This method will be used by the ArrayAdapter to display the item in the ListView
    @Override
    public String toString() {
        return name + " (" + quantity + ")";
    }
}