package com.example.ticktrack.models;

public class NotificationModel {
    private int id;
    private int userId;
    private String title;
    private String message;
    private boolean isRead;
    private String createdAt;
    private int ticketId;

    public NotificationModel(int id, int userId, String title, String message, boolean isRead, String createdAt, int ticketId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.ticketId = ticketId;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public int getTicketId() { return ticketId; }
    public void setRead(boolean read) { isRead = read; }
}
