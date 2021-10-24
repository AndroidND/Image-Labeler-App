package com.haodydoody.imagelabeler.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ImageViewModel extends AndroidViewModel {
    private ImageHistoryRepository repository;
    private LiveData<List<ImageHistory>> allImageHistory;


    public ImageViewModel(@NonNull Application application) {
        super(application);
        repository = new ImageHistoryRepository(application);
        allImageHistory = repository.getAllImageHistory();
    }

    public LiveData<List<ImageHistory>> getAllImageHistory() { return allImageHistory; }
    public ImageHistory getImageHistory (ImageHistory imageHistory) { return repository.get(imageHistory); }
    public void addImageHistory(ImageHistory imageHistory) { repository.insert(imageHistory); }
    public void updateImageHistory(ImageHistory imageHistory) { repository.update(imageHistory); }
}
