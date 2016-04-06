package org.xcorpio.myapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private String roomURI;
    private TextView textView;
    private EditText editText;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;

    private EglBase rootEglBase;
    private PeerConnectionFactory factory;

    private RoomClient roomClient;
    MediaStream localMediaStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        initLocalVideo();
    }

    private void initLocalVideo() {
        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        localRender.setZOrderMediaOverlay(true);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);

        // create PeerConnectionFactory
        boolean initializeAudio = true;
        boolean initializeVideo = true;
        boolean videoCodecHwAcceleration = true;
        boolean ret = PeerConnectionFactory.initializeAndroidGlobals(MainActivity.this, initializeAudio, initializeVideo, videoCodecHwAcceleration);
        Log.i(TAG.TAG_INFO, "PeerConnectionFactory initialize result:" + ret);
        factory = new PeerConnectionFactory();

        int deviceCount = CameraEnumerationAndroid.getDeviceCount();
        Log.i(TAG.TAG_INFO, "the number of device number:" + deviceCount);
        String nameOfFrontFacingDevice = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
        Log.i(TAG.TAG_INFO, "name of front facing device:" + nameOfFrontFacingDevice);
        String nameOfBackFacingDevice = CameraEnumerationAndroid.getNameOfBackFacingDevice();
        Log.i(TAG.TAG_INFO, "name of back facing device:" + nameOfBackFacingDevice);

        // create a VideoCaptureAndroid instance for the device name
        VideoCapturer videoCapturer = VideoCapturerAndroid.create(nameOfFrontFacingDevice);
        Log.i(TAG.TAG_INFO, "create video capture:" + videoCapturer);

        // create a VideoSource. VideoSource enable function to start/stop capturing your device.
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(100)));
        VideoSource videoSource = factory.createVideoSource(videoCapturer, videoConstraints);
        final String VIDEO_TRACK_ID = "ARDAMSv0";
        VideoTrack localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

        // create an AudioSource
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        AudioSource audioSource = factory.createAudioSource(audioConstraints);
        String AUDIO_TRACK_ID = "ARDAMSa0";
        AudioTrack localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        // VideoRenderer
        localVideoTrack.addRenderer(new VideoRenderer(localRender));

        // MediaStream
        localMediaStream = factory.createLocalMediaStream("LOCAL_MEDIA_STREAM_ID");
        localMediaStream.addTrack(localAudioTrack);
        localMediaStream.addTrack(localVideoTrack);

        // connect signaling server
        roomURI = editText.getText().toString();
        SignalingEventListenerImpl signalingEventListener = new SignalingEventListenerImpl();
        roomClient = new RoomClient(URI.create(roomURI), signalingEventListener, factory, localMediaStream);
        signalingEventListener.setClient(roomClient);
        roomClient.connect();
    }

    private void initLayout() {
        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText);
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textView.setText(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
