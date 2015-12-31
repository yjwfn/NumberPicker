package com.lw.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import com.lw.R;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;

/**
 * Created by yjwfn on 15-12-24.
 */
public class NumberPickerView extends View {

    private static final String TAG = "NumberPickerView";
    private Paint   mTextPaint;

    private int     mMinValue;

    private int     mMaxValue;

    private int     mPageSize;

    @DimenRes
    private int     mTextSize;

    private int     mLastTouchY;


    private int     mActivePointerId;

    private Object[]  mSelector;

    private OverScroller    mOverScroller;
    private VelocityTracker mVelocityTracker;

    private boolean mIsDragging;
    private int     mTouchSlop;
    private int     mMaximumVelocity;
    private int     mMinimumVelocity;


    @ColorInt
    private int     mTextColorNormal;

    @ColorInt
    private int     mTextColorSelected;


    private Reference<OnValueChanged> mCallbackRef;

    public NumberPickerView(Context context) {
        this(context, null);
    }

    public NumberPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerView);
        mTextSize = (int) array.getDimension(R.styleable.NumberPickerView_numberTextSize, 80);
        mMaxValue = array.getInt(R.styleable.NumberPickerView_maxValue, 0);
        mMinValue = array.getInt(R.styleable.NumberPickerView_minValue, 0);
        mPageSize = array.getInt(R.styleable.NumberPickerView_numberPageSize, 5);
        mTextColorNormal = array.getColor(R.styleable.NumberPickerView_numberColorNormal, Color.GREEN);
        mTextColorSelected = array.getColor(R.styleable.NumberPickerView_numberColorSelected, Color.RED);
        array.recycle();

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStyle(Paint.Style.STROKE);

        if(mMinValue < mMaxValue){
            mSelector = new Object[mMaxValue - mMinValue + 1];
            for(int  selectorIndex = mMinValue; selectorIndex <= mMaxValue; selectorIndex++){
                mSelector[selectorIndex - mMinValue] = selectorIndex;
            }


        }

        mOverScroller = new OverScroller(context, new DecelerateInterpolator());
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mSelector == null || mSelector.length < 1)
            return;

        int width = getWidth();
        int height = getHeight();

        int itemHeight = getItemHeight();
        int textHeight = computeTextHeight();
        int centerY = getScrollY() + height / 2;
        Rect itemRect = new Rect();
        int selectedPos = computePosition();
        for(int itemIndex = 0; itemIndex < mSelector.length; itemIndex++){

            itemRect.set(0, itemIndex * itemHeight, width, itemIndex * itemHeight + itemHeight);
           // canvas.drawRect(itemRect, mTextPaint);

            if(itemIndex == selectedPos){
                mTextPaint.setColor(mTextColorSelected);
            }else{
                mTextPaint.setColor(mTextColorNormal);
            }


            /*
                越靠近中间的文体越大。
                distance / (height / 2f) 算出的是递增的0-1之间的。
                1f - distance / (height / 2f) 将值变为递减1-0之间 。

                distance乘0.5可以确保scale不小于0.5
             */
            float distance = Math.abs(itemRect.centerY() - centerY) * 0.5f;
            float scale = 1f - distance / (height / 2f) ;
            float pivotY = itemRect.centerY();
            int  alpha = (int) (scale * 255);


            mTextPaint.setAlpha(alpha);
            canvas.save();
            canvas.scale(scale, scale, itemRect.centerX(),  pivotY);
            int y = (itemRect.top + itemRect.bottom - textHeight) /2;
            canvas.drawText(mSelector[itemIndex] + "" , itemRect.width() / 2 , y , mTextPaint);
            canvas.restore();
        }

    /*    mTextPaint.setColor(Color.BLACK);
        canvas.drawLine(0, centerY, width, centerY, mTextPaint);*/

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        ViewGroup.LayoutParams lp = getLayoutParams();
        if(lp == null)
            lp = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT,  ViewGroup.LayoutParams.WRAP_CONTENT);

        int width =  calculateSize(getSuggestedMinimumWidth(), lp.width, widthMeasureSpec);
        int height =  calculateSize(getSuggestedMinimumHeight(), lp.height, heightMeasureSpec);

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();


        setMeasuredDimension(width, height);
    }

    /**
     *
     * @param suggestedSize 最合适的大小
     * @param paramSize 配置的大小
     * @param measureSpec
     * @return
     */
    private int calculateSize(int suggestedSize, int paramSize, int measureSpec){
        int result = 0;
        int size = MeasureSpec.getSize(measureSpec);
        int mode = MeasureSpec.getMode(measureSpec);

        switch (MeasureSpec.getMode(mode)){
            case MeasureSpec.AT_MOST:

                if(paramSize == ViewGroup.LayoutParams.WRAP_CONTENT)
                    result = Math.min(suggestedSize, size);
                else if(paramSize == ViewGroup.LayoutParams.MATCH_PARENT)
                    result = size;
                else {
                    result = Math.min(paramSize, size);
                }

                break;
            case MeasureSpec.EXACTLY:
                 result = size;
                break;
            case MeasureSpec.UNSPECIFIED:

                if(paramSize == ViewGroup.LayoutParams.WRAP_CONTENT || paramSize == ViewGroup.LayoutParams.MATCH_PARENT)
                    result = suggestedSize;
                else {
                    result = paramSize;
                }

                break;
        }

        return result;
    }

    @Override
    protected int getSuggestedMinimumHeight() {

        int suggested = super.getSuggestedMinimumWidth();
        if(mSelector != null && mSelector.length > 0 && mPageSize > 0){
            Paint.FontMetricsInt fontMetricsInt = mTextPaint.getFontMetricsInt();
            int height = fontMetricsInt.descent - fontMetricsInt.ascent;
            suggested = Math.max(suggested,  height * mPageSize);
        }

        return suggested;
    }



    @Override
    protected int getSuggestedMinimumWidth() {

        int suggested = super.getSuggestedMinimumHeight();
        if(mSelector != null && mSelector.length > 0 && mPageSize > 0){
            suggested = Math.max(suggested, computeMaximumWidth());
        }

        return suggested;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(mVelocityTracker != null){
            mVelocityTracker.addMovement(event);
        }


        int action = event.getActionMasked();

        switch (action){

            case MotionEvent.ACTION_DOWN:
                if(!mOverScroller.isFinished())
                    mOverScroller.abortAnimation();

                if(mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }else
                    mVelocityTracker.clear();

                mVelocityTracker.addMovement(event);
                mActivePointerId = event.getPointerId(0);

                mLastTouchY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:

                int deltaY = (int) (mLastTouchY - event.getY(mActivePointerId));
                if(!mIsDragging && Math.abs(deltaY) > mTouchSlop ) {
                    //do something
                    final ViewParent parent = getParent();
                    if (parent != null)
                        parent.requestDisallowInterceptTouchEvent(true);


                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }

                    mIsDragging = true;
                }

                if(mIsDragging){
                    if(canScroll(deltaY))
                        scrollBy(0, deltaY);
                    else {


                    }

                    mLastTouchY = (int) event.getY();
                }

                break;
            case MotionEvent.ACTION_UP:

                if(mIsDragging) {

                    mIsDragging = false;

                    final ViewParent    parent = getParent();
                    if (parent != null)
                        parent.requestDisallowInterceptTouchEvent(false);

                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocity = (int) mVelocityTracker.getYVelocity(mActivePointerId);
                    if (Math.abs(velocity) > mMinimumVelocity) {
                        mOverScroller.fling(getScrollX(), getScrollY(), 0, -velocity, 0, 0, getMinimumScrollY(), getMaximumScrollY(), 0, 0);
                        invalidateOnAnimation();
                    } else {
                        //align item;
                        adjustItem();
                    }

                    recyclerVelocityTracker();
                }else{
                    //click event
                    int y = (int) event.getY(mActivePointerId);
                    handlerClick(y);
                }

                break;
            case MotionEvent.ACTION_CANCEL:

                if(mIsDragging){
                    adjustItem();
                    mIsDragging = false;
                }

                recyclerVelocityTracker();

                break;
        }


        return true;
    }

    private void    recyclerVelocityTracker(){

        if(mVelocityTracker != null)
                mVelocityTracker.recycle();

        mVelocityTracker = null;
    }
    private void    invalidateOnAnimation(){
        if(Build.VERSION.SDK_INT >= 16)
            postInvalidateOnAnimation();
        else
            invalidate();
    }

    private void    handlerClick(int y){

        y = y + getScrollY();

        int position = y / getItemHeight();
        if(y >= 0 && position < mSelector.length) {
            Rect actualLoc = getLocationByPosition(position);
            int scrollY = actualLoc.top - getScrollY();
            mOverScroller.startScroll(getScrollX(), getScrollY(), 0, scrollY);
            invalidateOnAnimation();
        }
    }

    /**
     * 获取一个item位置，通过滚动正好将这个item放置在中间
     * @param position
     * @return
     */
    private Rect getLocationByPosition(int position){
        int scrollY = position * getItemHeight() + getMinimumScrollY();
        return new Rect(0, scrollY, getWidth(), scrollY + getItemHeight());
    }
    @Override
    public void computeScroll() {
        super.computeScroll();

        if(mOverScroller.computeScrollOffset()){
            int x = mOverScroller.getCurrX();
            int y = mOverScroller.getCurrY();
            scrollTo(x, y);
            invalidate();
         }else if(!mIsDragging){
            //align item
            adjustItem();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if(mCallbackRef != null && mCallbackRef.get() != null) {
            int position = computePosition();
            mCallbackRef.get().onValueChanged(position, mSelector[position]);
        }

    }

    public void    smoothScrollTo(int position){
        if(position < 0 || mSelector == null || position > mSelector.length)
            return;

        Rect actualLoc = getLocationByPosition(position);
        int scrollY = actualLoc.top - getScrollY();
        mOverScroller.startScroll(getScrollX(), getScrollY(), 0, scrollY);
        invalidateOnAnimation();
    }


    public void smoothScrollToValue(Object object){
        if(mSelector == null)
            return;

        int pos = Arrays.binarySearch(mSelector, object);
        smoothScrollTo(pos);
    }

    /**
     * 调整item使对齐居中
     */
    private void    adjustItem( ){
        int position  = computePosition();
        Rect rect = getLocationByPosition(position);
        int scrollY =    rect.top - getScrollY();

        if(scrollY != 0) {
            mOverScroller.startScroll(getScrollX(), getScrollY(), 0, scrollY);
            invalidateOnAnimation();
        }
    }


    private int computePosition(int offset){
        int topOffset = getHeight() / 2;
        int scrollY = getScrollY()   + topOffset +  offset;
        int position = scrollY / getItemHeight();
        return position;
    }


    /**
     * 计算当前显示的位置
     * @return
     */
    public int computePosition(){
        return computePosition(0);
    }

    private void    printLog(String msg){
       // Log.d(TAG,msg);

    }

    public void     setSelector(Object...args){
        mSelector = args;


        postInvalidate();
    }

    private boolean canScroll(int deltaY){
        int scrollY = getScrollY() + deltaY;
        int top = getMinimumScrollY();
        int bottom = getMaximumScrollY();

        return scrollY >= top && scrollY <= bottom;
    }


    private int     getMaximumScrollY(){
        return (mSelector.length - 1) * getItemHeight()  + getMinimumScrollY();
    }

    private int     getMinimumScrollY(){
        return   -((getHeight() - getItemHeight()) / 2);
    }


    public int     getItemHeight(){
        return getHeight() / mPageSize;
    }

    private int     computeTextHeight(){
        Paint.FontMetricsInt metricsInt = mTextPaint.getFontMetricsInt();
        return metricsInt.bottom + metricsInt.top;
    }

    private int computeMaximumWidth(){
        Paint.FontMetricsInt    fontMetricsInt = mTextPaint.getFontMetricsInt();
        int result = (int) mTextPaint.measureText("0000");
        int width = 0;
        for(int objIndex =  0; mSelector != null &&  objIndex < mSelector.length; objIndex++){
            width = (int) mTextPaint.measureText(mSelector[objIndex].toString());
            if(width > result)
                result = width;
        }

        return result;
    }


    public void     setListener(OnValueChanged valueChanged){
        mCallbackRef = new SoftReference<>(valueChanged);
    }

    public interface OnValueChanged{
        void onValueChanged(int position, Object value);
    }
}
