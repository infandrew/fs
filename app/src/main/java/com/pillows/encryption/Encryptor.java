package com.pillows.encryption;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.pillows.phonesafe.Settings;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.Arrays;
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

    /**
     * Encrypt list of files
     *
     * @param files paths to files
     */
    public void decrypt(Iterable<String> files) {
        for (String file : files) {
            decrypt(file);
        }
    }

    public void encrypt(String file) {
        encrypt(file, file);
    }

    public void decrypt(String file) {
        decrypt(file, file);
    }

    /**
     * Encrypt one file
     *
     * @param oriPath path to file
     * @param encPath path to encrypted file
     */
    public void encrypt(String oriPath, String encPath) {

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File encFile = new File(encPath);

        if (oriPath.equals(encPath))
        {
            pathNotChanged = true;
            encPath = encPath + ".temp";
            encFile = new File(encPath);
            try {
                FileUtils.copyFile(oriFile, encFile);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PhoneSafe", String.format("Failed on copying of %s -> %s", oriFile, encFile));
            }
        }

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(encPath);
             CipherOutputStream cos = new CipherOutputStream(fos, encCipher);) {

            // some kind of watermark
            int b = 8;
            byte[] d = Settings.WATERMARK.clone();

            fos.write(d, 0, b);

            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on encryption of %s", oriPath));
        }

        try {
            secureDelete(oriFile);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on deleting of %s", oriPath));
        }

        if (pathNotChanged)
        {
            try {
                FileUtils.moveFile(encFile, oriFile);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PhoneSafe", String.format("Failed on renaming of %s -> %s", encFile, oriFile));
            }
        }
    }

    /**
     * Decrypt one file
     *
     * @param oriPath path to file
     * @param decPath path to encrypted file
     */
    public void decrypt(String oriPath, String decPath) {

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File decFile = new File(decPath);

        if (oriPath.equals(decPath))
        {
            pathNotChanged = true;
            decPath = decPath + ".temp";
            decFile = new File(decPath);
            try {
                FileUtils.copyFile(oriFile, decFile);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PhoneSafe", String.format("Failed on copying of %s -> %s", oriFile, decFile));
            }
        }

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(decPath);
             CipherOutputStream cos = new CipherOutputStream(fos, decCipher);) {

            // some kind of watermark
            int b = 8;
            byte[] d = new byte[8];
            b = fis.read(d);

            if(!Arrays.equals(d, Settings.WATERMARK))
                throw new Exception("Can't find watermark");

            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhoneSafe", String.format("Failed on decryption of %s. %s", oriPath, e.getMessage()));
        }

        oriFile.delete();

        if (pathNotChanged)
        {
            try {
                FileUtils.moveFile(decFile, oriFile);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PhoneSafe", String.format("Failed on renaming of %s -> %s", decFile, oriFile));
            }
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
