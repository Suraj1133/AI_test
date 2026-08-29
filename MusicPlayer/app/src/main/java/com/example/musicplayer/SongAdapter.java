package com.example.musicplayer;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    List<Song> songs;
    OnSongClickListener listener;


    public SongAdapter(List<Song> songs,OnSongClickListener listener){
        this.songs = songs;
        this.listener=listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, artist;
        ImageView songIcon;
        public ViewHolder(View view){
            super(view);


            title = view.findViewById(R.id.songTitle);
            artist = view.findViewById(R.id.songArtist);
            songIcon = view.findViewById(R.id.songIcon);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Song song = songs.get(position);

        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        Bitmap art = getAlbumArt(song.getPath());
        if(art != null){
            holder.songIcon.setImageBitmap(art);
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onSongClick(song);
        });
    }

    Bitmap getAlbumArt(String path){

        try{

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);

            byte[] art = retriever.getEmbeddedPicture();

            if(art != null){
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
}
