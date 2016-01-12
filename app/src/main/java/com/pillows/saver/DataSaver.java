package com.pillows.saver;

import android.util.Log;

import com.pillows.phonesafe.FileDetails;
import com.pillows.phonesafe.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by agudz on 06/01/16.
 */
public class DataSaver {

    /**
     * Serialize large tests map to file.
     *
     * @param items    large tests map
     * @param cacheFile path to serialization file
     * @return true if serialization was successful
     */
    public static synchronized boolean serialize(List<FileDetails> items, File cacheFile) {

        File dataFile = new File(cacheFile.getPath() + "/data");

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            out.writeObject(items);
        } catch (IOException e) {
            Log.w(Settings.TAG, "Exception on serializing. " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Deserialize large tests map from file.
     *
     * @param cacheFile path to cache file
     * @return large tests map
     */
    public static List deserialize(File cacheFile) {

        File dataFile = new File(cacheFile.getPath() + "/data");

        List<FileDetails> holder = new ArrayList<FileDetails>();
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(dataFile))) {
            holder = (ArrayList) stream.readObject();
        } catch (Exception e) {
            Log.w(Settings.TAG, "Exception on deserializing. " + e.getMessage());
        }
        return holder;
    }
}
