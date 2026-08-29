package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
    private final Context context;
    private final List<MusicFolder> folders;
    private final OnFolderClickListener listener;

    public FolderAdapter(Context context, List<MusicFolder> folders,
                         OnFolderClickListener listener) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_folder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicFolder folder = folders.get(position);
        holder.name.setText(folder.name);
        holder.details.setText(String.format(
                Locale.getDefault(), "%d tracks", folder.trackCount));
        ArtworkLoader.loadInto(context, holder.artwork, folder.artworkUri);
        holder.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
    }

    @Override public int getItemCount() { return folders.size(); }

    public interface OnFolderClickListener { void onFolderClick(MusicFolder folder); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView artwork;
        TextView name;
        TextView details;

        ViewHolder(View view) {
            super(view);
            artwork = view.findViewById(R.id.folderArtwork);
            name = view.findViewById(R.id.folderName);
            details = view.findViewById(R.id.folderDetails);
        }
    }
}
