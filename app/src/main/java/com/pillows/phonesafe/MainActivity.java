package com.pillows.phonesafe;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.pillows.accessory.AccessoryCallback;
import com.pillows.accessory.AccessoryService;
import com.pillows.encryption.Encryptor;
import com.pillows.saver.DataSaver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.pillows.phonesafe.Settings.*;
import static com.pillows.phonesafe.ActionMode.*;

public class MainActivity extends AppCompatActivity implements AccessoryCallback {

    private static final int FILE_SELECT_CODE1 = 0;

    private StableArrayAdapter adapter;
    private RelativeLayout progressBarLayout;
    private ProgressBar progressBar;
    private boolean mIsBound = false;
    private AccessoryService mConsumerService = null;
    private String currentAction = ACTION_NOTHING;

    /**
     * Object to establish connection between MainActivity and AccessoryService
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((AccessoryService.LocalBinder) service).getService();
            mConsumerService.setCallbacks(MainActivity.this);

            Log.d(Settings.TAG, "ServiceConnected");
            mConsumerService.findPeers();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            Log.d(Settings.TAG, "ServiceDisconnected");
        }
    };

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE1);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilePicker() {
        // This always works
        Intent i = new Intent(getApplicationContext(), FilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE_AND_DIR);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        String startPath = Environment.getExternalStorageDirectory().getPath() + "/test/";
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, startPath);

        startActivityForResult(i, FILE_SELECT_CODE1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Set<String> pickedFiles = new HashSet<String>();
        switch (requestCode) {
            case FILE_SELECT_CODE1:
                if (resultCode == RESULT_OK) {
                    if (data != null) {

                        // for multiple files
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                ClipData.Item item = clipData.getItemAt(i);
                                Uri uri = item.getUri();

                                String path = uri.getPath();
                                pickedFiles.add(path);
                                Log.d(Settings.TAG, path);
                            }
                        }

                        // for one file
                        Uri uri = data.getData();
                        if (uri != null) {
                            String path = uri.getPath();
                            pickedFiles.add(path);
                            Log.d(Settings.TAG, path);
                        }
                    }
                }
                break;
        }
        adapter.addAll(pickedFiles);
        DataSaver.serialize(adapter.getItems(), getCacheDir());
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIsBound = bindService(new Intent(MainActivity.this, AccessoryService.class), mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "mIsBound: " + mIsBound);

        progressBarLayout = (RelativeLayout) findViewById(R.id.progressbar_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

            final List<FileDetails> items = (List) DataSaver.deserialize(getCacheDir());
        for (FileDetails item: items)
            item.setEncrypted(Encryptor.checkWatermark(item.getPath()));

        final ListView listview = (ListView) findViewById(R.id.listview);
        listview.setEmptyView(findViewById(R.id.empty));
        adapter = new StableArrayAdapter(this, R.layout.list_item, items);

        listview.setAdapter(adapter);

        bindButtonActions();
    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        if (mIsBound && mConsumerService != null) {
            mConsumerService.closeConnection();
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }

    private void bindButtonActions() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilePicker();
            }
        });

        FloatingActionButton encfab = (FloatingActionButton) findViewById(R.id.encrypt_fab);
        encfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callGear(ACTION_CLOSE);
            }
        });

        FloatingActionButton decfab = (FloatingActionButton) findViewById(R.id.decrypt_fab);
        decfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callGear(ACTION_OPEN);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_reconnect) {
            mConsumerService.findPeers();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class StableArrayAdapter extends ArrayAdapter<FileDetails> {

        private List<FileDetails> items;
        private Map<String, FileDetails> itemsMap;
        private Context context;

        public StableArrayAdapter(Context context, int viewId,
                                  List<FileDetails> items) {
            super(context, viewId, items);
            this.items = items;
            this.itemsMap = new HashMap<String, FileDetails>();
            for(FileDetails d: items)
                itemsMap.put(d.getPath(), d);
            this.context = context;
        }

        public List<FileDetails> getItems() {
            return items;
        }

        public List<String> getItemsPaths() {
            List<String> paths = new ArrayList<String>();
            for (FileDetails item: items) {
                if (!item.isEncrypted())
                    paths.add(item.getPath());
            }
            return paths;
        }

        public void addAll(Collection<? extends FileDetails> newItems) {
            for (FileDetails possibleItem: newItems) {
                add(possibleItem);
            }
        }

        public void add(FileDetails possibleItem) {
            String filePath = possibleItem.getPath();
            if (!itemsMap.containsKey(filePath)) {
                itemsMap.put(filePath, possibleItem);
                super.add(possibleItem);
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_item, null);
            }

            //Handle TextView and display string from your list
            TextView listItemText = (TextView)view.findViewById(R.id.list_item_text);
            listItemText.setText(items.get(position).getPath());

            //Handle buttons and add onClickListeners
            ImageButton deleteBtn = (ImageButton)view.findViewById(R.id.list_item_delete);
            ImageView encImage = (ImageView)view.findViewById(R.id.list_item_enc);

            FileDetails fileDetails = items.get(position);
            if(fileDetails.isEncrypted())
                encImage.setVisibility(View.VISIBLE);
            else
                encImage.setVisibility(View.INVISIBLE);

            deleteBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String filePath = items.get(position).getPath();
                    //do something
                    items.remove(position); //or some other task
                    itemsMap.remove(filePath);
                    notifyDataSetChanged();
                    DataSaver.serialize(adapter.getItems(), getCacheDir());
                }
            });

            return view;
        }

        public void addAll(Set<String> pickedFiles) {
            for (String possibleItem: pickedFiles) {
                add(new FileDetails(possibleItem));
            }
        }
    }

    class EncProgressTask extends AsyncTask<Void, Integer, Void>
    {
        ProgressDialog dialog;
        String key;

        public EncProgressTask(String key) {
            this.key = key;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            if (isCancelled()) return null;

            publishProgress(0);

            List<FileDetails> files = adapter.getItems();

            Encryptor enc = new Encryptor(key);

            int i = 0, filesCount = files.size();

            for (FileDetails file : files) {
                if(!file.isEncrypted() && enc.encrypt(file.getPath())) {
                    file.setEncrypted(true);
                }
                publishProgress(++i);

                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            List<FileDetails> files = adapter.getItems();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Please wait file encryption");
            dialog.setIndeterminate(false);
            dialog.setMax(files.size());
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            dialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }
    }

    class DecProgressTask extends AsyncTask<Void, Integer, Void>
    {
        ProgressDialog dialog;
        String key;

        public DecProgressTask(String key) {
            this.key = key;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isCancelled()) return null;

            publishProgress(0);

            List<FileDetails> files = adapter.getItems();

            Encryptor enc = new Encryptor(key);

            int i = 0, filesCount = files.size();

            for (FileDetails file : files) {
                if (file.isEncrypted() && enc.decrypt(file.getPath())) {
                    file.setEncrypted(false);
                }
                i++;
                publishProgress(++i);

                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            List<FileDetails> files = adapter.getItems();
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Please wait file decryption");
            dialog.setIndeterminate(false);
            dialog.setMax(files.size());
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            dialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        }
    }


    private void callGear(String gearAction) {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Waiting for Gear...");
        dialog.setMessage("You have 60 sec to input safe code");
        //dialog.setIndeterminate(false);
        //dialog.setCancelable(false);
        //dialog.setMax(60);
        //dialog.setOnCancelListener(cancelListener);
        dialog.show();

        currentAction = gearAction;
        mConsumerService.sendData(gearAction);

        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (currentAction.equals(ACTION_NOTHING))
                    dialog.dismiss();
            }
            public void onFinish() {
                currentAction = ACTION_NOTHING;
                dialog.dismiss();
            }
        }.start();
    }


    @Override
    public void gearResponse(String data) {
        switch(currentAction) {
            case ACTION_CLOSE:
                new EncProgressTask(data).execute();
                break;
            case ACTION_OPEN:
                new DecProgressTask(data).execute();
                break;
            default:
        }
        currentAction = ACTION_NOTHING;
    }

}
