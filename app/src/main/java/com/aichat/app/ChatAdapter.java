package com.aichat.app;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        holder.tvMessage.setText(msg.getContent());
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                holder.tvMessage.getLayoutParams();
        if (msg.isUser()) {
            params.gravity = Gravity.END;
            holder.tvMessage.setBackgroundResource(R.drawable.bg_user);
            holder.tvMessage.setTextColor(0xFFFFFFFF);
        } else {
            params.gravity = Gravity.START;
            holder.tvMessage.setBackgroundResource(R.drawable.bg_ai);
            holder.tvMessage.setTextColor(0xFF212121);
        }
        holder.tvMessage.setLayoutParams(params);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
        }
    }
}
