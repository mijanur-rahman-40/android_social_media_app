package com.example.social_media_app.Models;

public class ModelComment {

    String commentId,comment,timestamp,uid,userEmail,userProfile,userName;

    public ModelComment(){

    }

    public ModelComment(String commentId, String comment, String timestamp, String uid, String userEmail, String userProfile, String userName) {
        this.commentId = commentId;
        this.comment = comment;
        this.timestamp = timestamp;
        this.uid = uid;
        this.userEmail = userEmail;
        this.userProfile = userProfile;
        this.userName = userName;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
