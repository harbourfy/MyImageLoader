package com.ekfans.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ekfans.imageloader.model.FolderModel;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;

    private RelativeLayout bottomLayout;

    private TextView mDirName;

    private TextView mDirCount;

    private List<String> mImgs;

    private File mCurrentDir;

    private int mMaxCount;

    private List<FolderModel> folderModels = new ArrayList<>();

    private ProgressDialog loadingDialog;

    private static final int DATA_LOADED = 0x100;

    private ImageAdapter mImageAdapter;

    private ListImageDirPopupWindow mDirPopupWindow;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            loadingDialog.dismiss();
            if(msg.what == 0x100){

                data2View();

                initDirPopupWindow();
            }
        }
    };

    private void initDirPopupWindow() {
        mDirPopupWindow = new ListImageDirPopupWindow(this,folderModels);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mDirPopupWindow.setOnDirSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderModel folderModel) {
                mCurrentDir = new File(folderModel.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith("jpeg") || filename.endsWith("png"))
                            return true;
                        return false;
                    }
                }));

                mImageAdapter = new ImageAdapter(MainActivity.this,mImgs,mCurrentDir.getAbsolutePath());
                gridView.setAdapter(mImageAdapter);
                mDirCount.setText(mImgs.size() + "张");
                mDirName.setText(folderModel.getName());
                mDirPopupWindow.dismiss();
            }
        });
    }

    /**
     * 区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    protected void data2View() {
        if (mCurrentDir == null){
            Toast.makeText(this,"未扫描到图片",Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImageAdapter = new ImageAdapter(this,mImgs,mCurrentDir.getAbsolutePath());
        gridView.setAdapter(mImageAdapter);
        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("正在加载...");
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
        initView();
        initData();
        initEvent();
    }

    private void initEvent() {
        bottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                mDirPopupWindow.showAsDropDown(bottomLayout, 0, 0);
                lightOff();
            }
        });

    }

    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 利用contentProvider扫描手机中所有的图片
     */
    private void initData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"当前存储卡不可用！",Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                Uri imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(imgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + " = ?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
                Set<String> mDirPaths = new HashSet<>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();
                    FolderModel folderModel = null;
                    if (mDirPaths.contains(dirPath)){
                        continue;
                    }else {
                        mDirPaths.add(dirPath);
                        folderModel = new FolderModel();
                        folderModel.setDir(dirPath);
                        folderModel.setFirstImagePath(path);

                    }
                    if (parentFile.list() == null){
                        continue;
                    }
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg") || filename.endsWith("jpeg") || filename.endsWith("png"))
                            return true;
                            return false;
                        }
                    }).length;
                    folderModel.setCount(picSize);
                    folderModels.add(folderModel);
                    if (picSize > mMaxCount){
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                //扫描完成
                cursor.close();
                //通知handler扫描图片完成
                mHandler.sendEmptyMessage(0x100);
            }
        }.start();
    }

    private void initView() {
        gridView = (GridView) findViewById(R.id.gridview);
        bottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        mDirName = (TextView) findViewById(R.id.file_name);
        mDirCount = (TextView) findViewById(R.id.file_num);
    }


}
