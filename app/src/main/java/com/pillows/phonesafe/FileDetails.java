package com.pillows.phonesafe;

import com.pillows.encryption.Encryptor;

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
        this.encrypted = Encryptor.checkWatermark(path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

}
