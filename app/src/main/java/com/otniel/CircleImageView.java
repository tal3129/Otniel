package com.otniel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by amxha on 28/01/2016.
 */
public class CircleImageView extends ImageView{

    public static  boolean crash = false;

    private boolean firstDraw = true;

        public CircleImageView(Context context) {
            super(context);
        }

        public CircleImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CircleImageView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }


    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        firstDraw = true;
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        firstDraw = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (crash){
            super.onDraw(canvas);
            return;
        }

        try {
            if (firstDraw) {


                Drawable drawable = getDrawable();

                if (drawable == null) {
                    return;
                }

                if (getWidth() == 0 || getHeight() == 0) {
                    return;
                }

                Bitmap b = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && drawable instanceof VectorDrawable) {
                    ((VectorDrawable) drawable).draw(canvas);
                    b = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas();
                    c.setBitmap(b);
                    drawable.draw(c);
                } else {
                    b = ((BitmapDrawable) drawable).getBitmap();
                }

                Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);

                int w = getWidth(), h = getHeight();

                Bitmap roundBitmap = getCroppedBitmap(bitmap, w);
                setImageBitmap(roundBitmap);

                firstDraw = false;
            }
        }catch (Exception e){
            crash = true;
        }
        //canvas.drawBitmap(roundBitmap, 0, 0, null);

        super.onDraw(canvas);
    }

    public static Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
        Bitmap sbmp;
        if(bmp.getWidth() != radius || bmp.getHeight() != radius)
            sbmp = Bitmap.createScaledBitmap(bmp, radius, radius, false);
        else
            sbmp = bmp;
        Bitmap output = Bitmap.createBitmap(sbmp.getWidth(),
                sbmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xffa19774;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, sbmp.getWidth(), sbmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(sbmp.getWidth() / 2+0.7f, sbmp.getHeight() / 2+0.7f,
                sbmp.getWidth() / 2+0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);


        return output;
    }

        /*@Override
        protected void onDraw(Canvas canvas) {

            Drawable drawable = getDrawable();

            if (drawable == null) {
                return;
            }

            if (getWidth() == 0 || getHeight() == 0) {
                return;
            }
            Bitmap b = ((BitmapDrawable) drawable).getBitmap();
            Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);

            int w = getWidth(), h = getHeight();

            Bitmap roundBitmap = getCroppedBitmap(bitmap,100);
            canvas.drawBitmap(roundBitmap, 0, 0, null);

        }

        public static Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
            Bitmap sbmp;

            if (bmp.getWidth() != radius || bmp.getHeight() != radius) {
                float smallest = Math.min(bmp.getWidth(), bmp.getHeight());
                float factor = smallest / radius;
                sbmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() / factor), (int)(bmp.getHeight() / factor), false);
            } else {
                sbmp = bmp;
            }

            Bitmap output = Bitmap.createBitmap(radius, radius,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xffa19774;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, radius, radius);

            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.parseColor("#BAB399"));
            canvas.drawCircle(radius / 2 + 0.7f,
                    radius / 2 + 0.7f, radius / 2 + 0.1f, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(sbmp, rect, rect, paint);

            return output;
        }*/
}
