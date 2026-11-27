package com.aruu.ai2;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class MediaUtil {

    private MediaUtil() { }

    private static InputStream open(ContentResolver cr, Uri uri) throws IOException {
        InputStream in = cr.openInputStream(uri);
        if (in == null) throw new IOException("File nahi khul rahi");
        return in;
    }

    static Bitmap decodeScaled(ContentResolver cr, Uri uri, int maxDim) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(cr, uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Ye image padh nahi paayi");
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = open(cr, uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("Image decode nahi hui");
        float scale = Math.min(1f, maxDim / (float) Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f) {
            bmp = Bitmap.createScaledBitmap(bmp,
                    Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
        }
        return bmp;
    }

    static byte[] jpegBytes(Bitmap bmp) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, bo);
        return bo.toByteArray();
    }

    static String base64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    static File saveFile(File dir, String name, byte[] data) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder nahi bana");
        File f = new File(dir, name);
        try (FileOutputStream fo = new FileOutputStream(f)) {
            fo.write(data);
        }
        return f;
    }

    static byte[] readBytes(ContentResolver cr, Uri uri, long maxBytes) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (InputStream in = open(cr, uri)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bo.write(buf, 0, n);
                if (bo.size() > maxBytes) throw new IOException("File bahut badi hai (max 18 MB)");
            }
        }
        return bo.toByteArray();
    }
}
