package com.ekfans.imageloader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by ekfans-com on 2015/9/8.
 */
public class ImageLoader {

    private static ImageLoader mInstance;
    /**
     * 图片缓存核心对象
     */
    private LruCache<String,Bitmap> mLruCache;
    /**
     * 图片就加载线程池
     */
    private ExecutorService mThreadPool;
    /**
     * 默认线程池数量
     */
    private static final int DEAFULT_THREAD_COUNT = 1;
    /**
     * 队列调度方式
     */
    private Type mType = Type.LIFO;
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的handler
     */
    private Handler mUIHandler;
    //同步mPoolThreadHandler
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    //是type有效的信号量
    private Semaphore mSemaphorehreadPool;
    //单例模式获取imageloader
    public static ImageLoader getmInstance(){
        if (mInstance == null){
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(DEAFULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    //单例模式获取imageloader
    public static ImageLoader getmInstance(int threadCount,Type type){
        if (mInstance == null){
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }

    private ImageLoader(int threadCount,Type type){
        init(threadCount,type);
    }

    public void init(int threadCount,Type type){
        //后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取一个任务执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphorehreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
        //获取应用最大使用类型
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;
        mSemaphorehreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     * @return
     */
    private Runnable getTask(){
        if (mType == Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if (mType == Type.LIFO){
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 根据path为imageview设置图片
     * 加载图片
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView){
        //设置tag
        imageView.setTag(path);
        if (mUIHandler == null){
            mUIHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片，为imageview回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageview;
                    String path = holder.path;
                    //比较imageview的tag和path，相等则加载图片
                    if (imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }
        //根据path在缓存中获取bitmap
        Bitmap bm = getBitMapFromLruCache(path);
        if (bm != null){
            refreshBitmap(path, bm, imageView);
        }else{
            addTasks(new Runnable(){
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    //1、获取图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path,imageSize.width,imageSize.height);
                    //把图片加入到缓存
                    addBitmapToLruCache(path, bm);
                    //执行回调
                    refreshBitmap(path,bm,imageView);
//                    bm.recycle();
                    mSemaphorehreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(String path,Bitmap bm,ImageView imageView){
        Message msg = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageview = imageView;
        msg.obj = holder;
        mUIHandler.sendMessage(msg);
    }

    /**
     * 将图片加入缓存
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitMapFromLruCache(path) == null){
            if (bm != null){
                mLruCache.put(path,bm);
            }
        }
    }

    /**
     * 根据图片需要显示的宽和高压缩图片
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //获取图片的宽高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = caculateInSampleSize(options,width,height);
        //使用或得到的inSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path,options);
        return bitmap;
    }

    /**
     * 根据需求的高和宽以及图片的实际高和宽计算SampleSize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqHeight || height > reqHeight){
            int widthRadio = Math.round(width*1.0f/reqWidth);
            int heightRadio = Math.round(height*1.0f/reqHeight);
            inSampleSize = Math.max(widthRadio,heightRadio);
        }

        return inSampleSize;
    }

    /**
     * 根据imageview获取适当的压缩的宽和高
     * @param imageView
     * @return
     */
    protected ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        int width = imageView.getWidth();//获取imageview的时间宽度
        if (width <= 0){
            width = lp.width;//获取imageview在layout中声明的宽度
        }
        if(width <= 0){
            width = getImageFildValue(imageView, "mMaxWidth");//检查最大值
        }
        if(width <= 0){
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();//获取imageview的实际高度
        if (height <= 0){
            height = lp.width;//获取imageview在layout中声明的宽度
        }
        if(height <= 0){
            height = getImageFildValue(imageView, "mMaxHeight");//检查最大值
        }
        if(height <= 0){
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height= height;
        return imageSize;
    }

    /**
     * 通过反射获取对象的某个属性值
     * @param object
     * @return
     */
    private static int getImageFildValue(Object object,String fieldName){
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private class ImageSize{
        int width;
        int height;
    }

    /**
     * mPoolThreadHandler是在线程里面初始化，防止多线程出问题，注意同步的使用
     * @param runnable
     */
    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        //发送通知，执行任务
        //防止并发引起空指针
        try {
            if(mPoolThreadHandler == null)
                mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x100);
    }

    private Bitmap getBitMapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private class ImageBeanHolder{
        Bitmap bitmap;
        ImageView imageview;
        String path;
    }

    public enum Type{
        FIFO,LIFO;
    }
}
