package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private final List<Song> songs;
    private final OnSongClickListener listener;
    private final OnSongMoreListener moreListener;

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this(songs, listener, null);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener,
                       OnSongMoreListener moreListener) {
        this.songs = songs;
        this.listener = listener;
        this.moreListener = moreListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        ImageView songIcon;
        ImageButton more;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.songTitle);
            artist = view.findViewById(R.id.songArtist);
            songIcon = view.findViewById(R.id.songIcon);
            more = view.findViewById(R.id.songMore);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        ArtworkLoader.loadInto(
                holder.itemView.getContext(), holder.songIcon, song.getContentUri());
        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        holder.more.setVisibility(moreListener == null ? View.GONE : View.VISIBLE);
        holder.more.setOnClickListener(v -> {
            if (moreListener != null) moreListener.onMore(song);
        });
    }

    @Override public int getItemCount() { return songs.size(); }

    public interface OnSongClickListener { void onSongClick(Song song); }
    public interface OnSongMoreListener { void onMore(Song song); }
}
