package com.picker.api;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PickerItemDecoration extends RecyclerView.ItemDecoration {
    private final float size;
    private final float margin;
    private final Paint paint;
    private final RectF rectF;

    public PickerItemDecoration() {
        this(Color.LTGRAY, 1.0f, 0f);
    }

    public PickerItemDecoration(int color) {
        this(color, 1.0f, 0f);
    }

    public PickerItemDecoration(int color, float size) {
        this(color, size, 0f);
    }

    public PickerItemDecoration(int color, float size, float margin) {
        this.size = size;
        this.margin = margin;
        rectF = new RectF();
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }


    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        if (parent.getLayoutManager() == null || !(parent.getLayoutManager() instanceof PickerLayoutManager)) {
            return;
        }
        calcDivider(c, parent);
    }


    private void calcDivider(Canvas canvas, RecyclerView parent) {
        PickerLayoutManager lm = (PickerLayoutManager) parent.getLayoutManager();
        if (lm != null) {
            int itemSize = lm.orientation == PickerLayoutManager.HORIZONTAL ? parent.getWidth() / lm.visibleCount : parent.getHeight() / lm.visibleCount;
            int startDrawPosition = (lm.visibleCount - 1) / 2;
            int endDrawPosition = startDrawPosition + 1;
            drawDivider(canvas, itemSize, startDrawPosition, parent, lm);
            drawDivider(canvas, itemSize, endDrawPosition, parent, lm);
        }

    }

    private void drawDivider(Canvas canvas, int itemSize, int position, RecyclerView parent, PickerLayoutManager lm) {
        if (lm.orientation == PickerLayoutManager.HORIZONTAL) {
            float left = position * itemSize * 1.0f - size / 2;
            float right = left + size;
            rectF.set(left, margin, right, parent.getHeight() - margin);
            canvas.drawRect(rectF, paint);
        } else {
            float top = position * itemSize * 1.0f - size / 2;
            float bottom = top + size;
            rectF.set(margin, top, parent.getWidth() - margin, bottom);
            canvas.drawRect(rectF, paint);
        }
    }
}
