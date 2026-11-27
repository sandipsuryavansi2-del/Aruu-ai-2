package com.aruu.ai2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class PdfMaker {

    private PdfMaker() { }

    static File create(Context ctx, String title, String body) throws IOException {
        final int pageW = 595;
        final int pageH = 842;
        final int margin = 48;
        final int textW = pageW - 2 * margin;
        final int contentH = pageH - 2 * margin;

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.rgb(40, 20, 60));
        tp.setTextSize(12f);

        String all = title + "\n\n" + body;
        StaticLayout layout = StaticLayout.Builder.obtain(all, 0, all.length(), tp, textW)
                .setLineSpacing(3f, 1.15f)
                .setIncludePad(false)
                .build();

        PdfDocument doc = new PdfDocument();
        try {
            int pages = Math.max(1, (layout.getHeight() + contentH - 1) / contentH);
            for (int p = 0; p < pages; p++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, p + 1).create();
                PdfDocument.Page page = doc.startPage(info);
                Canvas c = page.getCanvas();
                c.save();
                c.translate(margin, margin - p * contentH);
                c.clipRect(0, p * contentH, textW, (p + 1) * contentH);
                layout.draw(c);
                c.restore();
                doc.finishPage(page);
            }
            File dir = new File(ctx.getFilesDir(), "pdfs");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("PDF folder nahi bana");
            File out = new File(dir, "Aruu_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fo = new FileOutputStream(out)) {
                doc.writeTo(fo);
            }
            return out;
        } finally {
            doc.close();
        }
    }
}
