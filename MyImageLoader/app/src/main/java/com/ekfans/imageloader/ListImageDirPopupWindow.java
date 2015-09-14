package com.ekfans.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ekfans.imageloader.model.FolderModel;
import com.ekfans.imageloader.utils.ImageLoader;

import java.util.List;

/**
 * Created by ekfans-com on 2015/9/9.
 */

public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderModel> mDatas;
    private Context context;

    public interface OnDirSelectedListener{
        void onSelected(FolderModel folderModel);
    }

    public OnDirSelectedListener onDirSelectedListener;

    public ListImageDirPopupWindow(Context context,List<FolderModel> mDatas){
        calWidthAndHeight(context);
        this.context = context;
        this.mDatas = mDatas;
        mConvertView = LayoutInflater.from(context).inflate(R.layout.layout_popup,null);
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView();
        initEvent();
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (onDirSelectedListener != null){
                    onDirSelectedListener.onSelected(mDatas.get(position));
                }
            }
        });

    }

    private void initView() {
        mListView = (ListView) mConvertView.findViewById(R.id.list_dir);
        mListView.setAdapter(new ListDirAdapter(context,0,mDatas));
    }

    /**
     * 计算popupWindows的高宽
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderModel>{

        private LayoutInflater mInfolater;

        private List<FolderModel> mDatas;

        public ListDirAdapter(Context context, int resource, List<FolderModel> objects) {
            super(context, resource, objects);
            mInfolater = LayoutInflater.from(context);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            if (convertView == null){
                holder = new ViewHolder();
                convertView = mInfolater.inflate(R.layout.item_listview, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.item_image_dir);
                holder.count = (TextView) convertView.findViewById(R.id.item_dir_count);
                holder.name = (TextView) convertView.findViewById(R.id.item_dir_name);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mImg.setImageResource(R.mipmap.pic_thumb);
            FolderModel model = getItem(position);
            ImageLoader.getmInstance().loadImage(model.getFirstImagePath(),holder.mImg);
            holder.count.setText(model.getCount() + "张");
            holder.name.setText(model.getName());
            return convertView;
        }

        private class ViewHolder{
            ImageView mImg;
            TextView name;
            TextView count;
        }
    }

    public OnDirSelectedListener getOnDirSelectedListener() {
        return onDirSelectedListener;
    }

    public void setOnDirSelectedListener(OnDirSelectedListener onDirSelectedListener) {
        this.onDirSelectedListener = onDirSelectedListener;
    }
}
