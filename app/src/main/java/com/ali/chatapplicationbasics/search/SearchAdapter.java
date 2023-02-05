package com.ali.chatapplicationbasics.search;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {


    private static OnItemClickListener listener;
    private List<SearchList> searchLists;
    private final String user;
    private final Context context;
    public SearchAdapter(List<SearchList> searchLists, Context context, OnItemClickListener listener,
                         String user) {
        this.searchLists = searchLists;
        this.context = context;
        this.user = user;
        SearchAdapter.listener = listener;
    }

    public void updateData(List<SearchList> searchLists) {
        this.searchLists = searchLists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SearchViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.search_adapter_layout, null));
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        SearchList list = searchLists.get(position);

        holder.userName.setText(list.getUsername());
        if (list.getProfile() != null) {
            if (!list.getProfile().isEmpty())
            {
                Picasso.get().load(Uri.parse(list.getProfile())).into(holder.profile);
            }
        }
        String reason;
        if (list.getFriendList() == null) {
            holder.userAction.setImageDrawable(context.getDrawable(R.drawable.search_action));
            reason = "add";
        } else if (!list.getFriendList().contains(user)) {
            holder.userAction.setImageDrawable(context.getDrawable(R.drawable.search_action));
            reason = "add";
        } else {
            reason = "remove";
            holder.userAction.setImageDrawable(context.getDrawable(R.drawable.check_mark_icon));
        }
        holder.userAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(
                        list.getUserId(), list.getUsername(), list.getFriendList(), list.getGroupList(),
                        reason, holder.userAction
                );
            }
        });


    }

    @Override
    public int getItemCount() {
        return searchLists.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String userId, String name, List<String> friendList, HashMap<String, String> groupList, String reason,
                         View view);
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView profile;
        private final CircleImageView userAction;
        private final TextView userName;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);

            profile = itemView.findViewById(R.id.search_image);
            userAction = itemView.findViewById(R.id.search_action);
            userName = itemView.findViewById(R.id.search_name);


        }
    }
}
