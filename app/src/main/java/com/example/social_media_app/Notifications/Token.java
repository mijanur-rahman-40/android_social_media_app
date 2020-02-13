package com.example.social_media_app.Notifications;

public class Token {
    /* An FCM token ,or much commonly known as registrationToken
     * An ID issued by the GCM Connection servers to the client app that allows
     * it to receive messages
     */

    String token;

    public Token() {

    }

    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
