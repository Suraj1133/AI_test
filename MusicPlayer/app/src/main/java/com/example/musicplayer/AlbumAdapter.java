package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private final List<Album> albums;
    private final Context context;
    private final OnAlbumClickListener listener;

    public AlbumAdapter(Context context, List<Album> albums, OnAlbumClickListener listener) {
        this.context = context;
        this.albums = albums;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView albumName;
        TextView albumArtist;

        public ViewHolder(View view) {
            super(view);
            albumCover = view.findViewById(R.id.albumCover);
            albumName = view.findViewById(R.id.albumName);
            albumArtist = view.findViewById(R.id.albumArtist);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumName.setText(album.name);
        holder.albumArtist.setText(album.artist);

        holder.albumCover.setImageResource(android.R.drawable.ic_menu_gallery);
        Bitmap artwork = getFirstEmbeddedArtwork(album.songPaths);
        if (artwork != null) {
            holder.albumCover.setImageBitmap(artwork);
        }

        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    private Bitmap getFirstEmbeddedArtwork(List<String> songPaths) {
        for (String path : songPaths) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(path);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    return BitmapFactory.decodeByteArray(art, 0, art.length);
                }
            } catch (Exception ignored) {
                // Some tracks in one album may not contain embedded artwork.
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }
}
