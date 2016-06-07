package com.demoapps.filestats;

/**
 * Created by sandip.pandey on 6/3/2016.
 */

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * FileStatActivity displays the screen's UI and starts a TaskFragment which will
 * execute an asynchronous task and will retain itself when configuration
 * changes occur.
 */
public class FileStatActivity extends AppCompatActivity implements TaskFragment.TaskCallbacks {
    private static final String TAG = FileStatActivity.class.getSimpleName();
    private static final boolean DEBUG = true; // Set this to false to disable logs.

    private static final String KEY_CURRENT_PROGRESS = "current_progress";
    private static final String KEY_PERCENT_PROGRESS = "percent_progress";
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private static final String KEY_LARGE_FILES_MAP = "large_files_map";
    private static final String KEY_FREQUENT_FILES_MAP = "frequent_files_map";
    private static final String KEY_MEDIAN_COUNT = "median_count";

    private TaskFragment mTaskFragment;
    private ProgressBar mProgressBar;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyManager;
    int id = 1;
    private NotificationReceiver nReceiver;

    private TextView mPercent;
    private TextView mProgressMsg;
    private Button mButton, mshareButton;
    private ListView filesMappvigView;
    private ListView freqFilesView;
    private TextView medianCountView, largestFilesLabel, freqLabel, medianLabel;
    private JSONArray shareStats;
    private HashMap<String, Long> mLargeFilesMap, mFrequentFilesMap;
    private long mMedianCount;

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getExtras().getString(NLService.NOT_EVENT_KEY);
            if (DEBUG) Log.i("NotificationReceiver", "NotificationReceiver onReceive : " + event);
            if (event.trim().contentEquals(NLService.NOT_REMOVED)) {
                killTasks();
            }
        }
    }

    private void killTasks() {

        if (mTaskFragment != null && mTaskFragment.isRunning()) {
            mNotifyManager.cancelAll();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_stat);

        // Initialize views.
        initViews();
        mProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        mPercent = (TextView) findViewById(R.id.percent_progress);
        mProgressMsg = (TextView) findViewById(R.id.progres_message);
        //Init notification bar progress

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(FileStatActivity.this);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(FileStatActivity.this.getString(R.string.scanning_files)).setContentText(FileStatActivity.this
                .getString(R.string.task_progress_msg)).setSmallIcon(R.drawable.abc_ic_search_api_mtrl_alpha);
        // Restore saved state.
        if (savedInstanceState != null) {
            if (DEBUG) Log.i(TAG, "######from saved Instance");
            if (mTaskFragment != null && mTaskFragment.isRunning()) {
                if (DEBUG) Log.i(TAG, "######## from saved Instance task running");
                setProgressVisibility(true);
                mProgressBar.setProgress(savedInstanceState.getInt(KEY_CURRENT_PROGRESS));
                mPercent.setText(savedInstanceState.getString(KEY_PERCENT_PROGRESS));
                // notify notification progress
                mBuilder.setProgress(100, savedInstanceState.getInt(KEY_CURRENT_PROGRESS), true);
                mNotifyManager.notify(id, mBuilder.build());
                mBuilder.setAutoCancel(true);
            } else {
                if (DEBUG) Log.i(TAG, "######## from saved Instance task is not running");
                mLargeFilesMap = (HashMap<String, Long>) savedInstanceState.getSerializable(KEY_LARGE_FILES_MAP);
                mFrequentFilesMap = (HashMap<String, Long>) savedInstanceState.getSerializable(KEY_FREQUENT_FILES_MAP);
                mMedianCount = savedInstanceState.getLong(KEY_MEDIAN_COUNT);
                if ( mLargeFilesMap != null && mFrequentFilesMap != null && mLargeFilesMap.size() > 0
                        && mFrequentFilesMap.size() > 0) {
                    setProgressVisibility(false);
                    ArrayAdapter adapter1 = new FileListAdapter(this, android.R.layout.simple_list_item_2, new ArrayList(mLargeFilesMap.entrySet()),true);
                    filesMappvigView.setAdapter(adapter1);
                    ArrayAdapter adapter2 = new FileListAdapter(this, android.R.layout.simple_list_item_2, new ArrayList(mFrequentFilesMap.entrySet()),false);
                    freqFilesView.setAdapter(adapter2);
                    medianCountView.setText(readableFileSize(mMedianCount));
                    makeResultVisible();

                }
            }
        } else {
            setProgressVisibility(false);
        }

        mButton = (Button) findViewById(R.id.task_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTaskFragment.isRunning()) {
                    mTaskFragment.cancel();
                    setProgressVisibility(false);
                    mshareButton.setEnabled(false);
                } else {
                    setProgressVisibility(true);
                    mProgressBar.setProgress(0);
                    mPercent.setText(String.valueOf(0) + "%");
                    //Notification manager progress

                    mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mBuilder = new NotificationCompat.Builder(FileStatActivity.this);
                    mBuilder.setContentTitle(FileStatActivity.this.getString(R.string.scanning_files)).setContentText(FileStatActivity.this
                            .getString(R.string.task_progress_msg)).setSmallIcon(R.drawable.abc_ic_search_api_mtrl_alpha);
                    mBuilder.setProgress(0, 0, true);
                    mNotifyManager.notify(id, mBuilder.build());
                    mBuilder.setAutoCancel(true);
                    mshareButton.setEnabled(false);
                    mTaskFragment.start();
                }
            }
        });

        mshareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Scan stats data");
                String shareMessage = shareStats.toString();
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

                startActivity(Intent.createChooser(shareIntent, "Sharing via"));

            }
        });


        FragmentManager fm = getSupportFragmentManager();
        mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is being retained
        // over a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new TaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }

        if (mTaskFragment.isRunning()) {
            mButton.setText(getString(R.string.cancel));
            setProgressVisibility(true);
        } else {
            mButton.setText(getString(R.string.start));
        }

        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();

        // check to see if the enabledNotificationListeners String contains our
        // package name
        if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
            // in this situation we know that the user has not granted the app
            // the Notification access permission
            // Check if notification is enabled for this application
            if (DEBUG) Log.i("ACC", "Dont Have Notification access");
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        } else {
            if (DEBUG) Log.i("ACC", "Have Notification access");
        }

        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NLService.NOT_TAG);
        registerReceiver(nReceiver, filter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.i(TAG, "onSaveInstanceState(Bundle)");
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PROGRESS, mProgressBar.getProgress());
        outState.putString(KEY_PERCENT_PROGRESS, mPercent.getText().toString());
        outState.putSerializable(KEY_LARGE_FILES_MAP, mLargeFilesMap);
        outState.putSerializable(KEY_FREQUENT_FILES_MAP, mFrequentFilesMap);
        outState.putLong(KEY_MEDIAN_COUNT, mMedianCount);
    }


    @Override
    public void onPreExecute() {
        if (DEBUG) Log.i(TAG, "onPreExecute()");
        mButton.setText(getString(R.string.cancel));
        makeResultInvisible();
        Toast.makeText(this, R.string.task_started_msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgressUpdate(int percent) {
        if (DEBUG) Log.i(TAG, "onProgressUpdate(" + percent + "%)");
        mProgressBar.setProgress(percent * mProgressBar.getMax() / 100);
        mPercent.setText(percent + "%");
        // notify Notification bar progress

        mBuilder.setContentText("Scanned (" + percent + "/100");
        mBuilder.setProgress(100, percent, false);
        // Displays the progress bar for the first time.
        mNotifyManager.notify(id, mBuilder.build());
    }

    @Override
    public void onCancelled() {
        if (DEBUG) Log.i(TAG, "onCancelled()");
        mButton.setText(getString(R.string.start));
        makeResultInvisible();
        setProgressVisibility(false);
        // notify Notification bar progress
        mBuilder.setContentTitle(FileStatActivity.this.getString(R.string.cancel));
        mBuilder.setContentText(FileStatActivity.this.getString(R.string.scan_cancelled))
                // Removes the progress bar
                .setProgress(0, 0, false);
        mNotifyManager.notify(id, mBuilder.build());
        Toast.makeText(this, R.string.task_cancelled_msg, Toast.LENGTH_SHORT).show();
        mshareButton.setEnabled(false);
    }

    @Override
    public void onPostExecute(Map<String, Long> files, Map<String, Long> freqfiles, long median) {
        if (DEBUG) Log.i(TAG, "onPostExecute()");
        mLargeFilesMap = (HashMap) files;
        mFrequentFilesMap = (HashMap) freqfiles;
        mMedianCount = median;

        ArrayAdapter adapter1 = new FileListAdapter(this, android.R.layout.simple_list_item_2, new ArrayList(files.entrySet()),true);
        filesMappvigView.setAdapter(adapter1);
        ArrayAdapter adapter2 = new FileListAdapter(this, android.R.layout.simple_list_item_2, new ArrayList(freqfiles.entrySet()),false);
        freqFilesView.setAdapter(adapter2);
        medianCountView.setText(readableFileSize(median));
        makeResultVisible();
        mProgressBar.setProgress(mProgressBar.getMax());
        mPercent.setText(getString(R.string.one_hundred_percent));
        setProgressVisibility(false);

        //Notification Bar progress

        mBuilder.setContentTitle(FileStatActivity.this.getString(R.string.done));
        mBuilder.setContentText(FileStatActivity.this.getString(R.string.scan_complete))
                // Removes the progress bar
                .setProgress(0, 0, false);
        mNotifyManager.notify(id, mBuilder.build());

        mButton.setText(getString(R.string.start));
        shareStats = new JSONArray();
        try {
            JSONObject jsonMedian = new JSONObject();
            jsonMedian.put("median", median);
            shareStats.put(0, getJSONObject(files));
            shareStats.put(1, getJSONObject(freqfiles));
            shareStats.put(2, jsonMedian);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mshareButton.setEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }


    @Override
    protected void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
        killTasks();
        unregisterReceiver(nReceiver);
    }

    public static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void initViews() {
        filesMappvigView = (ListView) findViewById(R.id.listView);
        filesMappvigView.setVisibility(View.INVISIBLE);
        freqFilesView = (ListView) findViewById(R.id.listView2);
        freqFilesView.setVisibility(View.INVISIBLE);
        medianCountView = (TextView) findViewById(R.id.textView4);
        medianCountView.setVisibility(View.INVISIBLE);
        largestFilesLabel = (TextView) findViewById(R.id.textView);
        largestFilesLabel.setVisibility(View.INVISIBLE);
        freqLabel = (TextView) findViewById(R.id.textView2);
        freqLabel.setVisibility(View.INVISIBLE);
        medianLabel = (TextView) findViewById(R.id.textView3);
        medianLabel.setVisibility(View.INVISIBLE);
        mshareButton = (Button) findViewById(R.id.share_button);
        mshareButton.setEnabled(false);


    }

    private void makeResultInvisible() {
        filesMappvigView.setVisibility(View.INVISIBLE);
        filesMappvigView.invalidate();
        freqFilesView.setVisibility(View.INVISIBLE);
        freqFilesView.invalidate();
        medianCountView.setVisibility(View.INVISIBLE);
        medianCountView.invalidate();
        largestFilesLabel.setVisibility(View.INVISIBLE);
        freqLabel.setVisibility(View.INVISIBLE);
        medianLabel.setVisibility(View.INVISIBLE);
    }

    private void makeResultVisible() {
       // setProgressVisibility(false);
        filesMappvigView.setVisibility(View.VISIBLE);
        filesMappvigView.invalidate();
        freqFilesView.setVisibility(View.VISIBLE);
        freqFilesView.invalidate();
        medianCountView.setVisibility(View.VISIBLE);
        medianCountView.invalidate();
        largestFilesLabel.setVisibility(View.VISIBLE);
        freqLabel.setVisibility(View.VISIBLE);
        medianLabel.setVisibility(View.VISIBLE);
        mshareButton.setEnabled(true);
    }

    private void setProgressVisibility(boolean visibility) {
        if (visibility) {
            makeResultInvisible();
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.invalidate();
            mPercent.setVisibility(View.VISIBLE);
            mPercent.invalidate();
            mProgressMsg.setVisibility(View.VISIBLE);
            mProgressMsg.invalidate();
        } else {
            mProgressBar.setVisibility(View.GONE);
            mPercent.setVisibility(View.GONE);
            mProgressMsg.setVisibility(View.GONE);
        }

    }

    private JSONObject getJSONObject(Map<String, Long> map) {
        JSONObject json = new JSONObject(map);
        return json;
    }
}