package com.tosc.pkbg.ar;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
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

    static void vibrate(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            v.vibrate(1000);
        }
    }

    static void playFireSound(Context context) {
        MediaPlayer mPlayer;
        mPlayer = MediaPlayer.create(context, R.raw.fire_sound);
        mPlayer.start();
    }

    static void playHitSound(Context context) {
        MediaPlayer mPlayer;
        mPlayer = MediaPlayer.create(context, R.raw.hit_sound);
        mPlayer.start();
    }
}
