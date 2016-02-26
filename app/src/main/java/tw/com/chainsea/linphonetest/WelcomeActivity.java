package tw.com.chainsea.linphonetest;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import tw.com.chainsea.linphonetest.R;

import static android.content.Intent.ACTION_MAIN;

public class WelcomeActivity extends ActionBarActivity {

    Handler mHandler = new Handler();
    private ServiceWaitThread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    protected void onServiceReady() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent().setClass(WelcomeActivity.this, CallingActivity.class));
                finish();
            }
        }, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
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

        return super.onOptionsItemSelected(item);
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }
}
