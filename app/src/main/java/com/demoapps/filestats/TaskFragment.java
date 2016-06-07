package com.demoapps.filestats;

/**
 * Created by sandip.pandey on 6/3/2016.
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TaskFragment manages a single background task and retains itself across
 * configuration changes.
 */
public class TaskFragment extends Fragment {
    private static final String TAG = TaskFragment.class.getSimpleName();
    private static final boolean DEBUG = true; // Set this to false to disable logs.
    private static String SD_CARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    private Map<String, Long> filesMapping;
    private Map<String, Long> freqfiles;
    private long medianCount;

    /**
     * Callback interface through which the fragment can report the task's
     * progress and results back to the Activity.
     */

    static interface TaskCallbacks {
        void onPreExecute();

        void onProgressUpdate(int percent);

        void onCancelled();

        void onPostExecute(Map<String, Long> files, Map<String, Long> freqfiles, long median);
    }

    private TaskCallbacks mCallbacks;
    private DummyTask mTask;
    private boolean mRunning;

    /**
     * Hold a reference to the parent Activity so we can report the task's current
     * progress and results. The Android framework will pass us a reference to the
     * newly created Activity after each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        if (DEBUG) Log.i(TAG, "onAttach(Activity)");
        super.onAttach(activity);

        if (!(activity instanceof TaskCallbacks)) {
            throw new IllegalStateException("Activity must implement the TaskCallbacks interface.");
        }

        // Hold a reference to the parent Activity so we can report back the task's
        // current progress and results.
        mCallbacks = (TaskCallbacks) activity;
    }

    /**
     * This method is called once when the Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Note that this method is <em>not</em> called when the Fragment is being
     * retained across Activity instances. It will, however, be called when its
     * parent Activity is being destroyed for good (such as when the user clicks
     * the back button, etc.).
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
        cancel();
    }

    /**
     * Start the background task.
     */
    public void start() {
        if (!mRunning) {
            mTask = new DummyTask();
            mTask.execute();
            mRunning = true;
        }
    }

    /**
     * Cancel the background task.
     */
    public void cancel() {
        if (mRunning) {
            mTask.cancel(false);
            mTask = null;
            mRunning = false;
        }
    }

    /**
     * Returns the current state of the background task.
     */
    public boolean isRunning() {
        return mRunning;
    }


    /**
     * A dummy task that performs some (dumb) background work and proxies progress
     * updates and results back to the Activity.
     */
    private class DummyTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            // Proxy the call to the Activity.
            mCallbacks.onPreExecute();
            mRunning = true;
        }

        /**
         * Note that we do NOT call the callback object's methods directly from the
         * background thread, as this could result in a race condition.
         */
        @Override
        protected Void doInBackground(Void... ignore) {

            filesMapping = new LinkedHashMap<String, Long>();
            freqfiles = new LinkedHashMap<String, Long>();
            visitRecursive(Environment.getExternalStorageDirectory());
            publishProgress(40);

            if (DEBUG) Log.d(TAG, "fillRecursiveFiles filesMapping: " + filesMapping.toString());
            if (DEBUG) Log.d(TAG, "fillRecursiveFiles freqfiles: " + freqfiles.toString());
            filesMapping = sortByComparator(filesMapping, false);
            freqfiles = sortByComparator(freqfiles, false);
            publishProgress(80);

            if (DEBUG)
                Log.d(TAG, "fillRecursiveFiles after sort filesMapping: " + filesMapping.toString());
            if (DEBUG)
                Log.d(TAG, "fillRecursiveFiles after sort freqfiles: " + freqfiles.toString());
            Object[] val = filesMapping.values().toArray();

            medianCount = (Long) val[val.length / 2];
            filesMapping = fillResultValues(filesMapping, 10);
            freqfiles = fillResultValues(freqfiles, 5);
            publishProgress(100);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... percent) {
            // Proxy the call to the Activity.
            mCallbacks.onProgressUpdate(percent[0]);
        }

        @Override
        protected void onCancelled() {
            // Proxy the call to the Activity.
            mCallbacks.onCancelled();
            mRunning = false;
        }

        @Override
        protected void onPostExecute(Void ignore) {
            // Proxy the call to the Activity.
            mCallbacks.onPostExecute(filesMapping, freqfiles, medianCount);
            mRunning = false;
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onActivityCreated(Bundle)");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }


    private void visitRecursive(File root) {

        File[] list = root.listFiles();

        for (File f : list) {
            if (f.isDirectory()) {
                if (DEBUG) Log.d("", "Dir: " + f.getAbsoluteFile());
                visitRecursive(f);
            } else {
                filesMapping.put(f.getAbsolutePath(), f.length());
                String ext = getExtension(f);
                if (ext != null) {
                    if (freqfiles.containsKey(ext)) {
                        freqfiles.put(ext, freqfiles.get(ext) + 1);
                    } else {
                        freqfiles.put(ext, 1l);
                    }
                }
                if (DEBUG) Log.d("", "File: " + f.getAbsoluteFile());
            }
        }
    }

    /*
    * Get the extension of a file.
    */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    private static Map<String, Long> sortByComparator(Map<String, Long> unsortMap, final boolean order) {

        List<Map.Entry<String, Long>> list = new LinkedList<Map.Entry<String, Long>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> o1,
                               Map.Entry<String, Long> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Long> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static Map<String, Long> fillResultValues(Map mp, int reqCount) {
        Map<String, Long> tempMap = new LinkedHashMap<String, Long>();
        Iterator it = mp.entrySet().iterator();
        int count = 1;
        while (it.hasNext()) {
            if (count > reqCount) {
                break;
            }
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            tempMap.put(pair.getKey().toString(), Long.valueOf(pair.getValue().toString()));

            if (count > reqCount) {
                break;
            }
            it.remove(); // avoids a ConcurrentModificationException
            count++;
        }

        return tempMap;
    }


}