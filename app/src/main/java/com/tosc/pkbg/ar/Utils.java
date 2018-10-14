package com.tosc.pkbg.ar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Utils {

    static void  saveImage(Context ctx, Bitmap bmp) throws IOException {
        String path = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
                + "/img-" + (new Date()).getTime() + ".jpg";

        FileOutputStream outputStream = new FileOutputStream(new File(path));
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.flush();
        outputStream.close();
        bmp.recycle();
    }

    static void vibrate(Context context, int duration) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            v.vibrate(1000);
        }
    }

    static void playFireEmpty(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.fire_empty);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playFireNormal(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.fire_normal);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playFireHeadshot(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.fire_headshot);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playPainNormal(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.pain_normal);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playPainHeadshot(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.pain_headshit);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playReload(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.reload);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }


    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

}
