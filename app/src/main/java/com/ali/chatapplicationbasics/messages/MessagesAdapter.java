package com.ali.chatapplicationbasics.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MyViewHolder> {

    private List<MessageList> messagesLists;
    private final OnItemClickListener listener;
    public MessagesAdapter(List<MessageList> messages, OnItemClickListener listener) {
        this.messagesLists = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessagesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_adapter_layout, null));
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesAdapter.MyViewHolder holder, int position) {
        MessageList list2 = messagesLists.get(position);
        if (!list2.getProfilePic().isEmpty()) {
            Picasso.get().load(list2.getProfilePic()).into(holder.profilePic);
            holder.profilePic.setContentDescription(list2.getProfilePic());
        }
        holder.name.setText(list2.getName());
        holder.name.setContentDescription(list2.getGroupId());
        String msg = "";
        String sender = "";
        if (!list2.getLastSender().isEmpty()) {
            msg = list2.getLastSender() + ": " + list2.getLastMessage();
            sender = list2.getLastSender();
        } else {
            msg = list2.getLastMessage();
            sender = "";
        }
        holder.lastMessage.setText(msg);
        holder.lastMessage.setContentDescription(sender);

        if (list2.getUnSeenMessages() == 0) {
            holder.unSeenMessages.setVisibility(View.INVISIBLE);
            holder.unSeenMessages.setText("0");
        } else {
            holder.unSeenMessages.setVisibility(View.VISIBLE);
            System.out.println(list2.getUnSeenMessages());
            try {
                holder.unSeenMessages.setText(String.valueOf(list2.getUnSeenMessages()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }


    }

    public void updateData(List<MessageList> messagesLists) {
        this.messagesLists = messagesLists;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messagesLists.size();
    }

    public interface OnItemClickListener {
        void onItemClick(MessageList message);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView profilePic;
        private final TextView name;
        private final TextView lastMessage;
        private final TextView unSeenMessages;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.profilePic);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.last_message);
            unSeenMessages = itemView.findViewById(R.id.unseen_messages);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String sender = lastMessage.getContentDescription().toString();
                    String msg = lastMessage.getText().toString().replace(sender + ": ", "");
                    listener.onItemClick(new MessageList(name.getText().toString(),
                            name.getContentDescription().toString(),
                            msg,
                            profilePic.getContentDescription().toString(),
                            Integer.parseInt(unSeenMessages.getText().toString()), sender));
                }
            });
        }
    }
}
