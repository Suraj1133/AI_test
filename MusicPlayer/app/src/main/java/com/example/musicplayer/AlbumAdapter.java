package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder>{

    List<Album> albums;
    Context context;
    OnAlbumClickListener listener;

    public AlbumAdapter(Context context, List<Album> albums, OnAlbumClickListener listener){
        this.context = context;
        this.albums = albums;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView albumCover;
        TextView albumName;
        TextView albumArtist;

        public ViewHolder(View view){
            super(view);

            albumCover = view.findViewById(R.id.albumCover);
            albumName = view.findViewById(R.id.albumName);
            albumArtist = view.findViewById(R.id.albumArtist);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_album, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position){

        Album album = albums.get(position);

        holder.albumName.setText(album.name);
        holder.albumArtist.setText(album.artist);

        if(album.albumArt != null){
            Bitmap bitmap = BitmapFactory.decodeFile(album.albumArt);
            holder.albumCover.setImageBitmap(bitmap);
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onAlbumClick(album);
        });
    }

    @Override
    public int getItemCount(){
        return albums.size();
    }

    public interface OnAlbumClickListener{
        void onAlbumClick(Album album);
    }
}
