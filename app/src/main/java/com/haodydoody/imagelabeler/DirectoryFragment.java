package com.haodydoody.imagelabeler;


import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haodydoody.imagelabeler.data.ImageHistory;
import com.haodydoody.imagelabeler.data.ImageViewModel;

import java.io.IOException;
import java.util.List;

/**
 * Fragment that shows a list of documents in a directory.
 */
public class DirectoryFragment extends Fragment {

    private Uri directoryUri;
    private RecyclerView recyclerView;
    private DirectoryEntryAdapter adapter;
    private static final String TAG = "DirectoryFragment";
    View view;
    ImageViewModel imageViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Bundle bundle = getArguments();
        assert bundle != null;
        String uriString = bundle.getString(ARG_DIRECTORY_URI);
        directoryUri = Uri.parse(uriString);
        Log.d(TAG, "onCreateView: " + directoryUri.toString());

        imageViewModel = ViewModelProviders.of(this).get(ImageViewModel.class);
        observerSetup();

        view = inflater.inflate(R.layout.fragment_directory, container, false);
        recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new DirectoryEntryAdapter(getActivity(), directoryUri);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((MainActivity) requireActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main, menu);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_rename_local:
                try {
                    adapter.renameImages(imageViewModel, R.id.action_rename_local);
                    adapter.notifyDataSetChanged();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.action_rename_cloud:
                try {
                    adapter.renameImages(imageViewModel, R.id.action_rename_cloud);
                    adapter.notifyDataSetChanged();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                Log.d("TAG", "onOptionsItemSelected defaul: " + item.getItemId());
                Toast.makeText(getContext(),"Please choose to rename your files using the model on your phone or the cloud.", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }


    public static DirectoryFragment newInstance (Uri directoryUri) {
        DirectoryFragment directoryFragment = new DirectoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIRECTORY_URI, directoryUri.toString());
        directoryFragment.setArguments(args);
        return directoryFragment;
    }

    private void observerSetup() {

        imageViewModel.getAllImageHistory().observe(getViewLifecycleOwner(), new Observer<List<ImageHistory>>() {
            @Override
            public void onChanged(@Nullable final List<ImageHistory> imageHistories) {
                adapter.setAllImageHistory(imageHistories);
            }
        });

    }

private static final String ARG_DIRECTORY_URI = "com.example.android.directoryselection.ARG_DIRECTORY_URI";

}
