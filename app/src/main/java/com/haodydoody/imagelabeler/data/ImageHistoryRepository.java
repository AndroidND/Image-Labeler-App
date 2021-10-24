package com.haodydoody.imagelabeler.data;

import android.app.Application;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class ImageHistoryRepository {
    private ImageHistoryDao imageHistoryDao;
    private LiveData<List<ImageHistory>> allImageHistory;
    private boolean uriWasProcessed;

    public ImageHistoryRepository(Application application) {
        ImageHistoryDatabase database = ImageHistoryDatabase.getDatabase(application);
        imageHistoryDao = database.getImageHistoryDao();
        allImageHistory = imageHistoryDao.getAllImageHistory();

    }
    public static List<DocumentFile> toCachingList(DocumentFile[] documentFileArrayList) {
        List<DocumentFile> list= new ArrayList<DocumentFile>();
        int size=documentFileArrayList.length;
        list.addAll(Arrays.asList(documentFileArrayList).subList(0, size));
        return list;
    }

    // Room executes LiveData queries on separate thread automatically
    public LiveData<List<ImageHistory>> getAllImageHistory() { return allImageHistory; }

    // non-LiveData queries and edits must be done asynchronously
    public ImageHistory get (ImageHistory imageHistory) { return getTask(imageHistory); }
    public void insert (ImageHistory imageHistory) { insertTask(imageHistory); }
    public void update (ImageHistory imageHistory) { updateTask(imageHistory); }
    public boolean uriWasProcessed (ImageHistory imageHistory)  {
        checkUriWasProcessedTask(imageHistory);
        return uriWasProcessed;
    }

    private ImageHistory getTask(final ImageHistory imageHistory) {
        final ImageHistory[] result = new ImageHistory[1];
        new AsyncTask<ImageHistory, Void, ImageHistory>() {

            @Override
            protected ImageHistory doInBackground(ImageHistory... imageHistories) {
                try {
                    ImageHistory currentImageHistory = imageHistory;
                    ImageHistory fetchedImageHistory = imageHistoryDao.getImageHistory(
                            currentImageHistory.getUri(),
                            currentImageHistory.getMlModel());
                    return fetchedImageHistory;
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: ", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ImageHistory imageHistory) {
                super.onPostExecute(imageHistory);
                result[0] = imageHistory;
            }
        }.execute();
        return result[0];
    }

    private void insertTask(final ImageHistory imageHistory) {
        new AsyncTask<ImageHistory, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(ImageHistory... imageHistories) {
                try {
                    imageHistoryDao.insert(imageHistory);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: ", e);
                    return false;
                }
            }
        }.execute();
    }

    private void updateTask(final ImageHistory imageHistory) {
        new AsyncTask<ImageHistory, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(ImageHistory... imageHistories) {
                try {
                    imageHistoryDao.update(imageHistory);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: ", e);
                    return false;
                }
            }
        }.execute();
    }

    private void checkUriWasProcessedTask(final ImageHistory imageHistory) {
        new AsyncTask<ImageHistory, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(ImageHistory... imageHistories) {
                try {
                    imageHistoryDao.update(imageHistory);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: ", e);
                    return false;
                }
            }
        }.execute();
    }
}
