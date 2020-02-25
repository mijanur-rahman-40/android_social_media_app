package com.example.social_media_app.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Models.ModelUser;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.ChatActivity;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterChatList extends RecyclerView.Adapter<AdapterChatList.MyHolder> {
    private Context context;
    private List<ModelUser> userList;

    private HashMap<String, String> lastMessageMap;

    // constructor
    public AdapterChatList(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        this.lastMessageMap = new HashMap<>();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // inflate layout row_chat list.paths
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, viewGroup, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        // get data
        final String hisUid = userList.get(position).getUid();
        String userName = userList.get(position).getName();
        String userProfile = userList.get(position).getImage();
        String lastMessage = lastMessageMap.get(hisUid);

        // set data
        holder.nameText.setText(userName);
        if (lastMessage == null || lastMessage.equals("default")) {
            holder.lastMessage.setVisibility(View.GONE);
        } else {
            holder.lastMessage.setVisibility(View.VISIBLE);
            holder.lastMessage.setText(lastMessage);
        }

        try {
            Picasso.get().load(userProfile).placeholder(R.drawable.ic_default_image).into(holder.profileImage);
        } catch (Exception e) {
            Picasso.get().load(R.drawable.ic_default_image).into(holder.profileImage);
        }

        // set online status others user in chat list
        if (userList.get(position).getOnlineStatus().equals("online")) {
            // online
           holder.onlineStatus.setImageResource(R.drawable.circle_online);
        } else {
            // offline
            holder.onlineStatus.setImageResource(R.drawable.circle_offline);
        }
        // handle click of user in chat list
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start chat activity with that user
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUID", hisUid);
                context.startActivity(intent);
            }
        });

    }

    public void setLastMessageMap(String userId, String lastMessage) {
        lastMessageMap.put(userId, lastMessage);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        // views of row_chat list.paths
        ImageView profileImage, onlineStatus;
        TextView nameText, lastMessage;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            // init views
            profileImage = itemView.findViewById(R.id.userProfileImageView);
            onlineStatus = itemView.findViewById(R.id.onlineStatusImageView);
            nameText = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessageTextView);
        }
    }
}
