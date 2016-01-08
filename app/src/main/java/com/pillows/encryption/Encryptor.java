package com.pillows.encryption;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by agudz on 31/12/15.
 */
public class Encryptor {

    private static final String DEFAULT_PROVIDER = "Crypto";

    private Cipher encCipher = null;
    private Cipher decCipher = null;

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key) {
        this(key, DEFAULT_PROVIDER);
    }

    /**
     * Constructor
     *
     * @param key
     */
    public Encryptor(String key, String provider) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(generate128ByteKey(key, provider), "AES");
            encCipher = Cipher.getInstance("AES");
            encCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            decCipher = Cipher.getInstance("AES");
            decCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("PhoneSafe", String.format("Failed to init Encryptor"));
        }
    }

    /**
     * Encrypt list of files
     *
     * @param files paths to files
     */
    public void encrypt(Iterable<String> files) {
        for (String file : files) {
            encrypt(file);
        }
    }

    public void encrypt(String file) {
        encrypt(file, file + ".enc");
    }

    /**
     * Encrypt one file
     *
     * @param oriUri file uri
     */
    /*public void encrypt(ContentResolver contentResolver, Uri oriUri) {

        try (InputStream fis = contentResolver.openInputStream(oriUri);
             FileOutputStream fos = new FileOutputStream(encFile);
             CipherOutputStream cos = new CipherOutputStream(fos, encCipher);) {

            int b;
            byte[] d = new byte[8];
            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on encryption of %s", oriUri.getPath()));
        }
    }*/

    /**
     * Encrypt one file
     *
     * @param oriFile path to file
     * @param encFile path to encrypted file
     */
    public void encrypt(String oriFile, String encFile) {

        try (FileInputStream fis = new FileInputStream(oriFile);
             FileOutputStream fos = new FileOutputStream(encFile);
             CipherOutputStream cos = new CipherOutputStream(fos, encCipher);) {

            int b;
            byte[] d = new byte[8];
            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on encryption of %s", oriFile));
        }
    }

    /**
     * Decrypt one file
     *
     * @param oriFile path to file
     * @param decFile path to encrypted file
     */
    public void decrypt(String oriFile, String decFile) {

        try (FileInputStream fis = new FileInputStream(oriFile);
             FileOutputStream fos = new FileOutputStream(decFile);
             CipherOutputStream cos = new CipherOutputStream(fos, decCipher);) {

            int b;
            byte[] d = new byte[8];
            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on decryption of %s", oriFile));
        }
    }

    public static byte[] generate128ByteKey(String baseString) throws Exception {
        return generate128ByteKey(baseString, DEFAULT_PROVIDER);
    }

    /**
     * Generates 128-byte key from string
     *
     * @param baseString base string to build key
     * @return 128-byte array that represent key
     * @throws Exception
     */
    public static byte[] generate128ByteKey(String baseString, String provider) throws Exception {
        // init random
        byte[] keyStart = baseString.getBytes("UTF-8");
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", provider);
        rand.setSeed(keyStart);

        // init key
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128, rand);
        SecretKey skey = kgen.generateKey();

        return skey.getEncoded();
    }

    public static void secureDelete(File file) throws IOException {
        if (file.exists()) {
            long length = file.length();
            SecureRandom random = new SecureRandom();
            RandomAccessFile raf = new RandomAccessFile(file, "rws");
            raf.seek(0);
            raf.getFilePointer();
            byte[] data = new byte[64];
            int pos = 0;
            while (pos < length) {
                random.nextBytes(data);
                raf.write(data);
                pos += data.length;
            }
            raf.close();
            file.delete();
        }
    }
}
