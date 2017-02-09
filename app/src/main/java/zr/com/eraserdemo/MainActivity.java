package zr.com.eraserdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.netease.imageSelector.ImageSelector;

import java.util.ArrayList;

import zr.com.eraserdemo.util.ImageUtils;
import zr.com.eraserdemo.view.EraserView;
import zr.com.eraserdemo.view.info.Pen;

import static com.netease.imageSelector.ImageSelectorConstant.OUTPUT_LIST;
import static com.netease.imageSelector.ImageSelectorConstant.REQUEST_IMAGE;
import static com.netease.imageSelector.ImageSelectorConstant.REQUEST_PREVIEW;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    /** 原图的路径 */
    private String mImagePath;
    /** 原图的bitmap对象 */
    private Bitmap mBitmap;

    /** 书写 */
    private Button mWriteButton;
    /** 橡皮擦 */
    private Button mEraserButton;
    /** 到本地获取一张图片 */
    private Button mGetImageButton;
    /** 移动图片 */
    private Button mMoveButton;

    /** 涂鸦板view容器 */
    private FrameLayout mViewContainer;
    /** 涂鸦板view */
    private EraserView mEraserView;
    /** 当前已经选择的图片列表 */
    ArrayList<String> mCurrentImageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        setListener();
    }

    private void initView(){
        mWriteButton = (Button)findViewById(R.id.btn_write);
        mEraserButton = (Button)findViewById(R.id.btn_eraser);
        mGetImageButton = (Button)findViewById(R.id.btn_get_image);
        mMoveButton = (Button)findViewById(R.id.btn_move);
        mViewContainer = (FrameLayout)findViewById(R.id.layout_view_container);
    }

    private void setListener(){
        mWriteButton.setOnClickListener(this);
        mEraserButton.setOnClickListener(this);
        mGetImageButton.setOnClickListener(this);
        mMoveButton.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        // 接收图片选择器返回结果，更新所选图片集合
        if (requestCode == REQUEST_PREVIEW || requestCode == REQUEST_IMAGE) {
            ArrayList<String> newFiles = data.getStringArrayListExtra(OUTPUT_LIST);
            if (newFiles != null) {
                updateUI(newFiles);
            }
        }
    }

    /**
     * 取列表中第一张图进行涂鸦
     * @param newFiles 返回的图片列表
     */
    private void updateUI(ArrayList<String> newFiles) {
        mImagePath = newFiles.get(0).toString();

        mBitmap = ImageUtils.createBitmapFromPath(mImagePath, this);

        mEraserView = new EraserView(this, mBitmap, new EraserView.GraffitiListener() {
            @Override
            public void onSaved(Bitmap bitmap, Bitmap bitmapEraser) {

            }
        });

        mViewContainer.addView(mEraserView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_write:  // 书写
                mEraserView.setMoving(false);
                mEraserView.setPen(Pen.HAND);
                mEraserView.setPaintSize(10);
                break;

            case R.id.btn_eraser: // 橡皮擦
                mEraserView.setMoving(false);
                mEraserView.setPen(Pen.ERASER);
                mEraserView.setPaintSize(30);
                break;

            case R.id.btn_move:  // 移动
                mEraserView.setMoving(true);
                break;

            case R.id.btn_get_image: // 选取图片
                ImageSelector.getInstance().launchSelector(this, mCurrentImageList);
                break;
        }
    }
}
