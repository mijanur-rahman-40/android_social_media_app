package com.example.social_media_app.Models;

public class ModelChatList {
    String id; // we will need this id to get chat list, sender/receiver uid

    public ModelChatList(String id) {
        this.id = id;
    }

    public ModelChatList() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
