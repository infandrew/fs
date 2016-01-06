package com.pillows.encryption;

import com.pillows.encryption.Encryptor;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsNot;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by agudz on 31/12/15.
 */
public class EncryptorTest {

    /**
     * Checks that generated bytes are equals for same base strings.
     */
    @Test
    public void testKeyGeneration() throws Exception {
        byte[] b1 = Encryptor.generate128ByteKey("304029450398", "SUN");
        byte[] b2 = Encryptor.generate128ByteKey("304029450398", "SUN");
        byte[] b3 = Encryptor.generate128ByteKey("304029450398", "SUN");
        byte[] b4 = Encryptor.generate128ByteKey("304029450398", "SUN");

        Assert.assertTrue(Arrays.equals(b1, b2));
        Assert.assertTrue(Arrays.equals(b3, b4));
        Assert.assertTrue(Arrays.equals(b1, b4));
    }

    /**
     * Test Encryptor creates files
     */
    @Test
    public void testCreation() throws IOException {
        Encryptor enc = new Encryptor("201025153020", "SUN");

        String inputPath = "src/test/resources/1.jpg";
        String encPath = "src/test/resources/1.enc.jpg";
        String decPath = "src/test/resources/1.dec.jpg";

        File inputFile = new File(inputPath);
        File encFile = new File(encPath);
        File decFile = new File(decPath);

        Assert.assertTrue(inputFile.exists());

        enc.encrypt(inputPath, encPath);

        Assert.assertTrue(encFile.exists());

        Encryptor enc2 = new Encryptor("201025153020", "SUN");

        enc2.decrypt(encPath, decPath);

        Assert.assertTrue(decFile.exists());

        Assert.assertTrue(FileUtils.contentEquals(inputFile, decFile));

        Assert.assertTrue(encFile.delete());
        Assert.assertTrue(decFile.delete());
    }

}
