package com.example.smartech;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AnimatedGradientView extends View {

    private Paint paint;
    private LinearGradient gradient;
    private int[] colors;
    private float translateX = 0;
    private ValueAnimator animator;

    public AnimatedGradientView(Context context) {
        super(context);
        init();
    }

    public AnimatedGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        // Define gradient colors (customize as needed)
        colors = new int[]{
                0xFF578A9E, // Purple (#B481D6)
                0xFF93C7A9, // Red (#E86464)
                0xFFE6DDAE, // Yellow (#E8BE82)
                0xFF93C7A9, // Red (loop back)
                0xFF578A9E
        };
        setupAnimation();
    }

    private void setupAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(10000); // Duration of one cycle (5 seconds)
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            translateX = getWidth() * (float) animation.getAnimatedValue();
            invalidate(); // Redraw the view
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Create gradient from left to right, twice the width for smooth looping
        gradient = new LinearGradient(
                -w, 5, w, 5,
                colors,
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Translate the canvas to animate the gradient
        canvas.translate(translateX, 0);
        canvas.drawRect(-getWidth(), 0, getWidth() * 2, getHeight(), paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animator.cancel(); // Stop animation when view is detached
    }
}