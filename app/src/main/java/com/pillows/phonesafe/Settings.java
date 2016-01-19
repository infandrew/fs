package com.pillows.phonesafe;

/**
 * Created by agudz on 11/01/16.
 */
final public class Settings {

    public static final boolean DEBUG = false;

    public static final String TAG = "PhoneSafe";
    public static final String TEST_KEY = "Awertwertwergsdfgsdfgz";

    public static final String ACTION_CLOSE = "close";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_NOTHING = "nothing";

    public static final int ACTION_CHANNEL = 228;
    public static final int CALL_GEAR_DELAY = 60000;

    public static final byte[] WATERMARK = new byte[] {0x73,0x61,0x66,0x65,0x30,0x30,0x30,0x31};
}
