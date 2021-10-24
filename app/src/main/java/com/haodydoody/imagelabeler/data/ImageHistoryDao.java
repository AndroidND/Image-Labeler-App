package com.haodydoody.imagelabeler.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.haodydoody.imagelabeler.R;

import java.util.List;

@Dao
public interface ImageHistoryDao {
    @Query("SELECT * FROM image_history")
    public LiveData<List<ImageHistory>> getAllImageHistory();

    @Query("SELECT * FROM image_history WHERE uri=:uri AND mlModel=:mlModel")
    public ImageHistory getImageHistory(String uri, int mlModel);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public void insert(ImageHistory imageHistory);

    @Update
    public int update(ImageHistory imageHistory);

    @Query("SELECT COUNT(id) FROM image_history")
    public int getCount();
}
