package com.example.yukiishikawa.mymemoapp;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.EditText;

import org.w3c.dom.Attr;

/**
 * Created by yukiishikawa on 2017/07/22.
 */

public class MemoEditText extends android.support.v7.widget.AppCompatEditText {
    private static final int SOLID = 1;
    private static final int DASH = 2;
    private static final int NORMAL = 3;
    private static final int BOLD = 4;

    private int mMeasuredWidth;
    private int mLineHeight;
    private int mDisplayLineCount;

    private Path mPath;
    private Paint mPaint;

    public MemoEditText(Context context) {
        this(context, null);
    }

    public MemoEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MemoEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPath = new Path();
        mPaint = new Paint();

        mPaint.setStyle(Paint.Style.STROKE);

        if (attrs != null && !isInEditMode()) {
            int lineEffectBit;
            int lineColor;

            Resources resources = context.getResources();
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MemoEditText);
            try {
                lineEffectBit = typedArray.getInteger(R.styleable.MemoEditText_lineEffect, SOLID);
                lineColor = typedArray.getColor(R.styleable.MemoEditText_lineColor, Color.GRAY);
            } finally {
                typedArray.recycle();
            }

            // 罫線のエフェクタを設定
            if ((lineEffectBit & DASH) == DASH) {
                DashPathEffect effect = new DashPathEffect(new float[]{
                        resources.getDimension(R.dimen.text_rule_interval_on),
                        resources.getDimension(R.dimen.text_rule_interval_off)
                }, 0f);
                mPaint.setPathEffect(effect);
            }

            float strokeWidth;
            if ((lineEffectBit & BOLD) == BOLD) {
                strokeWidth = resources.getDimension(R.dimen.text_rule_width_bold);
            } else {
                strokeWidth = resources.getDimension(R.dimen.text_rule_width_normal);
            }
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setColor(lineColor);

        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mMeasuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        mLineHeight = getLineHeight();

        mDisplayLineCount = measuredHeight / mLineHeight;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int paddingTop = getExtendedPaddingTop();
        int scrollY = getScrollY();
        int firstVisibleLine = getLayout().getLineForVertical(scrollY);
        int lastVisibleLine = firstVisibleLine + mDisplayLineCount;

        mPath.reset();
        for ( int i = firstVisibleLine; i <= lastVisibleLine; i++) {
            mPath.moveTo(0, i * mLineHeight + paddingTop);
            mPath.lineTo(mMeasuredWidth, i * mLineHeight + paddingTop);
        }

        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }
}
