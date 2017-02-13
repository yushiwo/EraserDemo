package zr.com.eraserdemo.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.concurrent.CopyOnWriteArrayList;

import zr.com.eraserdemo.util.ImageUtils;
import zr.com.eraserdemo.view.info.GraffitiColor;
import zr.com.eraserdemo.view.info.GraffitiPath;
import zr.com.eraserdemo.view.info.Pen;
import zr.com.eraserdemo.view.info.Shape;


/**
 * modified by zhengrui
 * 作者文章:http://cdnnn.07net01.com/program/2016/10/1689006.html
 */
public class EraserView extends View {


    private static final String TAG = EraserView.class.getSimpleName();

    private static final float VALUE = 1f;

    /** 涂鸦板接口回调 */
    private GraffitiListener mGraffitiListener;

    /** 原图 */
    private Bitmap mBitmap, mOriginBitmap;
    /** 橡皮擦底图(一般都是原图) */
    private Bitmap mBitmapEraser;
    /** 原图 + 绘制path的合成图 */
    private Bitmap mGraffitiBitmap;
    /** 图片的Canvas,用户涂鸦绘制 */
    private Canvas mBitmapCanvas;

    /** 图片适应屏幕时的缩放倍数 */
    private float mPrivateScale;
    /** 图片适应屏幕时的大小（肉眼看到的在屏幕上的大小）*/
    private int mPrivateHeight, mPrivateWidth;
    /** 图片居中时的偏移（肉眼看到的在屏幕上的偏移）*/
    private float mCentreTranX, mCentreTranY;

    /** 用于涂鸦的图片上 */
    private BitmapShader mBitmapShader;
    /** 涂鸦的画布 */
    private BitmapShader mBitmapShader4C;
    /** 橡皮擦底图(一般都是用于涂鸦的原图) */
    private BitmapShader mBitmapShaderEraser;
    /** 橡皮擦的画布 */
    private BitmapShader mBitmapShaderEraser4C;
    /** 当前手写的路径,只记录一笔(相对于图片参考系的路径) */
    private Path mCurrPath;
    /** 当前手写的路径,貌似也是记录一笔(相对于view的画布的路径) */
    private Path mCanvasPath;
    /** 当前手写的路径,貌似也是记录一笔(仅单击时也能涂鸦) */
    private Path mTempPath;

    /** 画笔 */
    private Paint mPaint;
    /** 触摸模式，用于判断单点或多点触摸 */
    private int mTouchMode;
    /** 画笔粗细 */
    private float mPaintSize;
    /** 画笔底色 */
    private GraffitiColor mColor;
    /** 缩放倍数, 图片真实的缩放倍数为 mPrivateScale * mScale */
    private float mScale;
    /** 偏移量，图片真实偏移量为　mCentreTranX + mTransX */
    private float mTransX = 0, mTransY = 0;
    /** 是否正在绘制 */
    private boolean mIsPainting = false;
    /** 是否只绘制原图 */
    private boolean isJustDrawOriginal;

    /** 触摸时，图片区域外是否绘制涂鸦轨迹 */
    private boolean mIsDrawableOutside = false;
    /** 橡皮擦底图是否调整大小，如果可以则调整到跟当前涂鸦图片一样的大小 */
    private boolean mEraserImageIsResizeable;

    /** 保存涂鸦操作，便于撤销 */
    private CopyOnWriteArrayList<GraffitiPath> mPathStack = new CopyOnWriteArrayList<GraffitiPath>();
    /** 撤销时候缓存撤销步骤，反向撤销数据从此获取 */
    private CopyOnWriteArrayList<GraffitiPath> mUndoCacheStack = new CopyOnWriteArrayList<GraffitiPath>();

    /** 笔的类型:手写 or 橡皮擦 */
    private Pen mPen;
    /** 绘制方式 */
    private Shape mShape;

    private float mTouchDownX, mTouchDownY, mLastTouchX, mLastTouchY, mTouchX, mTouchY;
    private Matrix mShaderMatrix;
    private Matrix mShaderMatrix4C;
    private Matrix mMatrixTemp;

    /** 默认图片角度为0 */
    private int currentDegree = 0;

    private Context context;


    public EraserView(Context context, Bitmap bitmap, GraffitiListener listener) {
        this(context, bitmap, true, listener);
    }

    /**
     * @param context
     * @param bitmap
     * @param eraser                  橡皮擦的底图，如果涂鸦保存后再次涂鸦，传入涂鸦前的底图，则可以实现擦除涂鸦的效果．
     * @param eraserImageIsResizeable 橡皮擦底图是否调整大小，如果可以则调整到跟当前涂鸦图片一样的大小．
     * @param listener
     * @
     */
    public EraserView(Context context, Bitmap bitmap, boolean eraserImageIsResizeable, GraffitiListener listener) {
        super(context);

        this.context = context;

        // 关闭硬件加速，因为bitmap的Canvas不支持硬件加速
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mBitmap = bitmap;
        mOriginBitmap = bitmap;
        mGraffitiListener = listener;
        if (mGraffitiListener == null) {
            throw new RuntimeException("GraffitiListener is null!!!");
        }
        if (mBitmap == null) {
            throw new RuntimeException("Bitmap is null!!!");
        }

        mEraserImageIsResizeable = eraserImageIsResizeable;
        // 判断移动的最小距离
        mTouchSlop = ViewConfiguration.get(context.getApplicationContext()).getScaledTouchSlop();

        init();

    }

    public void init() {

        mScale = 1f;
        mColor = new GraffitiColor(Color.RED);
        mPaintSize = 10;
        mPaint = new Paint();
        mPaint.setStrokeWidth(mPaintSize);
        mPaint.setColor(mColor.getColor());
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);// 圆滑
        // 默认为涂鸦状态
        mPen = Pen.HAND;
        // 默认为手写方式
        mShape = Shape.HAND_WRITE;

        this.mBitmapShader = new BitmapShader(this.mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        this.mBitmapShader4C = new BitmapShader(this.mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        // 若指定橡皮擦底图,则使用之;否则底图和原图为同一张
        if (mBitmapEraser != null) {
            this.mBitmapShaderEraser = new BitmapShader(this.mBitmapEraser, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mBitmapShaderEraser4C = new BitmapShader(this.mBitmapEraser, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            this.mBitmapShaderEraser = mBitmapShader;
            this.mBitmapShaderEraser4C = mBitmapShader4C;
        }

        mShaderMatrix = new Matrix();
        mShaderMatrix4C = new Matrix();
        mMatrixTemp = new Matrix();
        mCanvasPath = new Path();
        mTempPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setBG();
    }

    /**
     * 计算两指间的距离
     *
     * @param event
     * @return
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }



    /** 手势操作相关 */
    private float mOldScale, mOldDist, mNewDist;
    /** 最大缩放倍数 */
    private final float mMaxScale = 5.0f;
    /** 最小缩放倍数 */
    private final float mMinScale = 1.0f;
    /** 判断为移动的最小距离 */
    private int mTouchSlop;
    /** 双指点击在涂鸦图片上的中点 */
    private float mToucheCentreXOnGraffiti, mToucheCentreYOnGraffiti;
    /** 双指点击在屏幕的中点 */
    private float mTouchCentreX, mTouchCentreY;
    /** 当前是否拖动图片标志位 */
    private boolean isMoving = false;
    /** 避免双指滑动，手指抬起时处理单指事件 */
    boolean mIsBusy = false;

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 开始操作画布，则清空撤销缓存，下次重新计数
        mUndoCacheStack.clear();
        if (isMoving()) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mTouchMode = 1;
                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTouchMode = 0;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mTouchMode < 2) { // 单点滑动
                        if (mIsBusy) { // 从多点触摸变为单点触摸，忽略该次事件，避免从双指缩放变为单指移动时图片瞬间移动
                            mIsBusy = false;
                            mLastTouchX = event.getX();
                            mLastTouchY = event.getY();
                            return true;
                        }
                        float tranX = event.getX() - mLastTouchX;
                        float tranY = event.getY() - mLastTouchY;
                        setTrans(getTransX() + tranX, getTransY() + tranY);
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                    } else { // 多点
                        mNewDist = spacing(event);// 两点滑动时的距离
                        if (Math.abs(mNewDist - mOldDist) >= mTouchSlop) {
                            float scale = mNewDist / mOldDist;
                            mScale = mOldScale * scale;

                            if (mScale > mMaxScale) {
                                mScale = mMaxScale;
                            }
                            if (mScale < mMinScale) { // 最小倍数
                                mScale = mMinScale;
                            }
                            // 围绕坐标(0,0)缩放图片
                            setScale(mScale);
                            // 缩放后，偏移图片，以产生围绕某个点缩放的效果
                            float transX = toTransX(mTouchCentreX, mToucheCentreXOnGraffiti);
                            float transY = toTransY(mTouchCentreY, mToucheCentreYOnGraffiti);
                            setTrans(transX, transY);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    mTouchMode -= 1;
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mTouchMode += 1;
                    mOldScale = getScale();
                    mOldDist = spacing(event);// 两点按下时的距离
                    mTouchCentreX = (event.getX(0) + event.getX(1)) / 2;// 不用减trans
                    mTouchCentreY = (event.getY(0) + event.getY(1)) / 2;
                    mToucheCentreXOnGraffiti = toX(mTouchCentreX);
                    mToucheCentreYOnGraffiti = toY(mTouchCentreY);
                    mIsBusy = true; // 标志位多点触摸
                    return true;
            }
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN");
                mTouchMode = 1;
                mTouchDownX = mTouchX = mLastTouchX = event.getX();
                mTouchDownY = mTouchY = mLastTouchY = event.getY();

                mCurrPath = new Path();
                mCurrPath.moveTo(toX(mTouchDownX), toY(mTouchDownY));
                mCanvasPath.reset();
                mCanvasPath.moveTo(toX4C(mTouchDownX), toY4C(mTouchDownY));
                mIsPainting = true;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchMode = 0;
                mLastTouchX = mTouchX;
                mLastTouchY = mTouchY;
                mTouchX = event.getX();
                mTouchY = event.getY();

                // 为了仅点击时也能出现绘图，必须移动path
                if (mTouchDownX == mTouchX && mTouchDownY == mTouchY & mTouchDownX == mLastTouchX && mTouchDownY == mLastTouchY) {
                    mTouchX += VALUE;
                    mTouchY += VALUE;
                }

                if (mIsPainting) {
                    GraffitiPath path = null;
                    mCurrPath.quadTo(
                            toX(mLastTouchX),
                            toY(mLastTouchY),
                            toX((mTouchX + mLastTouchX) / 2),
                            toY((mTouchY + mLastTouchY) / 2));
                    path = GraffitiPath.toPath(mPen, mShape, mPaintSize, mColor.copy(), mCurrPath, null, currentDegree);
                    mPathStack.add(path);
                    draw(mBitmapCanvas, path, false); // 保存到图片中
                    mIsPainting = false;  // 设置为false,将最后一笔保存在图片中,然后在ondraw中不去绘制
                }

                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchMode < 2) { // 单点滑动
                    Log.d(TAG, "ACTION_MOVE 1");
                    if (mIsBusy) { // 从多点触摸变为单点触摸，忽略该次事件，避免从双指缩放变为单指移动时图片瞬间移动
                        Log.d(TAG, "ACTION_MOVE 1 isbusy true");
                        mIsBusy = false;
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        return true;
                    }

                    mLastTouchX = mTouchX;
                    mLastTouchY = mTouchY;
                    mTouchX = event.getX();
                    mTouchY = event.getY();

                    mCurrPath.quadTo(
                            toX(mLastTouchX),
                            toY(mLastTouchY),
                            toX((mTouchX + mLastTouchX) / 2),
                            toY((mTouchY + mLastTouchY) / 2));
                    mCanvasPath.quadTo(
                            toX4C(mLastTouchX),
                            toY4C(mLastTouchY),
                            toX4C((mTouchX + mLastTouchX) / 2),
                            toY4C((mTouchY + mLastTouchY) / 2));
                } else { // 多点
                    Log.d(TAG, "ACTION_MOVE 2");
                    mIsPainting = false;

                    // 缩放
                    Log.d(TAG, "缩放");
                    mNewDist = spacing(event);// 两点滑动时的距离
                    if (Math.abs(mNewDist - mOldDist) >= mTouchSlop) {
                        float scale = mNewDist / mOldDist;
                        mScale = mOldScale * scale;

                        if (mScale > mMaxScale) {
                            mScale = mMaxScale;
                        }
                        if (mScale < mMinScale) { // 最小倍数
                            mScale = mMinScale;
                        }
                        // 围绕坐标(0,0)缩放图片
                        setScale(mScale);
                        // 缩放后，偏移图片，以产生围绕某个点缩放的效果
                        float transX = toTransX(mTouchCentreX, mToucheCentreXOnGraffiti);
                        float transY = toTransY(mTouchCentreY, mToucheCentreYOnGraffiti);
                        setTrans(transX, transY);
                    }

                    mLastTouchX = event.getX(0);
                    mLastTouchY = event.getY(0);
                }
                invalidate();

                return true;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchMode -= 1;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                mTouchMode += 1;

                mOldScale = getScale();
                mOldDist = spacing(event);// 两点按下时的距离
                mTouchCentreX = (event.getX(0) + event.getX(1)) / 2;// 不用减trans
                mTouchCentreY = (event.getY(0) + event.getY(1)) / 2;
                mToucheCentreXOnGraffiti = toX(mTouchCentreX);
                mToucheCentreYOnGraffiti = toY(mTouchCentreY);
                mIsBusy = true; // 标志位多点触摸
                return true;
        }
        return super.onTouchEvent(event);
    }


    private int w1;
    private int w2;
    private void setBG() {// 不用resize preview
        // 等比例缩放图片适应view的大小
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        if (nw > nh) {
            mPrivateScale = 1 / nw;
            mPrivateWidth = getWidth();
            mPrivateHeight = (int) (h * mPrivateScale);
        } else {
            mPrivateScale = 1 / nh;
            mPrivateWidth = (int) (w * mPrivateScale);
            mPrivateHeight = getHeight();
        }
        // 使图片居中
        mCentreTranX = (getWidth() - mPrivateWidth) / 2f;
        mCentreTranY = (getHeight() - mPrivateHeight) / 2f;

        initCanvas();
        setMatrix();

        if(currentDegree % 180 != 0){ // 横
            w2 = mPrivateWidth;
        }else {
            w1 = mPrivateWidth;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap.isRecycled() || mGraffitiBitmap.isRecycled()) {
            return;
        }
        // canvas增加抗锯齿效果,开启后很影响性能,慎用!!!
//        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));

        canvas.save();
        doDraw(canvas);
        canvas.restore();

    }

    /**
     * 开始绘制
     * @param canvas
     */
    private void doDraw(Canvas canvas) {
        System.out.println("do draw");
        canvas.scale(mPrivateScale * mScale, mPrivateScale * mScale); // 缩放画布，接下来的操作要进行坐标换算
        float left = (mCentreTranX + mTransX) / (mPrivateScale * mScale);
        float top = (mCentreTranY + mTransY) / (mPrivateScale * mScale);

        if (!mIsDrawableOutside) { // 裁剪绘制区域为图片区域
            canvas.clipRect(left, top, left + mBitmap.getWidth(), top + mBitmap.getHeight());
        }

        if (isJustDrawOriginal) { // 只绘制原图
            canvas.drawBitmap(mBitmap, left, top, null);
            return;
        }

        // 绘制历史涂鸦
        canvas.drawBitmap(mGraffitiBitmap, left, top, null);

        // 最新的一笔,先画在画布上,提笔的时候保存为新的mGraffitiBitmap
        if (mIsPainting) {  //画在view的画布上
            Path path;
            // 为了仅点击时也能出现绘图，必须移动path
            if (mTouchDownX == mTouchX && mTouchDownY == mTouchY && mTouchDownX == mLastTouchX && mTouchDownY == mLastTouchY) {
                mTempPath.reset();
                mTempPath.addPath(mCanvasPath);
                mTempPath.quadTo(
                        toX4C(mLastTouchX),
                        toY4C(mLastTouchY),
                        toX4C((mTouchX + mLastTouchX + VALUE) / 2),
                        toY4C((mTouchY + mLastTouchY + VALUE) / 2));
                path = mTempPath;
            } else {  // 是一个完整的笔画,则直接绘制,不需要偏移
                path = mCanvasPath;
            }
            // 画触摸的路径
            mPaint.setStrokeWidth(mPaintSize);
            if (mShape == Shape.HAND_WRITE) { // 手写,绘制到画布上,isCanvas为true
                draw(canvas, mPen, mPaint, path, null, true, mColor, currentDegree);
            }
        }

    }

    /**
     * 将所有路径绘制到图片上
     * @param canvas
     * @param pathStack
     * @param is4Canvas
     */
    private void draw(Canvas canvas, CopyOnWriteArrayList<GraffitiPath> pathStack, boolean is4Canvas) {
        // 还原堆栈中的记录的操作
        for (GraffitiPath path : pathStack) {
            draw(canvas, path, is4Canvas);
        }
    }

    /**
     * 手写,绘制在view的画布上
     * @param canvas
     * @param pen
     * @param paint
     * @param path
     * @param matrix
     * @param is4Canvas
     * @param color
     */
    private void draw(Canvas canvas, Pen pen, Paint paint, Path path, Matrix matrix, boolean is4Canvas, GraffitiColor color, int degree) {
        resetPaint(pen, paint, is4Canvas, matrix, color);
        paint.setStyle(Paint.Style.STROKE);
        canvas.save();
//        if(currentDegree == 90 || currentDegree == 270){  // 当前横屏
//            if(degree == 0 || degree == 180){
//                canvas.scale((float) w1/w2, (float) w1/w2);
//            }else {
//                canvas.scale(1, 1);
//            }
//        }else {
//            if(degree == 0 || degree == 180){
//                canvas.scale(1, 1);
//            }else {
//                canvas.scale((float) w2/w1, (float) w2/w1);
//            }
//        }

        canvas.rotate(currentDegree - degree, canvas.getWidth()/2.0f, canvas.getHeight()/2.0f);
        canvas.drawPath(path, paint);
        canvas.restore();
    }


    /**
     * 绘制路径到图片上(每笔抬起的时候,将绘制路径和底部一起绘制保存)
     * @param canvas
     * @param path
     * @param is4Canvas
     */
    private void draw(Canvas canvas, GraffitiPath path, boolean is4Canvas) {
        mPaint.setStrokeWidth(path.mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        if (path.mShape == Shape.HAND_WRITE) { // 手写,绘制到图片上,isCanvas为false
            draw(canvas, path.mPen, mPaint, path.mPath, path.mMatrix, is4Canvas, path.mColor, path.degree);
        }
    }

    /**
     * 设置画笔相关参数(绘制 or 橡皮擦)
     * @param pen
     * @param paint
     * @param is4Canvas
     * @param matrix
     * @param color
     */
    private void resetPaint(Pen pen, Paint paint, boolean is4Canvas, Matrix matrix, GraffitiColor color) {
        switch (pen) { // 设置画笔
            case HAND:
                paint.setShader(null);
//                if (is4Canvas) {  // 使用图片绘制
//                    System.out.println("111");
//                    color.initColor(paint, mShaderMatrix4C);
//                } else { // 普通绘制
//                    System.out.println("222");
//                    color.initColor(paint, null);
//                }

                // 直接调用此接口(是否使用bitmap作为画笔,在GraffitiColor中可通过PenType区分)
                color.initColor(paint, mShaderMatrix4C);
                break;

            case ERASER:
                if (is4Canvas) {  // 绘制到canvas
                    paint.setShader(this.mBitmapShaderEraser4C);
                } else {  // 绘制到原图片
                    if (mBitmapShader == mBitmapShaderEraser) { // 图片的矩阵不需要任何偏移(涂鸦图片和底图是同一张)
                        mBitmapShaderEraser.setLocalMatrix(null);
                    }
                    paint.setShader(this.mBitmapShaderEraser);
                }
                break;
        }
    }


    /**
     * 将屏幕触摸坐标x转换成在图片中的坐标 <br />
     * 图片实际便宜量:mCentreTranX + mTransX
     */
    public final float toX(float touchX) {
        return (touchX - mCentreTranX - mTransX) / (mPrivateScale * mScale);
    }

    /**
     * 将屏幕触摸坐标y转换成在图片中的坐标
     */
    public final float toY(float touchY) {
        return (touchY - mCentreTranY - mTransY) / (mPrivateScale * mScale);
    }

    /**
     * 坐标换算
     * （公式由toX()中的公式推算出）
     *
     * @param touchX    触摸坐标
     * @param graffitiX 在涂鸦图片中的坐标
     * @return 偏移量
     */
    public final float toTransX(float touchX, float graffitiX) {
        return -graffitiX * (mPrivateScale * mScale) + touchX - mCentreTranX;
    }

    public final float toTransY(float touchY, float graffitiY) {
        return -graffitiY * (mPrivateScale * mScale) + touchY - mCentreTranY;
    }

    /**
     * 将屏幕触摸坐标x转换成在canvas中的坐标(相对于屏幕的坐标)
     */
    public final float toX4C(float x) {
        return (x) / (mPrivateScale * mScale);
    }

    /**
     * 将屏幕触摸坐标y转换成在canvas中的坐标
     */
    public final float toY4C(float y) {
        return (y) / (mPrivateScale * mScale);
    }

    /**
     * 初始化画布
     */
    private void initCanvas() {
        if (mGraffitiBitmap != null) {
            mGraffitiBitmap.recycle();
        }
        mGraffitiBitmap = mBitmap.copy(Bitmap.Config.RGB_565, true);
        mBitmapCanvas = new Canvas(mGraffitiBitmap);
    }

    private void setMatrix() {
//        // 原图view保持不变,因此无需设置matrix
//        this.mShaderMatrix.set(null);
//        this.mBitmapShader.setLocalMatrix(this.mShaderMatrix);

        // 位置变动只是通过移动画布实现
        this.mShaderMatrix4C.set(null);
        this.mShaderMatrix4C.postTranslate((mCentreTranX + mTransX) / (mPrivateScale * mScale), (mCentreTranY + mTransY) / (mPrivateScale * mScale));
        this.mBitmapShader4C.setLocalMatrix(this.mShaderMatrix4C);

        // 如果使用了自定义的橡皮擦底图，则需要跳转矩阵
        if (mPen == Pen.ERASER && mBitmapShader != mBitmapShaderEraser) {
            mMatrixTemp.reset();
            // 缩放橡皮擦底图，使之与涂鸦图片大小一样
            if (mEraserImageIsResizeable) {
                mMatrixTemp.preScale(mBitmap.getWidth() * 1f / mBitmapEraser.getWidth(), mBitmap.getHeight() * 1f / mBitmapEraser.getHeight());
            }
            mBitmapShaderEraser.setLocalMatrix(mMatrixTemp);
            mBitmapShaderEraser4C.setLocalMatrix(mMatrixTemp);
        }
    }

    /**
     * 调整图片位置
     */
    private void judgePosition() {
        boolean changed = false;
        if (mPrivateWidth * mScale > getWidth()) { // 图片偏移的位置不能超过屏幕边缘
            if (mCentreTranX + mTransX > 0) {
                mTransX = -mCentreTranX;
                changed = true;
            } else if (mCentreTranX + mTransX + mPrivateWidth * mScale < getWidth()) {
                mTransX = getWidth() - mPrivateWidth * mScale - mCentreTranX;
                changed = true;
            }
        } else { // 图片只能在屏幕可见范围内移动
            if (mCentreTranX + mTransX + mBitmap.getWidth() * mPrivateScale * mScale > getWidth()) { // mScale<1是preview.width不用乘scale
                mTransX = getWidth() - mBitmap.getWidth() * mPrivateScale * mScale - mCentreTranX;
                changed = true;
            } else if (mCentreTranX + mTransX < 0) {  // 图片左边界只能到canvas的左边
                mTransX = -mCentreTranX;
                changed = true;
            }
        }

        if (mPrivateHeight * mScale > getHeight()) { // 图片偏移的位置不能超过屏幕边缘
            if (mCentreTranY + mTransY > 0) {
                mTransY = -mCentreTranY;
                changed = true;
            } else if (mCentreTranY + mTransY + mPrivateHeight * mScale < getHeight()) {
                mTransY = getHeight() - mPrivateHeight * mScale - mCentreTranY;
                changed = true;
            }
        } else { // 图片只能在屏幕可见范围内移动
            if (mCentreTranY + mTransY + mBitmap.getHeight() * mPrivateScale * mScale > getHeight()) {
                mTransY = getHeight() - mBitmap.getHeight() * mPrivateScale * mScale - mCentreTranY;
                changed = true;
            } else if (mCentreTranY + mTransY < 0) {
                mTransY = -mCentreTranY;
                changed = true;
            }
        }
        if (changed) {
            setMatrix();
        }
    }


    // ===================== api ==============

    /**
     * 保存
     */
    public void save() {
        mGraffitiListener.onSaved(mGraffitiBitmap);
    }

    /**
     * 清屏
     */
    public void clear() {
        mPathStack.clear();
        mUndoCacheStack.clear();
        initCanvas();
        invalidate();
    }

    /**
     * 撤销
     */
    public void undo() {
        if (mPathStack.size() > 0) {
            mUndoCacheStack.add(mPathStack.get(mPathStack.size() - 1));
            mPathStack.remove(mPathStack.size() - 1);
            initCanvas();
            draw(mBitmapCanvas, mPathStack, false);
            invalidate();
        }
    }

    /**
     * 反向撤销
     */
    public void reverse() {
        if (mUndoCacheStack.size() > 0) {
            mPathStack.add(mUndoCacheStack.get(mUndoCacheStack.size() - 1));
            mUndoCacheStack.remove(mUndoCacheStack.size() - 1);
            initCanvas();
            draw(mBitmapCanvas, mPathStack, false);
            invalidate();
        }
    }

    /**
     * 旋转
     */
    public void rotate() {
        currentDegree = (currentDegree + 90) % 360;

//        mBitmapEraser = ImageUtils.rotate(context, mOriginBitmap, currentDegree, false);
        // 将涂鸦后的图作为原图显示
        mBitmap = ImageUtils.rotate(context, mBitmap, 90, true);
        mGraffitiBitmap = ImageUtils.rotate(context, mGraffitiBitmap, 90, true);
        // 等比例缩放
        setBG();
        // 初始化
        init();
        // 生成一张新图，旋转之前的笔画不能清除
//        mPathStack.clear();
//        mUndoCacheStack.clear();
        // 刷新
        // 居中
        centrePic();
        // 绘制path
        draw(mBitmapCanvas, mPathStack, false);

    }

    /**
     * 是否有修改
     */
    public boolean isModified() {
        return mPathStack.size() != 0;
    }


    /**
     * 居中图片
     */
    public void centrePic() {
        mScale = 1;
        // 居中图片
        mTransX = 0;
        mTransY = 0;
        judgePosition();
        invalidate();
    }

    /**
     * 只绘制原图
     *
     * @param justDrawOriginal
     */
    public void setJustDrawOriginal(boolean justDrawOriginal) {
        isJustDrawOriginal = justDrawOriginal;
        invalidate();
    }

    public boolean isJustDrawOriginal() {
        return isJustDrawOriginal;
    }

    /**
     * 设置画笔底色
     *
     * @param color
     */
    public void setColor(int color) {
        mColor.setColor(color);
        invalidate();
    }



    public void setColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
        if (mBitmap == null) {
            return;
        }
        mColor.setColor(bitmap, tileX, tileY);
        invalidate();
    }


    /**
     * 缩放倍数，图片真实的缩放倍数为 mPrivateScale*mScale
     *
     * @param scale
     */
    public void setScale(float scale) {
        this.mScale = scale;
        judgePosition();
        setMatrix();
        invalidate();
    }

    public float getScale() {
        return mScale;
    }

    /**
     * 设置画笔
     *
     * @param pen
     */
    public void setPen(Pen pen) {
        if (pen == null) {
            throw new RuntimeException("Pen can't be null");
        }
        mPen = pen;
        setMatrix();
        invalidate();
    }

    public Pen getPen() {
        return mPen;
    }


    public void setTrans(float transX, float transY) {
        mTransX = transX;
        mTransY = transY;
        judgePosition();
        setMatrix();
        invalidate();
    }

    /**
     * 设置图片偏移
     *
     * @param transX
     */
    public void setTransX(float transX) {
        this.mTransX = transX;
        judgePosition();
        invalidate();
    }

    public float getTransX() {
        return mTransX;
    }

    public void setTransY(float transY) {
        this.mTransY = transY;
        judgePosition();
        invalidate();
    }

    public float getTransY() {
        return mTransY;
    }


    public void setPaintSize(float paintSize) {
        mPaintSize = paintSize;
        invalidate();
    }

    public float getPaintSize() {
        return mPaintSize;
    }

    /**
     * 触摸时，图片区域外是否绘制涂鸦轨迹
     *
     * @param isDrawableOutside
     */
    public void setIsDrawableOutside(boolean isDrawableOutside) {
        mIsDrawableOutside = isDrawableOutside;
    }

    /**
     * 触摸时，图片区域外是否绘制涂鸦轨迹
     */
    public boolean getIsDrawableOutside() {
        return mIsDrawableOutside;
    }



    public interface GraffitiListener {

        /**
         * 保存图片
         *
         * @param bitmap       涂鸦后的图片
         */
        void onSaved(Bitmap bitmap);
    }
}
