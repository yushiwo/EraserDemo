package zr.com.eraserdemo;

import android.app.Application;

import com.netease.imageSelector.ImageSelector;
import com.netease.imageSelector.ImageSelectorConfiguration;

/**
 * @author hzzhengrui
 * @Date 17/1/20
 * @Description
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 获取默认配置
        ImageSelectorConfiguration configuration = ImageSelectorConfiguration.createDefault(this);

        // 自定义图片选择器
//        ImageSelectorConfiguration configuration = new ImageSelectorConfiguration.Builder(this)
//                .setMaxSelectNum(9)
//                .setSpanCount(4)
//                .setSelectMode(ImageSelectorConstant.MODE_MULTIPLE)
//                .setTitleHeight(48)
//                .build();

        ImageSelector.getInstance().init(configuration);

    }
}
