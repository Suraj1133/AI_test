package com.example.musicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

public class MiniPlayerController {
    private final Activity activity;
    private final View bar;
    private final ImageView artwork;
    private final TextView title;
    private final TextView artist;
    private final ImageButton playPause;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onMediaMetadataChanged(MediaMetadata metadata) { refresh(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { refresh(); }
        @Override public void onPlaybackStateChanged(int state) { refresh(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refresh(); }
    };

    public MiniPlayerController(Activity activity) {
        this.activity = activity;
        bar = activity.findViewById(R.id.miniPlayerInclude);
        artwork = activity.findViewById(R.id.miniPlayerArtwork);
        title = activity.findViewById(R.id.miniPlayerTitle);
        artist = activity.findViewById(R.id.miniPlayerArtist);
        playPause = activity.findViewById(R.id.miniPlayerPlayPause);
        ImageButton next = activity.findViewById(R.id.miniPlayerNext);

        bar.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NowPlayingActivity.class)));
        playPause.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.isPlaying()) controller.pause(); else controller.play();
        });
        next.setOnClickListener(v -> {
            if (controller != null && controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem();
            }
        });
    }

    public void connect() {
        if (controllerFuture != null) return;
        SessionToken token = new SessionToken(
                activity, new ComponentName(activity, PlaybackService.class));
        controllerFuture = new MediaController.Builder(activity, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(listener);
                refresh();
            } catch (Exception ignored) {
                bar.setVisibility(View.GONE);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void disconnect() {
        if (controller != null) {
            controller.removeListener(listener);
            controller = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
    }

    private void refresh() {
        if (controller == null || controller.getMediaItemCount() == 0) {
            bar.setVisibility(View.GONE);
            return;
        }

        bar.setVisibility(View.VISIBLE);
        MediaMetadata metadata = controller.getMediaMetadata();
        title.setText(metadata.title == null ? "Unknown soundtrack" : metadata.title);
        artist.setText(metadata.artist == null ? "Unknown artist" : metadata.artist);
        playPause.setImageResource(controller.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);

        MediaItem item = controller.getCurrentMediaItem();
        if (item != null && !item.mediaId.isEmpty()) {
            ArtworkLoader.loadInto(activity, artwork, Uri.parse(item.mediaId));
        } else {
            artwork.setImageResource(R.drawable.player_art_placeholder);
        }
    }
}
