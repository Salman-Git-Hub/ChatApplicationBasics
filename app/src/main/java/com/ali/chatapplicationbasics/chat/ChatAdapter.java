package com.ali.chatapplicationbasics.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatList> chatLists;
    private final Context context;

    private final String userId;

    public ChatAdapter(List<ChatList> chatLists, Context context, String userId) {
        this.chatLists = chatLists;
        this.context = context;
        this.userId = userId;
    }

    public void updateData(List<ChatList> chatLists) {
        this.chatLists = chatLists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_adapter_layout, null));
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatList list = chatLists.get(position);
        if (list.getSender().equals(userId)) {
            holder.sendLayout.setVisibility(View.VISIBLE);
            holder.recvLayout.setVisibility(View.GONE);
            holder.sendText.setText(list.getMessage());
            holder.sendTime.setText(list.getTime());
        } else {
            holder.sendLayout.setVisibility(View.GONE);
            holder.recvLayout.setVisibility(View.VISIBLE);
            holder.recvText.setText(list.getMessage());
            holder.recvTime.setText(list.getTime());
        }

    }

    @Override
    public int getItemCount() {
        return chatLists.size();
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout recvLayout;
        private final LinearLayout sendLayout;
        private final TextView recvText;
        private final TextView recvTime;
        private final TextView sendText;
        private final TextView sendTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            // recv
            recvLayout = itemView.findViewById(R.id.recv_layout);
            recvText = itemView.findViewById(R.id.recv_text);
            recvTime = itemView.findViewById(R.id.recv_time);

            // send
            sendLayout = itemView.findViewById(R.id.send_layout);
            sendText = itemView.findViewById(R.id.send_text);
            sendTime = itemView.findViewById(R.id.send_time);

        }
    }
}
