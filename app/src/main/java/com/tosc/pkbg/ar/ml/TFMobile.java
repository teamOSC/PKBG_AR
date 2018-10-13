package com.tosc.pkbg.ar.ml;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.tosc.pkbg.ar.Utils;

import java.io.IOException;
import java.util.List;

public class TFMobile {
    public static final int TF_OD_API_INPUT_SIZE = 300;
    public static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    public static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    public static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
    public static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    int cropSize = TFMobile.TF_OD_API_INPUT_SIZE;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private Activity activity;
    private Classifier detector;

    private void initClassifier() {

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    activity.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            Log.e("lol", "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            activity,
                            "Classifier could not be initialized",
                            Toast.LENGTH_SHORT
                    );
            toast.show();
        }
    }

    public TFMobile(Activity activity) {
        this.activity = activity;
        initClassifier();
    }

    private boolean isRectInCrosshair(RectF location) {

        int height = 300;
        int width = 300;
        return location.contains(width /2, height / 2);
    }

    public boolean detectImage(Bitmap bitmap) {
        boolean isHit = false;

        final List<Classifier.Recognition> results = detector.recognizeImage(bitmap);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                Log.e("lol", "found " + result.getTitle());
                Log.e("lol", "x "+ location.left);
                Log.e("lol", "y "+ location.top);
                if (result.getTitle().equals("person")) {
                    if (isRectInCrosshair(location)) {
                        isHit = true;
                        break;
                    }
                }
            }
        }

        return isHit;
    }



}
