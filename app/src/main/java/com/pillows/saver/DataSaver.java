package com.pillows.saver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by agudz on 06/01/16.
 */
public class DataSaver {

    /**
     * Serialize large tests map to file.
     *
     * @param object large tests map
     * @param path path to serialization file
     * @return true if serialization was successful
     */
    public static boolean serialize(Object object, String path) {

        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
            out.writeObject(object);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Deserialize large tests map from file.
     *
     * @param path path to serialization file
     * @return large tests map
     */
    public static Object deserialize(String path) {
        Object holder = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(path));

            holder = (Object) objectInputStream.readObject();
            objectInputStream.close();
        } catch(IOException e) {
            e.printStackTrace();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return holder;
    }
}
