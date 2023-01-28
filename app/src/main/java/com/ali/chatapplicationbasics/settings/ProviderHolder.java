package com.ali.chatapplicationbasics.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.chatapplicationbasics.R;

import java.util.Arrays;
import java.util.List;

public class ProviderHolder extends RecyclerView.Adapter<ProviderHolder.ProviderViewHolder> {

    private final OnItemClickListener listener;
    private final List<String> providerList;
    private final List<String> providers = Arrays.asList("google.com", "password");
    private final Context context;
    public ProviderHolder(List<String> providerList, Context context, OnItemClickListener listener) {
        this.providerList = providerList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProviderViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.provider_adapter_layout, null));
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        String text;
        int icon;
        int action = R.drawable.add_icon;
        String desc = "false";
        switch (position) {
            case 0:
                text = "Google";
                icon = R.drawable.google_icon;
                if (providerList.contains("google.com")) {
                    action = R.drawable.check_mark_icon;
                    desc = "true";
                }
                holder.providerText.setText(text);
                holder.providerImage.setImageResource(icon);
                holder.providerAction.setImageResource(action);
                holder.providerAction.setContentDescription(desc);
                return;
            case 1:
                text = "Email-Password";
                icon = R.drawable.email_icon;
                if (providerList.contains("password")) {
                    action = R.drawable.check_mark_icon;
                    desc = "true";
                }
                holder.providerText.setText(text);
                holder.providerImage.setImageResource(icon);
                holder.providerAction.setImageResource(action);
                holder.providerAction.setContentDescription(desc);
        }
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String actionId, String provider, ImageView view);
    }

    public class ProviderViewHolder extends RecyclerView.ViewHolder {

        private final ImageView providerImage;
        private final ImageView providerAction;
        private final TextView providerText;

        public ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            providerImage = itemView.findViewById(R.id.provider_icon);
            providerAction = itemView.findViewById(R.id.provider_action);
            providerText = itemView.findViewById(R.id.provider_text);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(providerAction.getContentDescription().toString(),
                            providerText.getText().toString(), providerAction);
                }
            });
        }
    }
}
