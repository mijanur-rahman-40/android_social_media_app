
package com.example.social_media_app.Notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


import com.example.social_media_app.R;
import com.example.social_media_app.Views.ChatActivity;
import com.example.social_media_app.Views.PostDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class FirebaseMessaging extends FirebaseMessagingService {
    private static final String ADMIN_CHANNEL_ID = "admin_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // get current user from shared preference
        SharedPreferences sharedPreferences = getSharedPreferences("SP_USER", MODE_PRIVATE);
        String savedCurrentUser = sharedPreferences.getString("Current_USERID", "None");

        /* Now there ae two types of notifications
         * -> notificationsType ="PostNotification"
         * -> notificationsType ="ChatNotification"
         * */

        String notificationType = remoteMessage.getData().get("notificationType");
        assert notificationType != null;
        if (notificationType.equals("PostNotification")) {
            // post notification

            String sender = remoteMessage.getData().get("sender");
            String postId = remoteMessage.getData().get("postId");
            String postTitle = remoteMessage.getData().get("postTitle");
            String postDescription = remoteMessage.getData().get("postDescription");

            // if user is same that posted don not show notification
            assert sender != null;
            if (!sender.equals(savedCurrentUser)) {
                showPostNotification("" + postId, "" + postTitle, "" + postDescription);
            }
        } else if (notificationType.equals("ChatNotification")) {
            // chat notification
            String sent = remoteMessage.getData().get("sent");
            String user = remoteMessage.getData().get("user");

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            assert sent != null;
            if (firebaseUser != null && sent.equals(firebaseUser.getUid())) {
                if (!savedCurrentUser.equals(user)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        sendOreoAndAboveNotification(remoteMessage);
                    } else {
                        sendNormalNotification(remoteMessage);
                    }
                }
            }
        }
    }

    private void showPostNotification(String postId, String postTitle, String postDescription) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = new Random().nextInt(3000);

        /*
         * App targeting SDK 26 or above (Android O and above) must implement notification channels
         * and add its notifications to at least one of them
         * Let's add check if version is Oreo or higher then setup notification channel
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel(notificationManager);
        }

        // show post detail activity using post is when notification clicked
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("postId", postId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        // large icon
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.firebase_logo);

        // sound for notification
        Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "" + ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.firebase_logo)
                .setLargeIcon(largeIcon)
                .setContentTitle(postTitle)
                .setContentText(postDescription)
                .setSound(notificationSoundUri)
                .setContentIntent(pendingIntent);

        // show notification
        assert notificationManager != null;
        notificationManager.notify(notificationID, notificationBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupNotificationChannel(NotificationManager notificationManager) {
        CharSequence channelName = "New Notification";
        String channelDescription = "Device to Device post notification";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            adminChannel.setDescription(channelDescription);
            adminChannel.enableLights(true);
            adminChannel.setLightColor(Color.RED);
            adminChannel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(adminChannel);
            }
        }
    }

    private void sendNormalNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        // RemoteMessage.Notification notification = remoteMessage.getNotification();

        assert user != null;
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("hisUID", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        assert icon != null;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int j = 0;
        if (i > 0) {
            j = i;
        }
        assert notificationManager != null;
        notificationManager.notify(j, builder.build());
    }

    private void sendOreoAndAboveNotification(RemoteMessage remoteMessage) {

        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();

        assert user != null;
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("hisUID", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        assert icon != null;

        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);

        Notification.Builder builder = notification1.getOreoNotification(title, body, pendingIntent, defSoundUri, icon);

        int j = 0;
        if (i > 0) {
            j = i;
        }

        notification1.getManager().notify(j, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // update user token
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // signed in, update token
            updateToken(s);
        }
    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(tokenRefresh);
        assert user != null;
        databaseReference.child(user.getUid()).setValue(token);
    }
}

