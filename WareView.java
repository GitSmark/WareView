package hxy.ttt.com.wareview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by huangxy on 2016/3/19.
 */
public class WareView extends ViewGroup {

    private Context context;
    private WareIView wareIView;
    private String[] ImageUrls = new String[]{ };

    private SeismicWaveView wareView;
    private boolean isStarting = false;
    private Paint paint;

    private int WareColor = 0x33ccff; // 默认颜色
    private int WareImage = R.mipmap.ic_launcher;// 默认图片

    private ImageView[] views = new ImageView[15];// 设置最多View个数，实测一般不超过15个，超过12个可能陷入死循环，造成应用卡死
    private int NLength = 7;// 最多同时显示个数，默认为7，建议范围4-10
    private int SLength = 0;// 当前显示个数，默认为0；
    private int NextUrl = 0;// 下一个添加View对应Url位置，默认第一个
    private int NextAdd = 0;// 下一个添加View，默认第一个
    private int NextRmv = 0;// 下一个移除View，默认第一个
    private Boolean IsLoop = true; // 设置是否循环, 默认是

    private long AddTime = 2*1000;// 添加View间隔，默认2*1000ms
    private long RmvTime = 685; // 移除View间隔，默认685ms(大于AddTime/4，且略小于AddTime/3)

    private final AlphaAnimation animationIn = new AlphaAnimation(0, 1);
    private final AlphaAnimation animationOut = new AlphaAnimation(1, 0);

    private Random random =  new Random();
    private int WareRadius = 30;// 默认views半径,单位dip(下同)
    private int WareBevels = 40;// 默认views对角线半径，sqrt(60*60*2)约值85/2
    private int WareIView = 80; // 默认中间区域半径(60+20),ImageView默认半径60，边距半径20
    private int WareView = 200; // 默认WareView区域半径
    private int WareArea = 160; // 默认最小可加区域半径(200-40)
    private int WareMargin = 60;// 默认最小可加区域内XY边距半径(160-100)

    private float WareMarginX = 0.f;// 中心点X
    private float WareMarginY = 0.f;// 中心点Y

    private WareViewListener listener = null;

    public WareView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WareView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WareView);
        WareImage = typedArray.getResourceId(R.styleable.WareView_WareImage, WareImage);
        WareColor = typedArray.getResourceId(R.styleable.WareView_WareColor, WareColor);
        init();
    }

    public WareView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(0, 0);
            if(getChildAt(i) instanceof SeismicWaveView){
                getChildAt(i).layout(0, 0,(int)WareMarginX*2,(int)WareMarginY*2);
            }else if(getChildAt(i) instanceof WareIView){
                getChildAt(i).layout(0,0,WareBevels*2,WareBevels*2);
            }else{
                getChildAt(i).layout(0,0,WareRadius*2,WareRadius*2);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init() {

        try {
            listener = (WareViewListener) context;
        }catch (Exception e){

        }

        animationIn.setDuration(1000);
        animationOut.setDuration(1000);

        WareRadius = dip2px(context, WareRadius);
        WareBevels = dip2px(context, WareBevels);
        WareIView = dip2px(context, WareIView);

        WareMarginY = getDisplayHeightPxWithoutBar(context)/2;
        WareMarginX = getDisplayWidthPx(context)/2;

        int WareMargins = (int)WareMarginX;
        int WareWidth = dip2px(context, WareView);
        WareArea = ((WareMargins>WareWidth)?WareWidth:WareMargins) - WareRadius;
        WareMargin = (WareArea - WareIView);

        wareView = new SeismicWaveView(context);
        this.addView(wareView);

        wareIView = new WareIView(context);
        wareIView.setImageResource(WareImage);
        wareIView.setX((float)WareMarginX - WareBevels);
        wareIView.setY((float)WareMarginY - WareBevels);
        this.addView(wareIView);

        for (int i = 0; i < views.length; i++) {
            views[i] = new ImageView(context);
            //views[i].setLayoutParams(new LinearLayout.LayoutParams(WareRadius*2, WareRadius*2));
            this.addView(views[i]);
        }

        setNLength();// 设置数据和IsLoop值后需重新赋值
    }

    private class WareIView extends ImageView {

        public WareIView(Context context) {
            super(context);
        }
    }

    private class SeismicWaveView extends View{

        private int maxWidth = 255;
        private List<String> alphaList = new ArrayList<String>();
        private List<String> startWidthList = new ArrayList<String>();

        public SeismicWaveView(Context context) {
            super(context);
            initPaint();
        }

        public SeismicWaveView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initPaint();
        }

        public SeismicWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initPaint();
        }

        private void initPaint(){
            paint = new Paint();
            paint.setColor(WareColor);//默认水波颜色
            alphaList.add("255");//圆心的不透明度
            startWidthList.add("0");
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            setBackgroundColor(Color.TRANSPARENT);//颜色：完全透明
            //依次绘制 同心圆
            for (int i = 0; i < alphaList.size(); i++) {
                int alpha = Integer.parseInt(alphaList.get(i));
                int startWidth = Integer.parseInt(startWidthList.get(i));
                paint.setAlpha(alpha);
                canvas.drawCircle(getWidth() / 2, getHeight() / 2, startWidth,paint);
                //同心圆扩散
                if (isStarting && alpha > 0 && startWidth < maxWidth)
                {
                    alphaList.set(i, (alpha-1)+"");
                    startWidthList.set(i, (startWidth+1)+"");
                }
            }
            if (isStarting&&Integer.parseInt(startWidthList.get(startWidthList.size() - 1)) == maxWidth / 5) {
                alphaList.add("255");
                startWidthList.add("0");
            }
            //同心圆数量达到6个，删除最外层圆
            if(isStarting&&startWidthList.size()==6)
            {
                startWidthList.remove(0);
                alphaList.remove(0);
            }
            //刷新界面
            invalidate();
        }

    }

    private void setNLength(){
        int urllength = ImageUrls.length;
        if(urllength > 4){
            NLength = (urllength < 7 && IsLoop)?(urllength - 1):7;
        }else{
            NLength = urllength;
        }
    }

    private void AddView(int next){
        if (ImageUrls.length <= 0 || NLength <= 0) return;
        final int position = next%ImageUrls.length;
        int i = NextAdd%NLength;
        int num = 0;
        Boolean isfinish = false;
        while (!isfinish){
            RandomXY(i);
            int j;
            for (j = 0; j < NLength; j++) {
                if (j == i) continue;
                if(PointDistance(views[i].getX(), views[i].getY(), views[j].getX(), views[j].getY()) < WareBevels*2){
                    num ++;
                    break;
                }
            }
            if (num > 300){ //设置预警线，防止死循环，清理当前View
                //RHandler.postDelayed(RemoveView, RmvTime);
                if (listener != null) {
                    listener.AddViewFailed(position);
                }
                return;
            }
            if (j == NLength){
                isfinish = true;
            }
        }

        NextAdd ++;NextUrl ++;SLength ++;
        views[i].clearAnimation();
        views[i].setAnimation(animationIn);
        views[i].setVisibility(View.VISIBLE);
        Glide.with(context).load(ImageUrls[position]).transform(new GlideRoundBitmap(context)).into(views[i]);
        views[i].setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.OnItemSelected(position);
                }
            }
        });
        if (listener != null) {
            listener.AddViewSuccessed(position);
        }

        if (ImageUrls.length > NextUrl || (IsLoop && ImageUrls.length > 4)){
            AHandler.postDelayed(ShowView, AddTime);
            if (SLength == NLength && (ImageUrls.length > 7 || IsLoop)) {
                NextAdd = NextRmv;
                RHandler.postDelayed(RemoveView, RmvTime);
                if (IsLoop || (ImageUrls.length - NextUrl > 1)) {
                    RHandler.postDelayed(RemoveView, 2*RmvTime);
                    if (IsLoop || (ImageUrls.length - NextUrl > 2)){
                        RHandler.postDelayed(RemoveView, 5*RmvTime);
                    }
                }
            }
        }
    }

    Runnable ShowView = new Runnable() {
        @Override
        public void run() {
            AHandler.sendEmptyMessage(NextUrl);
        }
    };

    Runnable RemoveView = new Runnable() {
        @Override
        public void run() {
            if(ImageUrls.length < 4 || NLength <= 0) return;
            RHandler.sendEmptyMessage(NextRmv);
        }
    };

    private void RandomXY(int i) {
        if (WareMargin <= 0) return;
        int R = random.nextInt(WareMargin) + WareIView;
        int XX = random.nextInt(R*2);
        int X = XX%R;
        int Y = (int)Math.sqrt(R*R-X*X);

        views[i].setX((float) ((XX>R)?X:-X) + WareMarginX - WareRadius);
        views[i].setY((float) ((random.nextBoolean())?Y:-Y) + WareMarginY - WareRadius);

//        views[i].setX((float) WareMarginX);
//        views[i].setY((float) WareMarginY);
    }

    private float PointDistance(float x1, float y1, float x2, float y2){
        float x = Math.abs(x1 - x2);
        float y = Math.abs(y1 - y2);
        return (float)Math.sqrt(x*x + y*y);
    }

    @SuppressLint("HandlerLeak")
    private final Handler AHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isStarting){
                AddView(msg.what);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler RHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isStarting) {
                int p = msg.what%NLength;
                views[p].clearAnimation();
                views[p].setAnimation(animationOut);
                views[p].setVisibility(View.INVISIBLE);
                if (listener != null) {
                    listener.RemoveView(msg.what%ImageUrls.length);
                }
                NextRmv ++;
                SLength --;
            }
        }
    };

    /**
     * 根据手机的分辨率从 dp的单位 转成为 px(像素)
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 获取设备屏幕宽度 px
     */
    private int getDisplayWidthPx(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取设备屏幕高度 px
     */
    private int getDisplayHeightPx(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取设备屏幕高度(不计通知栏高度) px
     */
    private int getDisplayHeightPxWithoutBar(Context context) {
        return getDisplayHeightPx(context) - getStatusBarHeight(context);
    }

    /**
     * 获取设备通知栏高度 px
     */
    private int getStatusBarHeight(Context context){
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }

    class GlideRoundBitmap extends BitmapTransformation {

        public GlideRoundBitmap(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public String getId() {
            // TODO Auto-generated method stub
            return getClass().getName();
        }

        @Override
        protected Bitmap transform(BitmapPool arg0, Bitmap arg1, int arg2, int arg3) {
            // TODO Auto-generated method stub
            if(arg1 == null) return null;
            return getRoundedCornerBitmap(arg1);
        }

        //圆形头像
        private Bitmap getRoundedCornerBitmap(Bitmap bitmap) {

            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final Paint paint = new Paint();
            //保证是方形，并且从中心画
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int w;
            int deltaX = 0;
            int deltaY = 0;
            if (width <= height) {
                w = width;
                deltaY = height - w;
            } else {
                w = height;
                deltaX = width - w;
            }
            final Rect rect = new Rect(deltaX, deltaY, w, w);
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            //圆形，所有只用一个

            int radius = (int) (Math.sqrt(w * w * 2.0d) / 2);
            canvas.drawRoundRect(rectF, radius, radius, paint);

            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
        }
    }

    /* 公共方法 */
    // 设置数据，并设置是否循环
    public void setData(String[] data, Boolean isloop) {
        this.ImageUrls = data;
        IsLoop = isloop;
        setNLength();
    }

    // 设置中间图片
    public void setImage(int id) {
        wareIView.setImageResource(id);
    }

    // 设置水波颜色
    public void setColor(int color) {
        paint.setColor(color);
    }

    // 开始(继续)
    public void start() {
        isStarting = true;
        AHandler.postDelayed(ShowView, AddTime);
    }

    // 暂停
    public void stop() {
        isStarting = false;
    }

    // 获取当前是否运行
    public boolean isStarting() {
        return isStarting;
    }

    // 获取当前显示个数
    public int getSLength() {
        return  SLength;
    }

    // 获取最多显示个数
    public int getNLength() {
        return  NLength;
    }

    /* 公共接口 */
    public interface WareViewListener {
        // 子项选中监听事件
        public void OnItemSelected(int position);
        // 添加子项监听事件(成功)
        public void AddViewSuccessed(int position);
        // 添加子项监听事件(失败)
        public void AddViewFailed(int position);
        // 添加子项监听事件
        public void RemoveView(int position);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(AHandler != null){ AHandler.removeCallbacks(ShowView); }
        if (RHandler != null){ RHandler.removeCallbacks(RemoveView); }
        this.clearAnimation();
        listener = null;
    }
}
