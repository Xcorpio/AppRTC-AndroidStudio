package org.xcorpio.myapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.net.URI;

public class MainActivity extends AppCompatActivity implements RtcListener {

    private String roomURI;
    private TextView textView;
    private EditText editText;
    private Button button;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;

    private EglBase rootEglBase;

    private RoomClient roomClient;
    private PeerConnectionClient peerConnectionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
    }

    private void connectToServer() {
        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);

        // connect signaling server
        roomURI = editText.getText().toString();
        roomClient = new RoomClient(URI.create(roomURI));
        peerConnectionClient = new PeerConnectionClient(this);
        peerConnectionClient.setRoomClient(roomClient);
        peerConnectionClient.start();
        roomClient.setSignalingEventListener(peerConnectionClient);
        roomClient.connect();
    }

    private void initLayout() {
        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText);
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textView.setText(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToServer();
            }
        });
    }

    @Override
    public void onCallReady(String callId) {
        Toast.makeText(getApplicationContext(), "onCallReady", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Toast.makeText(getApplicationContext(), "onLocalStream", Toast.LENGTH_LONG).show();
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Toast.makeText(getApplicationContext(), "onRemoteStream", Toast.LENGTH_LONG).show();
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        Toast.makeText(getApplicationContext(), "onRemoveStream", Toast.LENGTH_LONG).show();
    }
}
