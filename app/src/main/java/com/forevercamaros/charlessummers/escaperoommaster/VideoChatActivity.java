package com.forevercamaros.charlessummers.escaperoommaster;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.forevercamaros.charlessummers.escaperoommaster.adapters.ChatAdapter;
import com.forevercamaros.charlessummers.escaperoommaster.adt.ChatMessage;
import com.forevercamaros.charlessummers.escaperoommaster.servers.XirSysRequest;
import com.forevercamaros.charlessummers.escaperoommaster.util.Constants;
import com.forevercamaros.charlessummers.escaperoommaster.util.LogRTCListener;
import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnSignalingParams;

/**
 * This chat will begin/subscribe to a video chat.
 * REQUIRED: The intent must contain a
 */
public class VideoChatActivity extends Activity implements PinchZoomGLSurfaceView.ScaleChangeListener {
    public static final String VIDEO_TRACK_ID = "videoPN";
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks remoteRender;
    private VideoRenderer.Callbacks remoteRender2;
    private PinchZoomGLSurfaceView videoView;
    private EditText mChatEditText;
    private TextView mCallStatus;
    private CountDownTimerPausable countDownTimer;

    private boolean countdownPaused = false;


    private String username;
    private boolean backPressed = false;
    private Thread  backPressedThread = null;

    private String stdByChannel;
    private Pubnub mPubNub;

    private boolean TwoMinuteWarningSent = false;

    private int countDownLength =130000;

    private Context context;


    private int currentCamera = 1;

    private Boolean cam1Connected = false, cam2Connected = false;

    private VideoRenderer videoRenderer1, videoRenderer2;

    private MediaStream remoteStream1, remoteStream2;
    private TextView reset_room;
    private String timeLeft;

    private TextView pause_countdown;

    private LinearLayout button_box;

    private boolean boolTestTimer = true;

    @Override
    public void onScaleChange(float scale) {
        final TextView lblScale = (TextView)findViewById(R.id.lblScale);
        lblScale.setText((Float.toString(scale)));
        lblScale.animate().setListener(null);
        lblScale.setVisibility(View.VISIBLE);
        lblScale.setAlpha(1.0f);
        lblScale.animate().setDuration(1500).alpha(0.0f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                lblScale.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }).setStartDelay(1500);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        button_box = (LinearLayout)findViewById(R.id.button_box);

        pause_countdown = (TextView)findViewById(R.id.pause_countdown);
        pause_countdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countDownTimer != null){
                    if (countdownPaused){
                        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                        dlgAlert.setMessage("Are you sure that you wish to pause the countdown?");
                        dlgAlert.setTitle("Escape Room Master");
                        dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                countdownPaused=false;
                                countDownTimer.start();
                                pause_countdown.setText("Pause Countdown");
                            }
                        });
                        dlgAlert.setNegativeButton("No",null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();

                    }else{
                        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                        dlgAlert.setMessage("Are you sure that you wish to unpause the countdown?");
                        dlgAlert.setTitle("Escape Room Master");
                        dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                countdownPaused=true;
                                countDownTimer.pause();
                                pause_countdown.setText("unPause Countdown");
                            }
                        });
                        dlgAlert.setNegativeButton("No",null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();
                    }
                }
            }
        });

        ImageButton switchCameras = (ImageButton)findViewById(R.id.btnSwitchCameras);
        switchCameras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setClickable(false);
                view.setEnabled(false);
                remoteStream1.videoTracks.get(0).removeRenderer(videoRenderer1);
                remoteStream2.videoTracks.get(0).removeRenderer(videoRenderer2);
                VideoRenderGuiWithZoom.remove(remoteRender);
                VideoRenderGuiWithZoom.remove(remoteRender2);
                if (currentCamera == 1){
                    currentCamera = 2;
                    remoteRender2 = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
                    remoteRender = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FILL, false);
                    videoRenderer1 = new VideoRenderer(remoteRender);
                    videoRenderer2 = new VideoRenderer(remoteRender2);
                    remoteStream1.videoTracks.get(0).addRenderer(videoRenderer1);
                    remoteStream2.videoTracks.get(0).addRenderer(videoRenderer2);
                    VideoRenderGuiWithZoom.update(remoteRender2, 0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
                    VideoRenderGuiWithZoom.update(remoteRender, 72, 65, 25, 25, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FIT, false);
                }else {
                    currentCamera = 1;
                    remoteRender = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
                    remoteRender2 = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FILL, false);
                    videoRenderer1 = new VideoRenderer(remoteRender);
                    videoRenderer2 = new VideoRenderer(remoteRender2);
                    remoteStream1.videoTracks.get(0).addRenderer(videoRenderer1);
                    remoteStream2.videoTracks.get(0).addRenderer(videoRenderer2);
                    VideoRenderGuiWithZoom.update(remoteRender, 0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
                    VideoRenderGuiWithZoom.update(remoteRender2, 72, 65, 25, 25, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FIT, false);
                }
                try{
                    Thread.sleep(2000);
                }catch (Exception e){}

                view.setClickable(true);
                view.setEnabled(true);
            }
        });

        TextView room_finished = (TextView)findViewById(R.id.room_finished);
        context=this;
        room_finished.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                dlgAlert.setMessage("Are you sure that the room is complete?");
                dlgAlert.setTitle("Escape Room Master");
                dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        countDownTimer.cancel();
                        ChatMessage chatMsg = new ChatMessage(username, "NOOO!!!\nYou Win!\n" + timeLeft, System.currentTimeMillis());
                        sendMessage(chatMsg,"time");
                        chatMsg = new ChatMessage(username, "turn off family room lamp", System.currentTimeMillis());
                        sendMessage(chatMsg,"assistant_command");
                        chatMsg = new ChatMessage(username,"turn on random outlet",System.currentTimeMillis());
                        sendMessage(chatMsg,"assistant_command");
                        chatMsg = new ChatMessage(username,"turn on living room floor lamp",System.currentTimeMillis());
                        sendMessage(chatMsg,"assistant_command");
                        chatMsg = new ChatMessage(username,"win",System.currentTimeMillis());
                        sendMessage(chatMsg,"music");

                    }
                });
                dlgAlert.setNegativeButton("No",null);
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        });

        reset_room = (TextView)findViewById(R.id.reset_room);
        reset_room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                dlgAlert.setMessage("Are you sure that you wish to reset the room?");
                dlgAlert.setTitle("Escape Room Master");
                dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reset_room();
                    }
                });
                dlgAlert.setNegativeButton("No",null);
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
            }
        });

        this.username     = "ESCAPE_ROOM_MASTER";
        this.stdByChannel = this.username + Constants.STDBY_SUFFIX;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.mChatEditText = (EditText) findViewById(R.id.chat_input);

        this.mCallStatus   = (TextView) findViewById(R.id.call_status);

        mCallStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallStatus.getText().equals("Connect")){
                    mCallStatus.setText("Connecting...");
                    dispatchCall("ESCAPE_ROOM");
                }else if (!mCallStatus.getText().equals("Connecting...")){
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);
                    dlgAlert.setMessage("Are you sure that you wish to start the room?");
                    dlgAlert.setTitle("Escape Room Master");
                    dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            start_room();
                        }
                    });
                    dlgAlert.setNegativeButton("No",null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }
            }
        });

        // First, we initiate the PeerConnectionFactory with our application context and some options.
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        this.pnRTCClient = new PnRTCClient(Constants.PUB_KEY, Constants.SUB_KEY, this.username);
        List<PeerConnection.IceServer> servers = getXirSysIceServers();
        if (!servers.isEmpty()){
            this.pnRTCClient.setSignalParams(new PnSignalingParams());
        }

        // Returns the number of cams & front/back face device name
        int camNumber = VideoCapturerAndroid.getDeviceCount();
        String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String backFacingCam = VideoCapturerAndroid.getNameOfBackFacingDevice();

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturer capturer = VideoCapturerAndroid.create(frontFacingCam);

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, this.pnRTCClient.videoConstraints());
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);


        // To create our VideoRenderer, we can use the included VideoRenderGuiWithZoom for simplicity
        // First we need to set the GLSurfaceView that it should render to
        this.videoView = (PinchZoomGLSurfaceView) findViewById(R.id.gl_surface);

        videoView.addScaleChangeListener(this);
        // Then we set that view, and pass a Runnable to run once the surface is ready
        VideoRenderGuiWithZoom.setView(videoView, null);

        // Now that VideoRenderGuiWithZoom is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        remoteRender = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
        remoteRender2 = VideoRenderGuiWithZoom.create(0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FILL, false);

        // We start out with an empty MediaStream object, created with help from our PeerConnectionFactory
        //  Note that LOCAL_MEDIA_STREAM_ID can be any string
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);


        // First attach the RTC Listener so that callback events will be triggered
        this.pnRTCClient.attachRTCListener(new DemoRTCListener());

        // Then attach your local media stream to the PnRTCClient.
        //  This will trigger the onLocalStream callback.
        this.pnRTCClient.attachLocalMediaStream(mediaStream);

        // Listen on a channel. This is your "phone number," also set the max chat users.
        this.pnRTCClient.listenOn("Kevin");
        this.pnRTCClient.setMaxConnections(2);

        initPubNub();

        mCallStatus.setText("Connecting...");
        dispatchCall("ESCAPE_ROOM");
        dispatchCall("ESCAPE_ROOM_2");

    }

    public void start_room(){
        if (countDownTimer != null){
            countDownTimer.cancel();
        }
        ChatMessage chatMsg = new ChatMessage(username,"background", System.currentTimeMillis());
        sendMessage(chatMsg,"music");
        TwoMinuteWarningSent=false;
        countDownTimer = new CountDownTimerPausable(countDownLength, 1000) {

            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished<120000 && !TwoMinuteWarningSent){
                    TwoMinuteWarningSent=true;
                    ChatMessage chatMsg = new ChatMessage(username, "2min_warning", System.currentTimeMillis());
                    sendMessage(chatMsg,"music");
                }
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                int minutes =  ((int)(millisUntilFinished / 1000) / 60) % 60;
                int hours = (int)(millisUntilFinished / 1000) / 3600;

                if (hours == 0) {
                    timeLeft = String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
                }else{
                    timeLeft = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
                }
                mCallStatus.setText("Restart Timer: " + timeLeft);
                ChatMessage chatMsg = new ChatMessage(username, timeLeft, System.currentTimeMillis());
                sendMessage(chatMsg,"time");
            }

            public void onFinish() {
                mCallStatus.setText("Restart Timer: " + "00:00:00");
                ChatMessage chatMsg = new ChatMessage(username, "Your Souls Are MINE!!!!", System.currentTimeMillis());
                sendMessage(chatMsg,"time");
                chatMsg = new ChatMessage(username, "turn off Family Room Lamp", System.currentTimeMillis());
                sendMessage(chatMsg,"assistant_command");
                chatMsg = new ChatMessage(username, "turn on living room floor lamp", System.currentTimeMillis());
                sendMessage(chatMsg,"assistant_command");
                chatMsg = new ChatMessage(username, "turn on random outlet", System.currentTimeMillis());
                sendMessage(chatMsg,"assistant_command");
                chatMsg = new ChatMessage(username,"lose",System.currentTimeMillis());
                sendMessage(chatMsg,"music");
                if (boolTestTimer){
                    try{
                        Thread.sleep(2000);
                    }catch (Exception e){}
                    reset_room();
                    start_room();
                }
                try{
                    Thread.sleep(500);
                }catch (Exception e){}
            }
        }.start();
    }
    public void toggle(View view) {
        final LinearLayout call_chat_box = (LinearLayout)findViewById(R.id.call_chat_box);
        if (mVisible){
            mVisible=false;
            button_box.animate().alpha((0.0f)).setDuration((1000)).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    button_box.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            }).start();
            call_chat_box.animate().alpha(0.0f).setDuration(1000).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    call_chat_box.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            }).start();

        }else {
            mVisible=true;
            call_chat_box.animate().setListener(null);
            button_box.animate().setListener(null);
            call_chat_box.setVisibility(View.VISIBLE);
            call_chat_box.animate().alpha(1.0f).setDuration(1000).start();
            button_box.setVisibility(View.VISIBLE);
            button_box.animate().alpha(1.0f).setDuration(1000).start();
        }
    }

    private boolean mVisible = true;

    public void initPubNub(){
        this.mPubNub  = new Pubnub(Constants.PUB_KEY, Constants.SUB_KEY);
        this.mPubNub.setUUID(this.username);
        subscribeStdBy();
    }

    private void subscribeStdBy(){
        try {
            this.mPubNub.subscribe(this.stdByChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-iPN", "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    if (!jsonMsg.has(Constants.JSON_CALL_USER)) return;
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("MA-iPN", "CONNECTED: " + message.toString());
                    setUserStatus(Constants.STATUS_AVAILABLE);
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("MA-iPN","ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e){
            Log.d("HERE","HEREEEE");
            e.printStackTrace();
        }
    }

    public void dispatchCall(final String callNum){
        final String callNumStdBy = callNum + Constants.STDBY_SUFFIX;
        this.mPubNub.hereNow(callNumStdBy, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                //try{Thread.sleep(1000);}catch (Exception e){};
                Log.d("MA-dC", "HERE_NOW: " +" CH - " + callNumStdBy + " " + message.toString());
                try {
                    int occupancy = ((JSONObject) message).getInt(Constants.JSON_OCCUPANCY);
                    if (occupancy == 0) {
                        showToast("User is not online!");
                        VideoChatActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCallStatus.setText("Connect");
                            }
                        });
                        return;
                    }
                    JSONObject jsonCall = new JSONObject();
                    jsonCall.put(Constants.JSON_CALL_USER, username);
                    jsonCall.put(Constants.JSON_CALL_TIME, System.currentTimeMillis());
                    mPubNub.publish(callNumStdBy, jsonCall, new Callback() {
                        @Override
                        public void successCallback(String channel, Object message) {
                            //try{Thread.sleep(1000);}catch (Exception e){};
                            Log.d("MA-dC", "SUCCESS: " + message.toString());
                            connectToUser(callNum);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void reset_room(){
        if (countDownTimer != null){
            countDownTimer.cancel();
        }
        TwoMinuteWarningSent=true;
        ChatMessage chatMsg = new ChatMessage(username, "stop_music", System.currentTimeMillis());
        sendMessage(chatMsg,"music");
        int seconds = (int) (countDownLength / 1000) % 60;
        int minutes =  ((int)(countDownLength / 1000) / 60) % 60;
        int hours = (int)(countDownLength / 1000) / 3600;
        String time;
        if (hours == 0) {
            time = String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        }else{
            time = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        }

        chatMsg = new ChatMessage(username, "ARE YOU READY?\n" + time, System.currentTimeMillis());
        sendMessage(chatMsg,"time");
        chatMsg = new ChatMessage(username, "turn on family room lamp", System.currentTimeMillis());
        sendMessage(chatMsg,"assistant_command");
        chatMsg = new ChatMessage(username,"turn off random outlet",System.currentTimeMillis());
        sendMessage(chatMsg,"assistant_command");
        chatMsg = new ChatMessage(username,"turn off living room floor lamp",System.currentTimeMillis());
        sendMessage(chatMsg,"assistant_command");
        mCallStatus.setText("Start Timer");
    }
    private void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserStatus(String status){
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.JSON_STATUS, status);
            this.mPubNub.setState(this.stdByChannel, this.username, state, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("MA-sUS","State Set: " + message.toString());
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_video_chat, menu);
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
    protected void onPause() {
        super.onPause();
        this.videoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localVideoSource != null){
            this.videoView.onResume();
            this.localVideoSource.restart();
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.backPressed){
            this.backPressed = true;
            Toast.makeText(this,"Press back again to end.",Toast.LENGTH_SHORT).show();
            this.backPressedThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        backPressed = false;
                    } catch (InterruptedException e){ Log.d("VCA-oBP","Successfully interrupted"); }
                }
            });
            this.backPressedThread.start();
            return;
        }
        if (this.backPressedThread != null)
            this.backPressedThread.interrupt();
        super.onBackPressed();
    }

    public List<PeerConnection.IceServer> getXirSysIceServers(){
        List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>();
        try {
            servers = new XirSysRequest().execute().get();
        } catch (InterruptedException e){
            e.printStackTrace();
        }catch (ExecutionException e){
            e.printStackTrace();
        }
        return servers;
    }

    public void connectToUser(String user) {
        this.pnRTCClient.connect(user);
    }

    private void endCall() {
        finish();
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        this.pnRTCClient.closeAllConnections();
        endCall();
        if (this.localVideoSource != null) {
            this.localVideoSource.stop();
        }
        if (this.pnRTCClient != null) {
            this.pnRTCClient.onDestroy();
        }
    }


    public void sendMessage(View view) {
        String message = mChatEditText.getText().toString();
        if (message.equals("")) return; // Return if empty
        ChatMessage chatMsg = new ChatMessage(this.username, message, System.currentTimeMillis());
        sendMessage(chatMsg,"hint");
        // Hide keyboard when you send a message.
        View focusView = this.getCurrentFocus();
        if (focusView != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        mChatEditText.setText("");
    }


    private void  sendMessage(ChatMessage chatMsg, String type){
        JSONObject messageJSON = new JSONObject();
        try {
            switch (type){
                case "time":
                    messageJSON.put(Constants.JSON_MSG_UUID, "time");
                    break;
                case "assistant_command":
                    messageJSON.put(Constants.JSON_MSG_UUID, "assistant_command");
                    break;
                case "music":
                    messageJSON.put(Constants.JSON_MSG_UUID, "music");
                    break;
                default:
                    messageJSON.put(Constants.JSON_MSG_UUID, chatMsg.getSender());
                    break;
            }

            messageJSON.put(Constants.JSON_MSG, chatMsg.getMessage());
            messageJSON.put(Constants.JSON_TIME, chatMsg.getTimeStamp());
            this.pnRTCClient.transmitAll(messageJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * LogRTCListener is used for debugging purposes, it prints all RTC messages.
     * DemoRTC is just a Log Listener with the added functionality to append screens.
     */
    private class DemoRTCListener extends LogRTCListener {
        @Override
        public void onLocalStream(final MediaStream localStream) {
            super.onLocalStream(localStream); // Will log values
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(localStream.videoTracks.size()==0) return;
                    //localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            });
        }





        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            super.onAddRemoteStream(remoteStream, peer); // Will log values
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoChatActivity.this,"Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();
                    if (peer.getId().equals("ESCAPE_ROOM")){
                        try {
                            if(remoteStream.audioTracks.size()==0 || remoteStream.videoTracks.size()==0) return;
                            cam1Connected=true;
                            mCallStatus.setText("Start Timer");
                            TextView reset_room = (TextView)findViewById(R.id.reset_room);
                            reset_room.setVisibility(View.VISIBLE);
                            TextView room_finished = (TextView)findViewById(R.id.room_finished);
                            room_finished.setVisibility(View.VISIBLE);
                            pause_countdown.setVisibility(View.VISIBLE);
                            LinearLayout call_chat_box = (LinearLayout)findViewById(R.id.call_chat_box);
                            call_chat_box.setVisibility(View.VISIBLE);
                            videoRenderer1 = new VideoRenderer(remoteRender);
                            remoteStream1=remoteStream;
                            remoteStream1.videoTracks.get(0).addRenderer(videoRenderer1);
                            VideoRenderGuiWithZoom.update(remoteRender, 0, 0, 100, 100, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_BALANCED, false);
                            reset_room();
                            if (boolTestTimer){
                                start_room();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (1==1){
                                            ChatMessage chatMsg = new ChatMessage(username, "Test Hint", System.currentTimeMillis());
                                            sendMessage(chatMsg,"hint");
                                            try{
                                                Thread.sleep(60000);
                                            }catch (Exception e){}
                                        }
                                    }
                                }).start();

                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }else {
                        try {
                            if(remoteStream.audioTracks.size()==0 || remoteStream.videoTracks.size()==0) return;
                            cam2Connected=true;
                            videoRenderer2 = new VideoRenderer(remoteRender2);
                            remoteStream2=remoteStream;
                            remoteStream2.videoTracks.get(0).addRenderer(videoRenderer2);
                            VideoRenderGuiWithZoom.update(remoteRender2, 72, 65, 25, 25, VideoRenderGuiWithZoom.ScalingType.SCALE_ASPECT_FIT, false);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    if (cam2Connected && cam1Connected){
                        ImageButton btnSwitchCameras = (ImageButton)findViewById(R.id.btnSwitchCameras);
                        btnSwitchCameras.setVisibility(View.VISIBLE);
                    }

                }
            });
        }

        @Override
        public void onMessage(PnPeer peer, Object message) {
            super.onMessage(peer, message);  // Will log values
            if (!(message instanceof JSONObject)) return; //Ignore if not JSONObject
            JSONObject jsonMsg = (JSONObject) message;
            try {
                String uuid = jsonMsg.getString(Constants.JSON_MSG_UUID);
                String msg  = jsonMsg.getString(Constants.JSON_MSG);
                long   time = jsonMsg.getLong(Constants.JSON_TIME);
                final ChatMessage chatMsg = new ChatMessage(uuid, msg, time);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            super.onPeerConnectionClosed(peer);
            VideoChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallStatus.setText("Call Ended...");
                    videoView.onPause();
                    localVideoSource.stop();
                }
            });
            try {Thread.sleep(1500);} catch (InterruptedException e){e.printStackTrace();}
        }
    }
}