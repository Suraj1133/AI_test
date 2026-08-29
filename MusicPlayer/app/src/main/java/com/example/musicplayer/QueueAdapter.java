package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
    private final List<MediaItem> items = new ArrayList<>();
    private final QueueActionListener listener;
    private int currentIndex = -1;

    public QueueAdapter(QueueActionListener listener) {
        this.listener = listener;
    }

    public void submit(List<MediaItem> queue, int playingIndex) {
        items.clear();
        items.addAll(queue);
        currentIndex = playingIndex;
        notifyDataSetChanged();
    }

    public void moveItem(int from, int to) {
        MediaItem item = items.remove(from);
        items.add(to, item);
        if (currentIndex == from) {
            currentIndex = to;
        } else if (from < currentIndex && to >= currentIndex) {
            currentIndex--;
        } else if (from > currentIndex && to <= currentIndex) {
            currentIndex++;
        }
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = items.get(position);
        CharSequence title = item.mediaMetadata.title;
        CharSequence artist = item.mediaMetadata.artist;
        holder.title.setText((position == currentIndex ? "▶  " : "")
                + (title == null ? "Unknown soundtrack" : title));
        holder.artist.setText(artist == null ? "Unknown artist" : artist);
        holder.remove.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onRemove(adapterPosition);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onPlay(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface QueueActionListener {
        void onPlay(int position);
        void onRemove(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        ImageButton remove;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.queueTitle);
            artist = view.findViewById(R.id.queueArtist);
            remove = view.findViewById(R.id.queueRemove);
        }
    }
}
