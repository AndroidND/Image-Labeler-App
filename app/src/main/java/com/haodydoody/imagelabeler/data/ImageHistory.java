package com.haodydoody.imagelabeler.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "image_history")
public class ImageHistory implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String uri;
    private String fileType;
    private String currentName;
    private String previousName;
    private String lastModified;
    private int mlModel;

    public ImageHistory(String uri, String fileType, String currentName, String previousName, String lastModified, int mlModel) {
        this.uri = uri;
        this.fileType = fileType;
        this.currentName = currentName;
        this.previousName = previousName;
        this.lastModified = lastModified;
        this.mlModel = mlModel;
    }

    protected ImageHistory(Parcel in) {
        uri = in.readString();
        fileType = in.readString();
        currentName = in.readString();
        previousName = in.readString();
        lastModified = in.readString();
        mlModel = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uri);
        dest.writeString(fileType);
        dest.writeString(currentName);
        dest.writeString(previousName);
        dest.writeString(lastModified);
        dest.writeInt(mlModel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ImageHistory> CREATOR = new Creator<ImageHistory>() {
        @Override
        public ImageHistory createFromParcel(Parcel in) {
            return new ImageHistory(in);
        }

        @Override
        public ImageHistory[] newArray(int size) {
            return new ImageHistory[size];
        }
    };

    public int getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public String getFileType() {
        return fileType;
    }

    public String getCurrentName() {
        return currentName;
    }

    public String getPreviousName() {
        return previousName;
    }

    public String getLastModified() {
        return lastModified;
    }

    public int getMlModel() {
        return mlModel;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
    }

    public void setPreviousName(String previousName) {
        this.previousName = previousName;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public void setMlModel(int mlModel) {
        this.mlModel = mlModel;
    }
}
