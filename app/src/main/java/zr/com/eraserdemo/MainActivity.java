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

    private Button mWriteButton;
    private Button mEraserButton;
    private Button mGetImageButton;

    private FrameLayout mViewContainer;

    private EraserView mEraserView;

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
        mViewContainer = (FrameLayout)findViewById(R.id.layout_view_container);
    }

    private void setListener(){
        mWriteButton.setOnClickListener(this);
        mEraserButton.setOnClickListener(this);
        mGetImageButton.setOnClickListener(this);
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

    private void updateUI(ArrayList<String> newFiles) {
        mImagePath = newFiles.get(0).toString();

        mBitmap = ImageUtils.createBitmapFromPath(mImagePath, this);

        mEraserView = new EraserView(this, mBitmap, new EraserView.GraffitiListener() {
            @Override
            public void onSaved(Bitmap bitmap, Bitmap bitmapEraser) {

            }

            @Override
            public void onError(int i, String msg) {

            }

            @Override
            public void onReady() {

            }
        });

        mViewContainer.addView(mEraserView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_write:
                mEraserView.setPen(Pen.HAND);
                break;

            case R.id.btn_eraser:
                mEraserView.setPen(Pen.ERASER);
                break;

            case R.id.btn_get_image:
                ImageSelector.getInstance().launchSelector(this, mCurrentImageList);
                break;
        }
    }
}
