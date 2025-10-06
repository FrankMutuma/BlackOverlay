package com.smarttechnologies.app.blackoverlay;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.view.View;
import android.content.Context;
import android.graphics.Paint;

// Create a custom view class
public class SystemBarOverlayView extends View {
	private Paint blackPaint;

	public SystemBarOverlayView(Context context) {
		super(context);
		init();
	}

	public SystemBarOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		blackPaint = new Paint();
		blackPaint.setColor(0xFF000000);
		blackPaint.setStyle(Paint.Style.FILL);
		setWillNotDraw(false);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Draw black over entire area, including system bar regions
		canvas.drawRect(0, 0, getWidth(), getHeight(), blackPaint);
	}
}