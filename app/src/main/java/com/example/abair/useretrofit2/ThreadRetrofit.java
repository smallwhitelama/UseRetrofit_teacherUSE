package com.example.abair.useretrofit2;

import android.content.Context;
import android.util.Log;

import com.androidplot.Plot;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for invoking Plot.redraw() on a background thread
 * at a set frequency.  Callers should be sure to create a separate thread from which to use this class.
 */
public class ThreadRetrofit implements Runnable {

    private static final int ONE_SECOND_MS = 1000;

    private static final String TAG = "ThreadRetrofit2";

    private List<WeakReference<Plot>> plots;
    private long sleepTime;

    // used to temporarily pause rendering without disposing of the run thread
    private boolean keepRunning;

    // when set to false, run thread will be allowed to exit the main run loop
    private boolean keepAlive;

    private Thread thread;
    private MyApp myApp;



    public ThreadRetrofit(Context context, float maxRefreshRate, boolean startImmediately) {
        this.myApp = (MyApp) context.getApplicationContext();

        setMaxRefreshRate(maxRefreshRate);
        thread = new Thread(this);
        thread.start();
        if(startImmediately) {
            start();
        }
    }

    public synchronized void pause() {
        keepRunning = false;
        notify();
        Log.d(TAG, "ThreadRetrofit paused.");
    }

    public synchronized void start() {
        keepRunning = true;
        notify();
        Log.d(TAG, "ThreadRetrofit started.");
    }

    public synchronized void finish() {
        keepRunning = false;
        keepAlive = false;
        notify();
    }

    @Override
    public void run() {
        keepAlive = true;
        try {
            while(keepAlive) {
                if(keepRunning) {
                    //要執行的程式碼，請放在這
                    Call<List<demoData>> readLastestOneClone = myApp.readLatestOne.clone();
                    readLastestOneClone.enqueue(new Callback<List<demoData>>() {
                        @Override
                        public void onResponse(Call<List<demoData>> call, Response<List<demoData>> response) {
                            myApp.resultDemoData=response.body();
                        if(myApp.resultDemoData==null){
                            Log.d(TAG,"myApp.resultDemoDate ==null");
                            return;
                        }
                            Iterator it = myApp.resultDemoData.iterator();
                            while (it.hasNext()){
                                Log.d(TAG,((demoData)it.next()).SENSOR);
                            }

                        }

                        @Override
                        public void onFailure(Call<List<demoData>> call, Throwable t) {
                            t.printStackTrace();

                        }
                    });





                    synchronized (this) {
                        wait(sleepTime);
                    }
                } else {
                    // sleep until notified
                    synchronized (this) {
                        wait();
                    }
                }
            }
        } catch(InterruptedException e) {

        } finally {
            Log.d(TAG, "ThreadRetrofit thread exited.");
        }
    }

    public void setMaxRefreshRate(float refreshRate) {
        sleepTime = (long)(ONE_SECOND_MS / refreshRate);
        Log.d(TAG, "Set ThreadRetrofit refresh rate to " +
                refreshRate + "( " + sleepTime + " ms)");
    }
}
