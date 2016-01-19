package com.pillows.encryption;

import android.util.Log;

import com.pillows.phonesafe.FileDetails;
import com.pillows.phonesafe.Settings;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static com.pillows.phonesafe.Settings.*;

/**
 * Created by agudz on 31/12/15.
 */
public class Encryptor {

    private static final String DEFAULT_PROVIDER = "Crypto";

    private Cipher encCipher = null;
    private Cipher decCipher = null;
    private boolean prefSecureDelete = true;

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key) {
        this(key, DEFAULT_PROVIDER, true);
    }

    /**
     * Constructor
     *
     * @param key string key
     */
    public Encryptor(String key, boolean prefSecureDelete) {
        this(key, DEFAULT_PROVIDER, prefSecureDelete);

    }

    /**
     * Constructor
     *
     * @param key
     */
    public Encryptor(String key, String provider, boolean prefSecureDelete) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(generate128ByteKey(key, provider), "AES");
            encCipher = Cipher.getInstance("AES");
            encCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            decCipher = Cipher.getInstance("AES");
            decCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, String.format("Failed to init Encryptor"));
        }

        this.prefSecureDelete = prefSecureDelete;
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

    public void encryptFileDetails(Iterable<FileDetails> files) {
        for (FileDetails file : files) {
            if(!file.isEncrypted() && encrypt(file.getPath())) {
                file.setEncrypted(true);
            }
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

    public void decryptFileDetails(Iterable<FileDetails> files) {
        for (FileDetails file : files) {
            if(file.isEncrypted() && decrypt(file.getPath())) {
                file.setEncrypted(false);
            }
        }
    }

    public boolean encrypt(String file) {
        return encrypt(file, file);
    }

    public boolean decrypt(String file) {
        return decrypt(file, file);
    }

    /**
     * Encrypt one file
     *
     * @param oriPath path to file
     * @param encPath path to encrypted file
     */
    public boolean encrypt(String oriPath, String encPath) {

        long t1 = System.currentTimeMillis();

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File encFile = new File(encPath);

        if (oriPath.equals(encPath)) {
            pathNotChanged = true;
            encPath = encPath + ".temp";
            encFile = new File(encPath);
            try {
                FileUtils.copyFile(oriFile, encFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on copying of %s -> %s", oriFile, encFile));
                return false;
            }
        }
        Log.d("PhoneSafe", oriFile + " 1 " + (System.currentTimeMillis() - t1));

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(encPath);
             CipherOutputStream cos = new CipherOutputStream(fos, encCipher);) {

            // some kind of watermark
            int b = 1024;
            byte[] d = new byte[b];

            // write watermark
            fos.write(WATERMARK, 0, WATERMARK.length);
            // write sha1 checksum 20-byte  (+4-byte empty)
            byte[] sha1 = sha1(oriFile);
            fos.write(sha1, 0, 32);

            while ((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }

        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on encryption of %s. %s", oriPath, e.getMessage()));
            encFile.delete();
            return false;
        }

        Log.d("PhoneSafe", oriFile + " 2 " + (System.currentTimeMillis() - t1));

        Log.d("PhoneSafe", "" + prefSecureDelete);
        try {
            if (prefSecureDelete)
                secureDelete(oriFile);
            else
                oriFile.delete();
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on deleting of %s", oriPath));
            return false;
        }

        Log.d("PhoneSafe", oriFile + " 3 " + (System.currentTimeMillis() - t1));

        if (pathNotChanged) {
            try {
                FileUtils.moveFile(encFile, oriFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on renaming of %s -> %s", encFile, oriFile));
                return false;
            }
        }

        Log.d("PhoneSafe", oriFile + " 4 " + (System.currentTimeMillis() - t1));

        return true;
    }

    /**
     * Decrypt one file
     *
     * @param oriPath path to file
     * @param decPath path to encrypted file
     */
    public boolean decrypt(String oriPath, String decPath) {

        long t1 = System.currentTimeMillis();

        boolean pathNotChanged = false;
        File oriFile = new File(oriPath);
        File decFile = new File(decPath);

        if (oriPath.equals(decPath)) {
            pathNotChanged = true;
            decPath = decPath + ".temp";
            decFile = new File(decPath);
            try {
                FileUtils.copyFile(oriFile, decFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on copying of %s -> %s", oriFile, decFile));
                return false;
            }
        }

        Log.d("PhoneSafe", oriFile + " 1 " + (System.currentTimeMillis() - t1));

        byte[] sha1 = new byte[32];
        byte[] expectedSha1 = new byte[32];

        try (FileInputStream fis = new FileInputStream(oriPath);
             FileOutputStream fos = new FileOutputStream(decPath);
             CipherOutputStream cos = new CipherOutputStream(fos, decCipher);) {

            byte[] watermarkCheck = new byte[WATERMARK.length];
            fis.read(watermarkCheck);

            if (!Arrays.equals(watermarkCheck, WATERMARK))
                throw new Exception("Can't find watermark");

            fis.read(sha1);

            int b = 1024;
            byte[] d = new byte[b];
            while ((b = fis.read(d)) != -1)
                cos.write(d, 0, b);

        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on decryption of %s. %s", oriPath, e.getMessage()));
            decFile.delete();
            return false;
        }

        Log.d("PhoneSafe", oriFile + " 2 " + (System.currentTimeMillis() - t1));

        try {
            expectedSha1 = sha1(decFile);
            if (!Arrays.equals(sha1, expectedSha1)) {
                throw new Exception("Checksum not matched");
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed on decryption of %s. %s", oriPath, e.getMessage()));
            return false;
        }

        Log.d("PhoneSafe", oriFile + " 3 " + (System.currentTimeMillis() - t1));

        oriFile.delete();

        if (pathNotChanged) {
            try {
                FileUtils.moveFile(decFile, oriFile);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed on renaming of %s -> %s", decFile, oriFile));
                return false;
            }
        }

        Log.d("PhoneSafe", oriFile + " 4 " + (System.currentTimeMillis() - t1));

        return true;
    }

    public byte[] encrypt(byte[] bytes)
    {
        try {
            return encCipher.doFinal(bytes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt byte array");
        }
        return new byte[256];
    }

    public byte[] decrypt(byte[] bytes)
    {
        try {
            return decCipher.doFinal(bytes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt byte array");
        }
        return new byte[256];
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
            byte[] data = new byte[4096];
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

    public static boolean checkWatermark(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);) {

                byte[] d = new byte[8];
                fis.read(d);

                if (Arrays.equals(d, Settings.WATERMARK))
                    return true;
                else
                    return false;

            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to check Watermark of %s.", filePath, e.getMessage()));
            }
        }
        return false;
    }

    public static byte[] sha1(final File file) throws Exception {
        MessageDigest messageDigest;

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            messageDigest = MessageDigest.getInstance("SHA-256");
            final byte[] buffer = new byte[1024];
            for (int read = 0; (read = is.read(buffer)) != -1;) {
                messageDigest.update(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new Exception("Failed to calculate checksum");
        }

        //byte[] result = new byte[32];
        //System.arraycopy(messageDigest.digest(),0,result,0,32);
        return messageDigest.digest();
    }
}
