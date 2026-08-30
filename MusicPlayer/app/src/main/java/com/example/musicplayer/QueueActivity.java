package com.example.musicplayer;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends BaseActivity {
    private QueueAdapter adapter;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override public void onTimelineChanged(androidx.media3.common.Timeline timeline, int reason) {
            refreshQueue();
        }
        @Override public void onMediaItemTransition(MediaItem item, int reason) {
            refreshQueue();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        RecyclerView recyclerView = findViewById(R.id.queueRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QueueAdapter(new QueueAdapter.QueueActionListener() {
            @Override
            public void onPlay(int position) {
                if (controller != null) {
                    controller.seekToDefaultPosition(position);
                    controller.play();
                    finish();
                }
            }

            @Override
            public void onRemove(int position) {
                if (controller != null && controller.getMediaItemCount() > 1) {
                    controller.removeMediaItem(position);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder source,
                            RecyclerView.ViewHolder target) {
                        int from = source.getBindingAdapterPosition();
                        int to = target.getBindingAdapterPosition();
                        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION
                                || controller == null) {
                            return false;
                        }
                        adapter.moveItem(from, to);
                        controller.moveMediaItem(from, to);
                        return true;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return true;
                    }
                });
        helper.attachToRecyclerView(recyclerView);
        connectController();
    }

    private void connectController() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);
                refreshQueue();
            } catch (Exception ignored) {
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void refreshQueue() {
        if (controller == null) return;
        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < controller.getMediaItemCount(); i++) {
            items.add(controller.getMediaItemAt(i));
        }
        adapter.submit(items, controller.getCurrentMediaItemIndex());
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.removeListener(playerListener);
            controller = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        super.onDestroy();
    }
}
