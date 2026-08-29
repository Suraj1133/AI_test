package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkLoader {
    private static final int MAX_ARTWORK_SIZE = 512;
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 16)) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ArtworkLoader() {}

    public static void loadInto(Context context, ImageView target, Uri uri) {
        target.setImageResource(R.drawable.player_art_placeholder);
        if (uri == null) return;

        String key = uri.toString();
        target.setTag(key);
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = readArtwork(appContext, uri);
            if (bitmap != null) CACHE.put(key, bitmap);
            MAIN.post(() -> {
                if (key.equals(target.getTag()) && bitmap != null) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    public static void loadFirstInto(Context context, ImageView target, List<Uri> uris) {
        target.setImageResource(R.drawable.player_art_placeholder);
        if (uris == null || uris.isEmpty()) return;

        String key = uris.get(0).toString();
        target.setTag(key);
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = null;
            for (Uri uri : uris) {
                bitmap = readArtwork(appContext, uri);
                if (bitmap != null) break;
            }
            final Bitmap result = bitmap;
            if (result != null) CACHE.put(key, result);
            MAIN.post(() -> {
                if (key.equals(target.getTag()) && result != null) {
                    target.setImageBitmap(result);
                }
            });
        });
    }

    private static Bitmap readArtwork(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            byte[] bytes = retriever.getEmbeddedPicture();
            if (bytes == null) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);

            int sampleSize = 1;
            while (bounds.outWidth / sampleSize > MAX_ARTWORK_SIZE
                    || bounds.outHeight / sampleSize > MAX_ARTWORK_SIZE) {
                sampleSize *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }
}
