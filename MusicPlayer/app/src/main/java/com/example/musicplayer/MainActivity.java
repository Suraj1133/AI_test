package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 100;

    private final List<Song> sourceSongs = new ArrayList<>();
    private final List<Album> sourceAlbums = new ArrayList<>();
    private final List<MusicFolder> sourceFolders = new ArrayList<>();
    private final List<Song> visibleSongs = new ArrayList<>();
    private final List<Album> visibleAlbums = new ArrayList<>();
    private final List<MusicFolder> visibleFolders = new ArrayList<>();
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();

    private MusicRepository repository;
    private SongAdapter songAdapter;
    private AlbumAdapter albumAdapter;
    private FolderAdapter folderAdapter;
    private MiniPlayerController miniPlayerController;
    private EditText searchInput;
    private int selectedTab;
    private int songSort;
    private int albumSort;
    private int folderSort;
    private boolean libraryLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        repository = new MusicRepository(getContentResolver());
        miniPlayerController = new MiniPlayerController(this);
        searchInput = findViewById(R.id.librarySearch);
        ImageButton sortButton = findViewById(R.id.librarySort);
        View libraryToolbar = findViewById(R.id.libraryToolbar);
        View personalPanel = findViewById(R.id.personalPanel);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Albums"));
        tabLayout.addTab(tabLayout.newTab().setText("Folders"));
        tabLayout.addTab(tabLayout.newTab().setText("Personal"));

        RecyclerView recyclerSongs = findViewById(R.id.recyclerSongs);
        RecyclerView recyclerAlbums = findViewById(R.id.recyclerAlbums);
        RecyclerView recyclerFolders = findViewById(R.id.recyclerFolders);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));
        recyclerFolders.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(
                visibleSongs,
                song -> openPlayer(song.getContentUri().toString()),
                song -> SongActions.show(this, song)
        );
        albumAdapter = new AlbumAdapter(this, visibleAlbums, album -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            intent.putExtra("albumIds", album.getAlbumIds());
            intent.putExtra("albumName", album.name);
            intent.putExtra("albumArtist", album.artist);
            startActivity(intent);
        });
        folderAdapter = new FolderAdapter(this, visibleFolders, folder -> {
            Intent intent = new Intent(this, FolderActivity.class);
            intent.putExtra("folderName", folder.name);
            intent.putExtra("folderPath", folder.path);
            startActivity(intent);
        });
        recyclerSongs.setAdapter(songAdapter);
        recyclerAlbums.setAdapter(albumAdapter);
        recyclerFolders.setAdapter(folderAdapter);
        recyclerAlbums.setVisibility(View.GONE);
        recyclerFolders.setVisibility(View.GONE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                recyclerSongs.setVisibility(selectedTab == 0 ? View.VISIBLE : View.GONE);
                recyclerAlbums.setVisibility(selectedTab == 1 ? View.VISIBLE : View.GONE);
                recyclerFolders.setVisibility(selectedTab == 2 ? View.VISIBLE : View.GONE);
                personalPanel.setVisibility(selectedTab == 3 ? View.VISIBLE : View.GONE);
                libraryToolbar.setVisibility(selectedTab == 3 ? View.GONE : View.VISIBLE);
                updateSearchHint();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        sortButton.setOnClickListener(this::showSortMenu);
        findViewById(R.id.personalFavorites).setOnClickListener(v ->
                openPersonalCollection(PersonalLibraryRepository.FAVORITES, "Favorites"));
        findViewById(R.id.personalRecent).setOnClickListener(v ->
                openPersonalCollection(PersonalLibraryRepository.RECENT, "Recently played"));
        findViewById(R.id.personalMostPlayed).setOnClickListener(v ->
                openPersonalCollection(PersonalLibraryRepository.MOST_PLAYED, "Most played"));
        findViewById(R.id.personalPlaylists).setOnClickListener(v ->
                startActivity(new Intent(this, PlaylistsActivity.class)));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        if (hasAudioPermission()) {
            loadLibrary();
            requestNotificationPermissionIfNeeded();
        } else {
            requestRequiredPermissions();
        }
    }

    private void updateSearchHint() {
        searchInput.setHint(selectedTab == 0 ? "Search songs, artists or albums"
                : selectedTab == 1 ? "Search albums or artists" : "Search folders");
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        if (selectedTab == 0) {
            menu.getMenu().add(0, 0, 0, "Title");
            menu.getMenu().add(0, 1, 1, "Artist");
            menu.getMenu().add(0, 2, 2, "Album");
            menu.getMenu().add(0, 3, 3, "Duration");
            menu.getMenu().add(0, 4, 4, "Recently added");
        } else if (selectedTab == 1) {
            menu.getMenu().add(0, 0, 0, "Album name");
            menu.getMenu().add(0, 1, 1, "Artist");
            menu.getMenu().add(0, 2, 2, "Track count");
        } else {
            menu.getMenu().add(0, 0, 0, "Folder name");
            menu.getMenu().add(0, 1, 1, "Track count");
        }
        menu.setOnMenuItemClickListener(item -> {
            if (selectedTab == 0) songSort = item.getItemId();
            else if (selectedTab == 1) albumSort = item.getItemId();
            else folderSort = item.getItemId();
            applyFiltersAndSort();
            return true;
        });
        menu.show();
    }

    private void applyFiltersAndSort() {
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);

        visibleSongs.clear();
        for (Song song : sourceSongs) {
            if (query.isEmpty()
                    || song.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || song.getArtist().toLowerCase(Locale.ROOT).contains(query)
                    || song.getAlbum().toLowerCase(Locale.ROOT).contains(query)) {
                visibleSongs.add(song);
            }
        }
        Comparator<Song> songComparator;
        switch (songSort) {
            case 1: songComparator = Comparator.comparing(
                    Song::getArtist, String.CASE_INSENSITIVE_ORDER); break;
            case 2: songComparator = Comparator.comparing(
                    Song::getAlbum, String.CASE_INSENSITIVE_ORDER); break;
            case 3: songComparator = Comparator.comparingLong(Song::getDuration); break;
            case 4: songComparator = Comparator.comparingLong(Song::getDateAdded).reversed(); break;
            default: songComparator = Comparator.comparing(
                    Song::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
        visibleSongs.sort(songComparator);

        visibleAlbums.clear();
        for (Album album : sourceAlbums) {
            if (query.isEmpty()
                    || album.name.toLowerCase(Locale.ROOT).contains(query)
                    || album.artist.toLowerCase(Locale.ROOT).contains(query)) {
                visibleAlbums.add(album);
            }
        }
        if (albumSort == 1) {
            visibleAlbums.sort(Comparator.comparing(
                    (Album album) -> album.artist, String.CASE_INSENSITIVE_ORDER));
        } else if (albumSort == 2) {
            visibleAlbums.sort(Comparator.comparingInt(Album::getTrackCount).reversed());
        } else {
            visibleAlbums.sort(Comparator.comparing(
                    (Album album) -> album.name, String.CASE_INSENSITIVE_ORDER));
        }

        visibleFolders.clear();
        for (MusicFolder folder : sourceFolders) {
            if (query.isEmpty()
                    || folder.name.toLowerCase(Locale.ROOT).contains(query)
                    || (folder.path != null
                    && folder.path.toLowerCase(Locale.ROOT).contains(query))) {
                visibleFolders.add(folder);
            }
        }
        if (folderSort == 1) {
            visibleFolders.sort(Comparator.comparingInt(
                    (MusicFolder folder) -> folder.trackCount).reversed());
        } else {
            visibleFolders.sort(Comparator.comparing(
                    (MusicFolder folder) -> folder.name, String.CASE_INSENSITIVE_ORDER));
        }

        songAdapter.notifyDataSetChanged();
        albumAdapter.notifyDataSetChanged();
        folderAdapter.notifyDataSetChanged();
    }

    private void openPersonalCollection(String type, String title) {
        Intent intent = new Intent(this, PersonalCollectionActivity.class);
        intent.putExtra("collectionType", type);
        intent.putExtra("collectionTitle", title);
        startActivity(intent);
    }

    private void openPlayer(String songUri) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongUri", songUri);
        startActivity(intent);
    }

    private boolean hasAudioPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_AUDIO
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_AUDIO
                : android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST);
        }
    }

    private void loadLibrary() {
        if (libraryLoaded) return;
        libraryLoaded = true;
        libraryExecutor.execute(() -> {
            try {
                List<Song> songs = repository.getAllSongs();
                List<Album> albums = repository.getAlbums();
                List<MusicFolder> folders = repository.getFolders();
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    sourceSongs.clear();
                    sourceSongs.addAll(songs);
                    sourceAlbums.clear();
                    sourceAlbums.addAll(albums);
                    sourceFolders.clear();
                    sourceFolders.addAll(folders);
                    applyFiltersAndSort();
                });
            } catch (SecurityException exception) {
                libraryLoaded = false;
                runOnUiThread(() -> Toast.makeText(this,
                        "Audio permission is required to load music",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSION_REQUEST) {
            if (hasAudioPermission()) loadLibrary();
            else Toast.makeText(this,
                    "Allow audio access to display your music library",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        miniPlayerController.connect();
    }

    @Override protected void onStop() {
        miniPlayerController.disconnect();
        super.onStop();
    }

    @Override protected void onDestroy() {
        libraryExecutor.shutdownNow();
        super.onDestroy();
    }
}
