package com.aruu.ai2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final Context context;
    private final List<ChatMessage> items;

    ChatAdapter(Context context, List<ChatMessage> items) {
        this.context = context;
        this.items = items;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final TextView bubble;
        final ImageView image;

        VH(View v) {
            super(v);
            row = v.findViewById(R.id.row);
            bubble = v.findViewById(R.id.tvBubble);
            image = v.findViewById(R.id.imgChat);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final ChatMessage m = items.get(position);
        boolean user = m.role == ChatMessage.USER;
        h.row.setGravity(user ? Gravity.END : Gravity.START);

        if (m.text.isEmpty()) {
            h.bubble.setVisibility(View.GONE);
        } else {
            h.bubble.setVisibility(View.VISIBLE);
            h.bubble.setText(m.text);
            int bg = user ? R.drawable.bg_bubble_user
                    : (m.role == ChatMessage.SYSTEM ? R.drawable.bg_bubble_sys : R.drawable.bg_bubble_ai);
            h.bubble.setBackgroundResource(bg);
            h.bubble.setOnLongClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Aruu", m.text));
                Toast.makeText(context, "Copy ho gaya", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        File f = m.imagePath.isEmpty() ? null : new File(m.imagePath);
        if (f != null && f.exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            h.image.setImageBitmap(bm);
            h.image.setVisibility(View.VISIBLE);
        } else {
            h.image.setImageBitmap(null);
            h.image.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
