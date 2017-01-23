package zr.com.eraserdemo.util;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;


public class ImageUtils {


    public static final Bitmap createBitmapFromPath(String path, Context context) {
        WindowManager manager = (WindowManager)context.getSystemService("window");
        Display display = manager.getDefaultDisplay();
        int screenW = display.getWidth();
        int screenH = display.getHeight();
        return createBitmapFromPath(path, context, screenW, screenH);
    }

    public static final Bitmap createBitmapFromPath(String path, Context context, int maxResolutionX, int maxResolutionY) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = null;
        if(path.endsWith(".3gp")) {
            return ThumbnailUtils.createVideoThumbnail(path, 1);
        } else {
            try {
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int e = options.outWidth;
                int height = options.outHeight;
                options.inSampleSize = computeBitmapSimple(e * height, maxResolutionX * maxResolutionY);
                options.inPurgeable = true;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                options.inDither = false;
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(path, options);
                return rotateBitmapByExif(bitmap, path, true);
            } catch (OutOfMemoryError var8) {
                options.inSampleSize *= 2;
                bitmap = BitmapFactory.decodeFile(path, options);
                return rotateBitmapByExif(bitmap, path, true);
            } catch (Exception var9) {
                var9.printStackTrace();
                return null;
            }
        }
    }

    public static int computeBitmapSimple(int realPixels, int maxPixels) {
        try {
            if(realPixels <= maxPixels) {
                return 1;
            } else {
                int e;
                for(e = 2; realPixels / (e * e) > maxPixels; e *= 2) {
                    ;
                }

                return e;
            }
        } catch (Exception var3) {
            return 1;
        }
    }

    public static Bitmap rotateBitmapByExif(Bitmap bitmap, String path, boolean isRecycle) {
        int digree = getBitmapExifRotate(path);
        if(digree != 0) {
            bitmap = rotate((Context)null, bitmap, digree, isRecycle);
        }

        return bitmap;
    }

    public static int getBitmapExifRotate(String path) {
        short digree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(path);
        } catch (IOException var4) {
            var4.printStackTrace();
            return 0;
        }

        if(exif != null) {
            int ori = exif.getAttributeInt("Orientation", 0);
            switch(ori) {
                case 3:
                    digree = 180;
                    break;
                case 6:
                    digree = 90;
                    break;
                case 8:
                    digree = 270;
                    break;
                default:
                    digree = 0;
            }
        }

        return digree;
    }

    public static Bitmap rotate(Context context, Bitmap bitmap, int degree, boolean isRecycle) {
        Matrix m = new Matrix();
        m.setRotate((float)degree, (float)bitmap.getWidth() / 2.0F, (float)bitmap.getHeight() / 2.0F);

        try {
            Bitmap ex = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if(isRecycle) {
                bitmap.recycle();
            }

            return ex;
        } catch (OutOfMemoryError var6) {
            var6.printStackTrace();
            return null;
        }
    }


}

