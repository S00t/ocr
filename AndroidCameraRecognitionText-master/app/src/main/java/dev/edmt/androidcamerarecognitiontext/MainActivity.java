package dev.edmt.androidcamerarecognitiontext;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.edmt.androidcamerarecognitiontext.R.id.text_label;

public class MainActivity extends AppCompatActivity {
    Dialog dialog;
    List<String> nips = new ArrayList<>();
    boolean showDialog = false;
    public static final String NIP_REGEX = "(((NIP)?:?\\u0020?(PL)?)\\u0020?\\d{3}\\u0020{1}\\d{2,3}\\u0020{1}\\d{2,3}\\u0020{1}\\d{2,3})|(((NIP)?:?\\u0020?(PL)?)\\u0020?\\d{10})|(((NIP)?:?\\u0020?(PL)?)\\u0020?\\d{3}\\u0020{1}\\d{3}\\u0020{1}\\d{4})";
    SurfaceView cameraView;
    TextView textView;
    TextView label;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        dialog = new Dialog(MainActivity.this);
        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);
        label = (TextView) findViewById(R.id.text_label);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {

            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.CAMERA},
                                        RequestCameraPermissionID);
                             return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if(items.size() != 0)
                    {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i =0;i<items.size();++i)
                                {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");

                                }

                                Pattern p = Pattern.compile(NIP_REGEX);

                                Matcher m = p.matcher(stringBuilder.toString());
                                while(m.find()&& dialog!= null && !dialog.isShowing() && nips.size()<15 ) {

                                    if(!nips.contains(m.group())){
                                        nips.add(m.group() + "\n");
                                    }
//                                    label.setText("Wykryto :");
//                                    textView.setText(m.group());
//                                    ShowDialog(m.group());
                                }
                                if(nips!=null && nips.size()==15 && !dialog.isShowing()){
                                    ShowDialog(nips);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void ShowDialog(List<String> group) {
        int count =1;
        // Create custom dialog object
        dialog = new Dialog(MainActivity.this);
        // Include dialog.xml file
        dialog.setContentView(R.layout.dialog);
        // Set dialog title
        dialog.setTitle("Wykryto NIP");

        // set values for custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.textDialog);
        String finds = "";
        for(String item : group){
            item.replaceAll("\\u0020+","");
            item.replaceAll("PL","");
            item.replaceAll("NIP","");

            finds+=count + ". " + " " +item;
            count++;
        }

        text.setText(finds);
        dialog.show();
        if(dialog.isShowing()){
            showDialog = false;
        }
        Button declineButton = (Button) dialog.findViewById(R.id.declineButton);
        // if decline button is clicked, close the custom dialog
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close dialog

                dialog.dismiss();
                label.setText("Skanowanie ...");
                textView.setText("");
                showDialog = false;
                nips.clear();

            }
        });

        if(cameraSource != null && showDialog){
            cameraSource.stop();
        }

    }


}
