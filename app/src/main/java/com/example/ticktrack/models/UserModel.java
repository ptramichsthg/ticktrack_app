package com.example.ticktrack.models;

public class UserModel {
    private int id;
    private String name;
    private String email;
    private String role;
    private boolean isActive;
    private int totalTickets;

    public UserModel(int id, String name, String email, String role, boolean isActive, int totalTickets) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
        this.totalTickets = totalTickets;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getTotalTickets() { return totalTickets; }
}
