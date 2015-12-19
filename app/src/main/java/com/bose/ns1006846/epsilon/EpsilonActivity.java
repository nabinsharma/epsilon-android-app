package com.bose.ns1006846.epsilon;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class EpsilonActivity extends AppCompatActivity {
    private static final String TAG = "EpsilonActivity";

    private int sampleRateInHz = 44100;
    private int maxListenDurationInSec = 6;
    private int maxNumAudioSamples = sampleRateInHz * maxListenDurationInSec;
    private int readSizeInShort;
    private short audioBuffer[] = new short[maxNumAudioSamples];
    private int audioBufferOffset;

    private AudioRecord audioRecord = null;
    private boolean recordingInProgress = false;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epsilon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.listen_toggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled: record.
                    Thread t_record = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            recordAudio();
                        }
                    });
                    t_record.start();
                } else {
                    recordingInProgress = false;
                    audioRecord.stop();
                    audioRecord.release();
                    Log.d(TAG, "Recorded " + audioBufferOffset + " samples!");
                    // Data is recorded, send to cloud.
                    new sendDataToServer().execute("www.nabinsharma.com");
                }
            }
        });

        Button button = (Button) findViewById(R.id.play_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Thread t_play = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        playAudio();
                    }
                });
                t_play.start();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "TODO: Send an email to Nabin.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_epsilon, menu);
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

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Epsilon Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.bose.ns1006846.epsilon/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Epsilon Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.bose.ns1006846.epsilon/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private void recordAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // Using 2 times larger buffer (compared to minimum).
        readSizeInShort = minBufferSize / 2;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2 * readSizeInShort);
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "AudioRecord initialized successfully!");
        }
        audioBufferOffset = 0;
        recordingInProgress = true;
        Log.d(TAG, "Recording ...");
        while(recordingInProgress) {
            if(audioRecord == null) {
                Log.d(TAG, "NULL AudioRecord instance. Something went wrong!");
                return;
            }
            audioRecord.startRecording();
            int audioRecordingState = audioRecord.getRecordingState();
            if(audioRecordingState != AudioRecord.RECORDSTATE_RECORDING) {
                finish();
            }
            int n = audioRecord.read(audioBuffer, audioBufferOffset, readSizeInShort);
            audioBufferOffset += n;
            recordingInProgress = recordingInProgress && (audioBufferOffset < maxNumAudioSamples);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToggleButton toggle = (ToggleButton) findViewById(R.id.listen_toggle);
                toggle.setChecked(false);
            }
        });
    }

    private void playAudio() {
        int maxJitter = AudioTrack.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxJitter, AudioTrack.MODE_STREAM);
        int numSamplesWritten = 0;
        Log.d(TAG, "Playing ...");
        audioTrack.play();
        while (numSamplesWritten < audioBufferOffset) {
            int n = audioTrack.write(audioBuffer, numSamplesWritten, readSizeInShort);
            numSamplesWritten += n;
        }
        audioTrack.stop();
        audioTrack.release();
        Log.d(TAG, "Playing ... Done.");
    }

    private JSONObject jsonifyAudioData() {
        JSONObject dataPacket = new JSONObject();
        JSONArray audioData = new JSONArray(Arrays.asList(audioBuffer));
        try {
            dataPacket.put("data", audioData);
            dataPacket.put("length", audioBufferOffset);
            dataPacket.put("channels", 1);
        } catch (JSONException e) {
            // TODO
        }
        return dataPacket;
    }

    private class sendDataToServer extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            android.os.Debug.waitForDebugger();
            Boolean result = false;
            URL url = null;
            try {
                url = new URL(params[0]);
            } catch (MalformedURLException e) {
                // TODO
            }
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
/*                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setChunkedStreamingMode(0);
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                JSONObject data = jsonifyAudioData();
                out.write(data.toString().getBytes());*/
            } catch (IOException e) {
                Log.e(TAG, "Uninitialized URL Connection!", e);
            }
            finally {
                urlConnection.disconnect();
                result = true;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }

    }

}
