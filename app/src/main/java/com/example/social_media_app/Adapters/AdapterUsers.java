package com.example.social_media_app.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Models.ModelUser;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.ChatActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyUserHolder> {

    Context context;
    List<ModelUser> userList;

    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyUserHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        // inflate layout(row_user.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, viewGroup,false);

        return new MyUserHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyUserHolder myHolder, int position) {

        // get data
        final String hisUID  = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        // set data
        myHolder.nameText.setText(userName);
        myHolder.emailText.setText(userEmail);

        try {
            Picasso.get()
                    .load(userImage)
                    .placeholder(R.drawable.ic_default_image)
                    .into(myHolder.myAvatarImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // handle item click
        myHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Click user from user to start chatting/messaging
                * Start activity by putting UID of receiver
                * We will use that UID to identify the user we are gonna chat
                * */

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUID",hisUID);
                context.startActivity(intent);
                Toast.makeText(context, "" + userEmail, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // view holder classes
    class MyUserHolder extends RecyclerView.ViewHolder {

        ImageView myAvatarImage;
        TextView nameText, emailText;

        public MyUserHolder(@NonNull View itemView) {
            super(itemView);

            // init views
            myAvatarImage = itemView.findViewById(R.id.avatarImageView);
            nameText = itemView.findViewById(R.id.nameTextView);
            emailText = itemView.findViewById(R.id.emailTextView);

        }
    }
}
