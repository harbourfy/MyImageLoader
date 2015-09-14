package com.ekfans.imageloader;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.ekfans.imageloader.utils.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ekfans-com on 2015/9/9.
 */
public class ImageAdapter extends BaseAdapter {

    private static Set<String> mSelectedImg = new HashSet<>();

    private Context context;
    private List<String> mDatas;
    private String dirPath;
    public ImageAdapter(Context context,List<String> mDatas,String dirPath){
        this.context = context;
        this.mDatas = mDatas;
        this.dirPath = dirPath;
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder =  null;
        if (convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_gridview,parent,false);
            viewHolder.pic = (ImageView) convertView.findViewById(R.id.item_image);
            viewHolder.select = (ImageButton) convertView.findViewById(R.id.item_select);
            convertView.setTag(viewHolder);
        }else viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.pic.setImageResource(R.mipmap.pic_thumb);
        viewHolder.select.setImageResource(R.mipmap.ic_check_box_disselected);
        viewHolder.pic.setColorFilter(null);
        ImageLoader.getmInstance(3, ImageLoader.Type.LIFO).loadImage(dirPath + "/" + mDatas.get(position), viewHolder.pic);
        viewHolder.pic.setOnClickListener(new PicOnClickListener(position,viewHolder));
        final String filePath =  dirPath + "/" + mDatas.get(position);
        if (mSelectedImg.contains(filePath)){
            viewHolder.pic.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.select.setImageResource(R.mipmap.ic_check_box_selected);
        }
        return convertView;
    }

    static class ViewHolder{
        ImageView pic;
        ImageButton select;
    }

    private class PicOnClickListener implements View.OnClickListener{

        private int position;

        private ViewHolder viewHolder;

        public PicOnClickListener(int position,ViewHolder viewHolder){
            this.position = position;
            this.viewHolder = viewHolder;
        }

        @Override
        public void onClick(View v) {
            String filePath = dirPath + "/" + mDatas.get(position);
            //选中状态
            if (mSelectedImg.contains(filePath)){
                mSelectedImg.remove(filePath);
                viewHolder.pic.setColorFilter(null);
                viewHolder.select.setImageResource(R.mipmap.ic_check_box_disselected);
            }else {
                mSelectedImg.add(filePath);
                viewHolder.pic.setColorFilter(Color.parseColor("#77000000"));
                viewHolder.select.setImageResource(R.mipmap.ic_check_box_selected);
            }
//            notifyDataSetChanged();
        }
    }
}
