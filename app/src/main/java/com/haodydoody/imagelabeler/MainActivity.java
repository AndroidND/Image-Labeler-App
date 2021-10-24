package com.haodydoody.imagelabeler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.haodydoody.imagelabeler.data.CloudWorker;

import java.util.concurrent.TimeUnit;

import static android.graphics.BitmapFactory.decodeFile;
import static androidx.work.NetworkType.CONNECTED;
import static com.haodydoody.imagelabeler.data.CloudWorker.CHANNEL_ID;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int GALLERY_REQUEST_CODE = 101;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton openDirectoryButton = findViewById(R.id.fab_open_directory);
        openDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDirectory();
            }
        });

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                boolean directoryOpen = getSupportFragmentManager().getBackStackEntryCount() > 0;
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(directoryOpen);
                    actionBar.setDisplayShowHomeEnabled(directoryOpen);
                }

            }
        });

        // Notification to remind user to use app 5 days from app open
        createNotificationChannel();
        createNotificationWorker();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the directory that the user selected.
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                showDirectoryContents(uri);
            }
        }

    }

    public void showDirectoryContents(Uri directoryUri) {
        DirectoryFragment directoryFragment = DirectoryFragment.newInstance(directoryUri);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, directoryFragment)
                .addToBackStack(directoryUri.toString())
                .commit();
//        Toast.makeText(getApplicationContext(), "showing directory contents uri:  " + directoryUri.toString(), Toast.LENGTH_LONG).show();
    }

    public void openDirectory() {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        // Provide read access to files and sub-directories in the user-selected
        // directory.
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
    }
    private void createNotificationWorker() {
        OneTimeWorkRequest cloudWorkRequest = new OneTimeWorkRequest
                .Builder(CloudWorker.class)
                .setInitialDelay(5, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueue(cloudWorkRequest);
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private final int OPEN_DIRECTORY_REQUEST_CODE = 0xf11e;
}