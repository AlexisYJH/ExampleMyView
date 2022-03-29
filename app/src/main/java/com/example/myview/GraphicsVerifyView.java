package com.example.myview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

public class GraphicsVerifyView extends View {
    private float mSeekBorderWidth = 4f;//滑块描边宽度
    private int mSeekBgColor = Color.parseColor("#EEEEEE"); //滑块背景颜色
    private int mSeekDefaultColor = Color.WHITE; //滑块默认颜色
    private int mSeekBorderColor = Color.GRAY; //滑块描边颜色
    private int mSeekTouchColor = Color.BLUE; //滑块触摸的颜色
    private int mSeekVerFailColor = Color.RED; //滑块验证失败的颜色
    private int mSeekVerSuccessColor = Color.GREEN; //滑块验证成功的颜色
    private int mSeekArrowDefaultColor = Color.GRAY; //滑块箭头默认颜色
    private int mSeekArrowTouchColor = Color.WHITE; //滑块箭头触摸颜色
    private float mOffsetDegrees = 10f;//允许的误差角度

    private Bitmap mImage;
    private Paint mPaint;
    private float mDefaultDegrees;
    private float mCurrentDegrees = 0f; //当前的图片角度
    private Path mSeekBgPath = new Path();
    private float mCircleRadius = 0f; //滑块两边圆角的半径
    private float mSeekMoveX = 0f; //手指触摸的X轴位置
    private float mSeekCenterX = 0f; //滑块的中心点位置
    private float mSeekTop = 0f; //滑块距离顶部的距离
    private boolean mIsTouch;
    private Status mStatus = Status.DEFAULT;
    private VerifyListener mListener;
    /**
     * 验证的三个状态
     */
    enum Status{
        DEFAULT,//默认
        FAIL, //失败
        SUCCESS //成功
    }

    interface VerifyListener{
        void onSuccess();
        void onFail();
    }
    
    public GraphicsVerifyView(Context context) {
        this(context, null);
    }

    public GraphicsVerifyView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphicsVerifyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.GraphicsVerifyView);
        mSeekBorderWidth = a.getDimension(R.styleable.GraphicsVerifyView_seekBorderWidth,4f);
        mSeekBgColor = a.getColor(R.styleable.GraphicsVerifyView_seekBgColor,Color.parseColor("#EEEEEE"));
        mSeekDefaultColor = a.getColor(R.styleable.GraphicsVerifyView_seekDefaultColor,Color.WHITE) ;
        mSeekBorderColor = a.getColor(R.styleable.GraphicsVerifyView_seekBorderColor,Color.GRAY);
        mSeekTouchColor = a.getColor(R.styleable.GraphicsVerifyView_seekTouchColor,Color.BLUE);
        mSeekVerFailColor = a.getColor(R.styleable.GraphicsVerifyView_seekVerFailColor,Color.RED);
        mSeekVerSuccessColor = a.getColor(R.styleable.GraphicsVerifyView_seekVerSuccessColor,Color.GREEN);
        mSeekArrowDefaultColor = a.getColor(R.styleable.GraphicsVerifyView_seekArrowDefaultColor,Color.GRAY);
        mSeekArrowTouchColor = a.getColor(R.styleable.GraphicsVerifyView_seekArrowTouchColor,Color.WHITE);
        int imgSrc = a.getResourceId(R.styleable.GraphicsVerifyView_imgSrc,R.drawable.img);
        mOffsetDegrees = a.getFloat(R.styleable.GraphicsVerifyView_offsetDegrees,10f);
        a.recycle();

        //关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mImage = BitmapFactory.decodeResource(context.getResources(), imgSrc);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //随机初始化默认角度(值的范围为-80~-280)，匹配的时候与滑块旋转的角度相加如果在误差范围内就验证成功
        mDefaultDegrees = getRandomDegrees();
    }

    private float getRandomDegrees() {
        return - new Random().nextInt(201) - 80f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST ? 300 : 
                MeasureSpec.getSize(widthMeasureSpec);

        //这里的高度只有自适应，如果可以精确指定的话可能会导致控件展示不全, 高度根据宽度来定
        int height = new Float(width / 10f + width / 2f + width / 6f + mSeekBorderWidth / 2f).intValue();
        
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //缩放图片大小(宽高都缩放为控件宽度的二分之一)
        int scaleValue = w / 2;
        mImage = Bitmap.createScaledBitmap(mImage, scaleValue,scaleValue,true);
        mSeekTop = getWidth() / 10f + mImage.getWidth();
        //滑块两边圆角的半径为控件宽度的十二分之一
        mCircleRadius = getWidth() / 12f;
        initSeekBgPath();
    }

    private void initSeekBgPath() {
        //距离上边图片的高度(为图片的宽度加控件宽度的十分之一)
        float top = mSeekTop;
        //由于画笔的stroke是平均向内向外扩散的，因此需要滑块背景两边需要预留二分之一的seekBgBorderWidth的宽度才能保证不超出控件
        float borderOffset = mSeekBorderWidth / 2;
        //通过计算得出滑块背景的路径
        mSeekBgPath.moveTo(mCircleRadius+borderOffset,top);
        mSeekBgPath.addArc(borderOffset,top ,mCircleRadius * 2 + borderOffset,top + mCircleRadius * 2,-90f,-180f);
        mSeekBgPath.lineTo(getWidth() - mCircleRadius - borderOffset,top + mCircleRadius * 2);
        mSeekBgPath.addArc(getWidth() - mCircleRadius * 2 - borderOffset,top ,
                getWidth() - borderOffset,
                top + mCircleRadius * 2,
                90f,-180f);
        mSeekBgPath.lineTo(mCircleRadius + borderOffset,top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawSeekBg(canvas);
        drawSeek(canvas);
        drawCircleImage(canvas);
    }

    private void drawSeekBg(Canvas canvas) {
        mPaint.setColor(mSeekBgColor);
        mPaint.setStrokeWidth(mSeekBorderWidth);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mSeekBgPath, mPaint);

        mPaint.setColor(mSeekBorderColor);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mSeekBgPath, mPaint);
    }

    private void drawSeek(Canvas canvas) {
        //圆的中心点X(根据手指的位置动态改变)
        mSeekCenterX = Math.max(mCircleRadius, Math.min(getWidth()- mCircleRadius, mSeekMoveX));
        //圆的中心点Y
        float centerY = mSeekTop + mCircleRadius;
        mPaint.setColor(getColor(mSeekTouchColor, mSeekVerSuccessColor, mSeekVerFailColor, mSeekDefaultColor));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mSeekCenterX, centerY, mCircleRadius - mSeekBorderWidth, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getColor(mSeekTouchColor, mSeekVerSuccessColor, mSeekVerFailColor, mSeekBorderColor));
        mPaint.setStrokeWidth(mSeekBorderWidth);
        canvas.drawCircle(mSeekCenterX, centerY, mCircleRadius - mSeekBorderWidth, mPaint);

        //画滑块中间的两个箭头
        mPaint.setTextSize(mCircleRadius);
        mPaint.setColor(getColor(mSeekArrowTouchColor, mSeekArrowTouchColor, mSeekArrowTouchColor, mSeekArrowDefaultColor));
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setStrokeWidth(2f);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        // 计算文字高度
        float fontHeight = fontMetrics.bottom - fontMetrics.top;
        // 计算文字baseline 让文字垂直居中
        float textBaseY = mCircleRadius - fontHeight/2;
        canvas.save();
        canvas.translate(mSeekCenterX, centerY);
        canvas.rotate(180f);
        canvas.drawText("ㄍ", 0f, textBaseY, mPaint);
        canvas.restore();                
    }
    

    private int getColor(int touchColor, int successColor, int failColor, int defaultColor) {
        if (mIsTouch) {
            return touchColor;
        } else if (mStatus == Status.SUCCESS) {
            return successColor;
        } else if (mStatus == Status.FAIL) {
            return failColor;
        }
        return defaultColor;
    }
    
    private void drawCircleImage(Canvas canvas) {
        //根据滑块移动的距离计算旋转的角度
        mCurrentDegrees = (mSeekCenterX - mCircleRadius) / (getWidth() - mCircleRadius * 2) * 360 + mDefaultDegrees;
        canvas.save();
        //先将画布移动到原点为（width/2f,width/4f）的位置（即：显示图片的中心点位置）
        canvas.translate(getWidth()/2f,getWidth()/4f);
        //根据拖动滑块来调整角度
        canvas.rotate(mCurrentDegrees);
        //利用混合模式将图片画成圆形
        canvas.drawCircle(0f ,0f,getWidth() / 4f,mPaint);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(mImage,-mImage.getWidth() / 2f,-mImage.getWidth() / 2f,mPaint);
        //清空混合模式
        mPaint.setXfermode(null);
        canvas.restore();      
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //当状态为默认的时候才可以拖动
                if(mStatus == Status.DEFAULT){
                    //判断触摸点是否在滑块上
                    RectF rectF = new RectF(0f,mSeekTop,mCircleRadius * 2,mSeekTop + mCircleRadius * 2);
                    if(rectF.contains(event.getX(),event.getY())){
                        mIsTouch = true;
                        postInvalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsTouch) {
                    mSeekMoveX = event.getX();
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mStatus = (mCurrentDegrees <= mOffsetDegrees && mCurrentDegrees >= -mOffsetDegrees) ?
                        Status.SUCCESS : Status.FAIL;
                if (mListener != null) {
                    if (mStatus == Status.SUCCESS) {
                        mListener.onSuccess();
                    } else {
                        mListener.onFail();
                    }
                }
                mIsTouch = false;
                postInvalidate();
                break;                
            default:
                break;
        }
        return mIsTouch;
    }

    public void setListener(VerifyListener listener) {
        mListener = listener;
    }

    /**
     * 重置
     */
    public void reset(){
        if (mStatus == Status.DEFAULT) {
            return;
        }
        mSeekMoveX = 0f;
        mStatus = Status.DEFAULT;
        mDefaultDegrees = getRandomDegrees();
        postInvalidate();
    }
}
