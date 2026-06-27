package com.example.ticktrack.models;

public class ActivityModel {
    private int id;
    private String actionType;
    private String description;
    private String createdAt;

    public ActivityModel(int id, String actionType, String description, String createdAt) {
        this.id = id;
        this.actionType = actionType;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getActionType() { return actionType; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
}
