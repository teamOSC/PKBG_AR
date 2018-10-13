package com.tosc.pkbg.ar.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;

import java.util.List;

public class MLKit {

    private Context context;

    public MLKit(Context context) {
        this.context = context;
    }
    public interface OnHitDetectListener {
        void onHitDetected();
    }
    public void detectFace (Bitmap bitmap, OnHitDetectListener ohdl) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector();

        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Log.w("FACE", "SUCCESS " + firebaseVisionFaces.size());
                        firebaseVisionFaces.forEach(face -> {
                            if(face.getBoundingBox().contains(150, 150)) {
                                if (ohdl != null) {
                                    ohdl.onHitDetected();
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FACE", "Could not detect", e);
                        Toast.makeText(context, "Missed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<FirebaseVisionFace>> task) {
                        Log.w("FACE", "Done");
                    }
                });
    }
}
