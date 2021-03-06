package zr.com.eraserdemo.view.info;

import android.graphics.Matrix;
import android.graphics.Path;

/**
 * @author hzzhengrui
 * @Date 17/1/17
 * @Description
 */
public class GraffitiPath {
    public Pen mPen; // 画笔类型
    public Shape mShape; // 画笔形状
    public float mStrokeWidth; // 大小
    public GraffitiColor mColor; // 颜色
    public Path mPath; // 画笔的路径
    public float mSx, mSy; // 映射后的起始坐标，（手指点击）
    public float mDx, mDy; // 映射后的终止坐标，（手指抬起）
    public Matrix mMatrix; //　仿制图片的偏移矩阵
    public int degree; // path对应canvas的旋转角度

    public static GraffitiPath toShape(Pen pen, Shape shape, float width, GraffitiColor color,
                                       float sx, float sy, float dx, float dy, Matrix matrix, int degree) {
        GraffitiPath path = new GraffitiPath();
        path.mPen = pen;
        path.mShape = shape;
        path.mStrokeWidth = width;
        path.mColor = color;
        path.mSx = sx;
        path.mSy = sy;
        path.mDx = dx;
        path.mDy = dy;
        path.mMatrix = matrix;
        path.degree = degree;
        return path;
    }

    public static GraffitiPath toPath(Pen pen, Shape shape, float width, GraffitiColor color, Path p, Matrix matrix, int degree) {
        GraffitiPath path = new GraffitiPath();
        path.mPen = pen;
        path.mShape = shape;
        path.mStrokeWidth = width;
        path.mColor = color;
        path.mPath = p;
        path.mMatrix = matrix;
        path.degree = degree;
        return path;
    }
}
