package com.tosc.pkbg.ar;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
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
}
