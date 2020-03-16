package com.example.social_media_app.Models;

public class ModelPost {
    // same name as uploading post
    private String uid, userName, userEmail, userProfile, postId, postTitle, postDescription, postImage, postTime, postLikes, postComments;

    public ModelPost() {

    }

    public ModelPost(String uid, String userName, String userEmail, String userProfile, String postId, String postTitle, String postDescription, String postImage, String postTime, String postLikes, String postComments) {
        this.uid = uid;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userProfile = userProfile;
        this.postId = postId;
        this.postTitle = postTitle;
        this.postDescription = postDescription;
        this.postImage = postImage;
        this.postTime = postTime;
        this.postLikes = postLikes;
        this.postComments = postComments;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public String getPostImage() {
        return postImage;
    }

    public void setPostImage(String postImage) {
        this.postImage = postImage;
    }

    public String getPostTime() {
        return postTime;
    }

    public void setPostTime(String postTime) {
        this.postTime = postTime;
    }

    public String getPostLikes() {
        return postLikes;
    }

    public void setPostLikes(String postLikes) {
        this.postLikes = postLikes;
    }

    public String getPostComments() {
        return postComments;
    }

    public void setPostComments(String postComments) {
        this.postComments = postComments;
    }
}
