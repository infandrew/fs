package com.pillows.phonesafe;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);

        String inputPath = "src/test/resources/Book.mp4";
        String testPath = "src/test/resources/Book.test.mp4";

        File inputFile = new File(inputPath);
        File testFile = new File(testPath);

        long t1 = System.currentTimeMillis();
        copyFile(inputFile, testFile);
        System.out.println("" + (System.currentTimeMillis() - t1));

        Assert.assertTrue(testFile.delete());
    }


    public static void copyFile(File in, File out)
            throws IOException
    {
        FileChannel inChannel = new
                FileInputStream(in).getChannel();
        FileChannel outChannel = new
                FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(),
                    outChannel);
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}