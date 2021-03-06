package zr.com.eraserdemo.view.info;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * @author hzzhengrui
 * @Date 17/1/17
 * @Description 涂鸦底色,颜色 or 图片
 */
public class GraffitiColor {
    public enum Type {
        COLOR, // 颜色值
        BITMAP // 图片
    }

    public int mColor;
    public Bitmap mBitmap;
    public GraffitiColor.Type mType;
    public Shader.TileMode mTileX = Shader.TileMode.MIRROR;
    public Shader.TileMode mTileY = Shader.TileMode.MIRROR;  // 镜像

    public GraffitiColor(int color) {
        mType = GraffitiColor.Type.COLOR;
        mColor = color;
    }

    public GraffitiColor(Bitmap bitmap) {
        mType = GraffitiColor.Type.BITMAP;
        mBitmap = bitmap;
    }

    public GraffitiColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
        mType = GraffitiColor.Type.BITMAP;
        mBitmap = bitmap;
        mTileX = tileX;
        mTileY = tileY;
    }

    /**
     * 初始化画笔的颜色(手写 or 图片涂鸦)
     * @param paint 画笔
     * @param matrix 矩阵
     */
    public void initColor(Paint paint, Matrix matrix) {
        if (mType == GraffitiColor.Type.COLOR) {
            System.out.println("init color 111");
            paint.setColor(mColor);
        } else if (mType == GraffitiColor.Type.BITMAP) {
            System.out.println("init color 222");
            BitmapShader shader = new BitmapShader(mBitmap, mTileX, mTileY);
            shader.setLocalMatrix(matrix);
            paint.setShader(shader);
        }
    }

    public void setColor(int color) {
        mType = GraffitiColor.Type.COLOR;
        mColor = color;
    }

    public void setColor(Bitmap bitmap) {
        mType = GraffitiColor.Type.BITMAP;
        mBitmap = bitmap;
    }

    public void setColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
        mType = GraffitiColor.Type.BITMAP;
        mBitmap = bitmap;
        mTileX = tileX;
        mTileY = tileY;
    }

    public int getColor() {
        return mColor;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public GraffitiColor.Type getType() {
        return mType;
    }

    public GraffitiColor copy() {
        GraffitiColor color = null;
        if (mType == GraffitiColor.Type.COLOR) {
            color = new GraffitiColor(mColor);
        } else {
            color = new GraffitiColor(mBitmap);
        }
        color.mTileX = mTileX;
        color.mTileY = mTileY;
        return color;
    }
}
