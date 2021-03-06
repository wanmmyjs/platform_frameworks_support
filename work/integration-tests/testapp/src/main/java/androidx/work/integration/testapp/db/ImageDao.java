/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.work.integration.testapp.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link Image}.
 */
@Dao
public interface ImageDao {

    /**
     * Inserts a {@link Image} into the database.
     *
     * @param image The {@link Image} to insert
     */
    @Insert
    void insert(Image image);

    /**
     * Gets all {@link Image}s in the database.
     *
     * @return A list of all {@link Image}s in the database
     */
    @Query("SELECT * FROM image")
    List<Image> getImages();

    /**
     * Marks an {@link Image} as processed and adds the processed image file path
     *
     * @return number of images processed (must be 0 or 1)
     */
    @Query("UPDATE image SET mProcessedFilePath=:processedFilePath, mIsProcessed=1 "
            + "WHERE mOriginalAssetName=:originalAssetName")
    int setProcessed(String originalAssetName, String processedFilePath);

    /**
     * Gets all {@link Image}s in the database as a {@link LiveData}.
     *
     * @return A {@link LiveData} list of all {@link Image}s in the database
     */
    @Query("SELECT * FROM image")
    LiveData<List<Image>> getImagesLiveData();

    /**
     * Clears the database.
     */
    @Query("DELETE FROM image")
    void clear();
}
