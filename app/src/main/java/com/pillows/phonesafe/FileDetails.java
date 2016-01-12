package com.pillows.phonesafe;

import java.io.Serializable;

/**
 * Created by agudz on 11/01/16.
 */
public class FileDetails implements Serializable {

    private boolean encrypted;
    private String path;

    /**
     * Constructor
     */
    public FileDetails(String path) {
        encrypted = false;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean isEncrypted() {
        return encrypted;
    }
}
