package com.pillows.encryption;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by agudz on 31/12/15.
 */
@RunWith(AndroidJUnit4.class)
public class EncryptorAndroidTest {

    private static final String TAG = "PhoneSafe";
    private static final String PACKAGE_ID = "com.pillows.phonesafe";
    private static final String CACHE_DIR = "/data/data/" + PACKAGE_ID + "/cache/";

    /**
     * Checks uri handling
     */
    @Test
    public void uriTest() {
        Uri uri = Uri.fromFile(new File("/sdcard/test/1.jpg"));
        Log.d(TAG, uri.getPath());

        Encryptor enc = new Encryptor("afsdgfhkasdhgfkajsdh");

    }

    /**
     * Checks that generated bytes are equals for same base strings.
     */
    @Test
    public void testKeyGeneration() {

        try {
            byte[] b1 = Encryptor.generate128ByteKey("304029450398");
            byte[] b2 = Encryptor.generate128ByteKey("304029450398");
            byte[] b3 = Encryptor.generate128ByteKey("304029450398");
            byte[] b4 = Encryptor.generate128ByteKey("304029450398");

            Assert.assertTrue(Arrays.equals(b1, b2));
            Assert.assertTrue(Arrays.equals(b3, b4));
            Assert.assertTrue(Arrays.equals(b1, b4));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Test Encryptor creates files
     */
    @Test
    public void testCreation() throws IOException {
        Encryptor enc = new Encryptor("afsdgfhkasdhgfkajsdh");

        String inputPath = CACHE_DIR + "1.jpg";
        String testPath = CACHE_DIR + "1.test.jpg";
        String encPath = CACHE_DIR + "1.enc.jpg";
        String decPath = CACHE_DIR + "1.dec.jpg";

        File inputFile = new File(inputPath);
        File testFile = new File(testPath);
        File encFile = new File(encPath);
        File decFile = new File(decPath);

        inputFile.delete();
        testFile.delete();
        encFile.delete();
        decFile.delete();

        // copy to buffer
        InputStream resourceFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("1.jpg");

        OutputStream cacheFile = new FileOutputStream(inputFile);

        copyFile(resourceFile, cacheFile);

        FileUtils.copyFile(inputFile, testFile);

        Assert.assertTrue(inputFile.exists());

        enc.encrypt(inputPath, encPath);

        Assert.assertTrue(encFile.exists());

        Encryptor enc2 = new Encryptor("afsdgfhkasdhgfkajsdh");

        enc2.decrypt(encPath, decPath);

        Assert.assertTrue(decFile.exists());

        Assert.assertTrue(FileUtils.contentEquals(testFile, decFile));

        inputFile.delete();
        encFile.delete();
        Assert.assertTrue(testFile.delete());
        Assert.assertTrue(decFile.delete());
    }


    /**
     * Test Encryptor with watermark
     */
    @Test
    public void testWatermark() throws IOException {
        String inputPath = CACHE_DIR + "1.jpg";
        String testPath = CACHE_DIR + "1.test.jpg";

        File inputFile = new File(inputPath);
        File testFile = new File(testPath);

        inputFile.delete();
        testFile.delete();

        // copy to buffer
        InputStream resourceFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("1.jpg");

        OutputStream inputStream = new FileOutputStream(inputFile);
        copyFile(resourceFile, inputStream);

        FileUtils.copyFile(inputFile, testFile);

        Assert.assertTrue(inputFile.exists());
        Assert.assertTrue(testFile.exists());

        Assert.assertTrue(FileUtils.contentEquals(inputFile, testFile));

        Encryptor enc = new Encryptor("2010251530201213");

        enc.encrypt(testPath);

        Encryptor enc2 = new Encryptor("2010251530201213");

        enc2.decrypt(testPath);

        Assert.assertTrue(testFile.exists());

        Assert.assertTrue(FileUtils.contentEquals(inputFile, testFile));

        Assert.assertTrue(inputFile.delete());
        Assert.assertTrue(testFile.delete());
    }

    /**
     * Test wrong encryption key
     */
    @Test
    public void testWrong() throws IOException {
        String inputPath = CACHE_DIR + "1.jpg";
        String testPath = CACHE_DIR + "1.test.jpg";

        File inputFile = new File(inputPath);
        File testFile = new File(testPath);

        inputFile.delete();
        testFile.delete();

        // copy to buffer
        InputStream resourceFile = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("1.jpg");

        OutputStream inputStream = new FileOutputStream(inputFile);
        copyFile(resourceFile, inputStream);

        FileUtils.copyFile(inputFile, testFile);

        Assert.assertTrue(inputFile.exists());
        Assert.assertTrue(testFile.exists());

        Assert.assertTrue(FileUtils.contentEquals(inputFile, testFile));

        Encryptor enc = new Encryptor("2010251530201213");

        enc.encrypt(testPath);

        Encryptor enc2 = new Encryptor("201025153020wrong");

        enc2.decrypt(testPath);

        Assert.assertTrue(testFile.exists());

        Assert.assertFalse(FileUtils.contentEquals(inputFile, testFile));

        Assert.assertTrue(inputFile.delete());
        Assert.assertTrue(testFile.delete());
    }


    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
