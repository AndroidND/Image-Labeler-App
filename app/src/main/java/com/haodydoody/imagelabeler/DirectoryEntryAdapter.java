package com.haodydoody.imagelabeler;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionCloudDocumentRecognizerOptions;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions;
import com.haodydoody.imagelabeler.data.CloudWorker;
import com.haodydoody.imagelabeler.data.ImageHistory;
import com.haodydoody.imagelabeler.data.ImageViewHolder;
import com.haodydoody.imagelabeler.data.ImageViewModel;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static android.content.Context.MODE_PRIVATE;
import static androidx.constraintlayout.widget.Constraints.TAG;
import static androidx.work.NetworkType.CONNECTED;

public class DirectoryEntryAdapter extends RecyclerView.Adapter<ImageViewHolder> {
    public static String PREFERENCES_ID = "labeler ID";
    public static String PREFERENCES_WIDGET_TITLE = "labeler title";
    public static String PREFERENCES_WIDGET_CONTENT = "labeler content";

    private static DocumentFile[] documentFiles;
    private Uri directoryUri;
    private Uri documentUri;
    private String newName;
    private Context context;
    private ImageViewModel imageViewModel;
    private SharedPreferences sharedPreferences;
    private List<ImageHistory> allImageHistory;

    List<String> imageTypes = Arrays.asList("image/jpeg", "image/bmp", "image/gif", "image/jpg", "image/png","image/webp");


    public void setAllImageHistory(List<ImageHistory> allImageHistory) {
        this.allImageHistory = allImageHistory;
        notifyDataSetChanged();
    }

    public DirectoryEntryAdapter(Context context, Uri directoryUri) {

        this.directoryUri = directoryUri;
        this.context = context;

        Log.d(TAG, "DirectoryEntryAdapter: " + directoryUri.toString());
        DocumentFile file = DocumentFile.fromTreeUri(context, directoryUri);
        assert file != null;
        documentFiles = file.listFiles();

    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.directory_item, parent, false);
        sharedPreferences = context.getSharedPreferences (BuildConfig.APPLICATION_ID, MODE_PRIVATE);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        DocumentFile file = documentFiles[position];

        // skip if not image type
        String fileType = file.getType();
        if (!imageTypes.contains(fileType)) return;

        holder.mTextViewImageName.setText(file.getName());
        holder.mTextViewImageType.setText(file.getType());
        Glide.with(context).load(file.getUri()).placeholder(R.drawable.image_placeholder).into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return documentFiles.length;
    }

    public void renameImages(ImageViewModel imageViewModel, int processingMethod) throws IOException {
        this.imageViewModel = imageViewModel;
        switch (processingMethod) {
            case R.id.action_rename_local:
                processLocally();
                break;
            case R.id.action_rename_cloud:
                processCloudly();
                break;
            default:

        }
    }
    // local processing will only use image labeler model
    private void processLocally() throws IOException {
        //  initialize ML app
        FirebaseApp.initializeApp(context);
        // set the minimum confidence required
        FirebaseVisionOnDeviceImageLabelerOptions localOptions =
                new FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.85f)
                        .build();
        // get on-device image labeler instance
        FirebaseVisionImageLabeler localLabeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler(localOptions);


        for (final DocumentFile document: documentFiles) {
            Log.d(TAG, "renameImages: " + document.getType());

            // skip if not image type
            String documentType = document.getType();
            if (!imageTypes.contains(documentType)) continue;

            // skip if exists in database as processed before
            String uri = document.getUri().toString();
            String name = document.getName();
            final ImageHistory imageHistory = new ImageHistory(uri,"", name, "", "", R.id.action_rename_local);
            if (imageViewModel.getImageHistory(imageHistory) != null) continue;

            // split documentType string to get file extension
            final String fileExt = documentType.split("/")[1];
            Log.d(TAG, "renameImages: file extension " + fileExt);

            //  get uri and convert to bitmap
            documentUri = document.getUri();
            Bitmap bitmap = getBitmapFromUri(documentUri);

            // convert bitmap to FirebaseVisionImage for image labelling process
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            localLabeler.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            // Task completed successfully, show new name and rename file
                            newName = "";
                            for (FirebaseVisionImageLabel label: labels)  {
                                if (newName == "") {
                                    newName = label.getText().trim();
                                } else {
                                    newName += "_" + label.getText().trim();
                                }
                            }
                            // replace all space with underscore
                            newName = newName.replaceAll(" ", "_").toLowerCase();
                            // get current file name and remove file extension before comparison, if any
                            String originalName = document.getName().replace("."+ fileExt, "");
                            Boolean nameChanged = originalName.compareTo(newName) != 0;
                            Log.d(TAG, "onSuccess: compare newName and currentName "   + newName + " - " + originalName + " changed boolean " + nameChanged);
                            if (nameChanged) {
                                // rename if different, add file extension to final name
                                document.renameTo(newName + "." + fileExt);
                                notifyDataSetChanged();
                                // add to database
                                // uri and mlModel added during initialization, no need to set here
                                imageHistory.setPreviousName(originalName);
                                imageHistory.setCurrentName(newName);
                                imageHistory.setFileType(fileExt);
                                imageHistory.setLastModified(SimpleDateFormat.getDateTimeInstance().format(new Date()));
                                imageViewModel.addImageHistory(imageHistory);

                                // update widget
                                updateAppWidget();
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed - log silently
                            Log.d(TAG, "onFailure: " + e.getMessage());
                        }
                    });
        }
    }

    // cloud processing will combine image labeler model with text recognizer to create image name
    private void processCloudly() throws IOException {

        // initialize  ML app
        FirebaseApp.initializeApp(context);

        // set the minimum confidence required
        FirebaseVisionCloudImageLabelerOptions cloudOptions =
                new FirebaseVisionCloudImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.9f)
                        .build();

        // get cloud image labeler instance
        FirebaseVisionImageLabeler cloudLabeler = FirebaseVision.getInstance()
                .getCloudImageLabeler(cloudOptions);

        // Provide language hints to assist with language detection:
        // See https://cloud.google.com/vision/docs/languages for supported languages
        FirebaseVisionCloudDocumentRecognizerOptions cloudTextRecognizerOptions = new FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en"))
                .build();

        // get on-device text recognizer
        FirebaseVisionDocumentTextRecognizer cloudTextRecognizer = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer(cloudTextRecognizerOptions);

        //Create a FirebaseVisionImage object//
        for (final DocumentFile document: documentFiles) {

            // skip if not image type
            String documentType = document.getType();
            if (!imageTypes.contains(documentType)) continue;

            // skip if exists in database as processed before
            String uri = document.getUri().toString();
            String name = document.getName();
            final ImageHistory imageHistory = new ImageHistory(uri,"", name, "", "", R.id.action_rename_cloud);
            if (imageViewModel.getImageHistory(imageHistory) != null) continue;

            // split documentType string to get file extension
            final String fileExt = documentType.split("/")[1];
            Log.d(TAG, "renameImages: file extension " + fileExt);

            //  get uri and convert to bitmap
            documentUri = document.getUri();
            Bitmap bitmap = getBitmapFromUri(documentUri);

            // convert bitmap to FirebaseVisionImage for image labelling process
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

            // use cloud image labeler to change image name
            cloudLabeler.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            final Set<String> uniqueNames = new HashSet<String>();

                            // Task completed successfully, add label to Set, ensuring uniqueness
                            for (FirebaseVisionImageLabel label: labels)  {
                                uniqueNames.addAll(Arrays.asList(label.getText().toLowerCase().split(" ")));
                                //uniqueNames.add(label.getText());
                            }
                            for (String name: uniqueNames) {
                                Log.d(TAG, "onSuccess: image labler " + name);
                            }

                            // Use Set to build new name
                            newName = "";
                            for (String name: uniqueNames) {
                                if (newName == "") {
                                    newName = name.trim();
                                } else {
                                    newName += "_" + name.trim();
                                }
                            }

                            // replace space, if any, with underscore and make lowercase
                            newName = newName.trim().replaceAll(" ", "_").toLowerCase();
                            // get original name without extension - assumes that periods only used once, before file type
                            String originalName = document.getName().split("\\.")[0];

                            Boolean nameChanged = originalName.compareTo(newName) != 0;
                            Log.d(TAG, "onSuccess: compare newName and originalName "   + newName + " - " + originalName + " changed boolean " + nameChanged);
                            if (nameChanged) {
                                // rename if different, add file extension to final name
                                document.renameTo(newName + "." + fileExt);
                                notifyDataSetChanged();
//                                // add to database
//                                // uri and mlModel added during initialization, no need to set here
//                                imageHistory.setPreviousName(originalName);
//                                imageHistory.setCurrentName(newName);
//                                imageHistory.setFileType(fileExt);
//                                imageHistory.setLastModified(SimpleDateFormat.getDateTimeInstance().format(new Date()));
//                                imageViewModel.addImageHistory(imageHistory);
                                // update widget
                                updateAppWidget();
                            }

                            // because this is an async process, must rename after each process
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed - log silently
                            Log.d(TAG, "onFailure: cloud image labeler " + e.getMessage());
                        }
                    });
            // use cloud text recognizer to add recognized text to image name
            cloudTextRecognizer.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                        @Override
                        public void onSuccess(FirebaseVisionDocumentText result) {
                            final Set<String> uniqueNames = new HashSet<String>();

                            // Add words to Set
                            uniqueNames.addAll(Arrays.asList(result.getText().toLowerCase().split(" ")));
                            // Add previous name's words to set too
                            uniqueNames.addAll(Arrays.asList(document.getName().split("\\.")[0].split("_")));

                            for (String name: uniqueNames) {
                                Log.d(TAG, "onSuccess: text recognizer " + name);
                            }
                            // Use Set to build new name
                            newName = "";
                            for (String name: uniqueNames) {
                                if (newName == "") {
                                    newName = name.trim();
                                } else {
                                    newName += "_" + name.trim();
                                }
                            }

                            // replace space, if any, with underscore and make lowercase, and remove non-words
                            newName = newName.trim().replaceAll(" ", "_").toLowerCase()
                                    .replaceAll("[^\\p{L}\\p{Nd}|^_]+", "");
                            // get original name without extension - assumes that periods only used once, before file type
                            String originalName = document.getName().split("\\.")[0];

                            Boolean nameChanged = originalName.compareTo(newName) != 0;
                            Log.d(TAG, "onSuccess: compare newName and originalName "   + newName + " - " + originalName + " changed boolean " + nameChanged);
                            if (nameChanged) {
                                // rename if different, add file extension to final name
                                document.renameTo(newName + "." + fileExt);
                                notifyDataSetChanged();
                                // add to database
                                // uri and mlModel added during initialization, no need to set here
                                imageHistory.setPreviousName(originalName);
                                imageHistory.setCurrentName(newName);
                                imageHistory.setFileType(fileExt);
                                imageHistory.setLastModified(SimpleDateFormat.getDateTimeInstance().format(new Date()));
                                imageViewModel.addImageHistory(imageHistory);
                                // update widget
                                updateAppWidget();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            Log.d(TAG, "onFailure: image text recognizer " + e.getMessage());
                        }
                    });
        }
    }
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        assert parcelFileDescriptor != null;
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private void updateAppWidget() {

        // Clear history
            sharedPreferences.edit()
                    .remove(PREFERENCES_WIDGET_CONTENT)
                    .apply();

        // Add history
            String content = "";
            for (ImageHistory imageHistory: allImageHistory) {
                content += imageHistory.getPreviousName()
                        + "."
                        + imageHistory.getFileType()
                        + " -> "
                        + imageHistory.getCurrentName()
                        + "."
                        + imageHistory.getFileType()
                        + "\n\n";
            }

            sharedPreferences
                    .edit()
                    .putString(PREFERENCES_WIDGET_TITLE, context.getResources().getString(R.string.appwidget_title))
                    .putString(PREFERENCES_WIDGET_CONTENT, content)
                    .apply();



        // Put changes on the Widget
        ComponentName provider = new ComponentName(context, ImageHistoryWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(provider);
        ImageHistoryWidget imageHistoryWidget = new ImageHistoryWidget();
        imageHistoryWidget.onUpdate(context, appWidgetManager, ids);
    }

}
