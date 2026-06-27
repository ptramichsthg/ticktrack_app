package com.example.ticktrack.models;

public class CategoryModel {
    private int id;
    private String name;
    private String color;
    private int totalTickets; // To store how many tickets use this category

    public CategoryModel(int id, String name, String color, int totalTickets) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.totalTickets = totalTickets;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getTotalTickets() { return totalTickets; }
}
