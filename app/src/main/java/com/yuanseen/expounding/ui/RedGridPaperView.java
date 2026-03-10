package com.yuanseen.expounding.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.DashPathEffect;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

import com.yuanseen.expounding.utils.TextProcessor;

import java.util.ArrayList;
import java.util.List;

public class RedGridPaperView extends View {

    private Paint gridPaint;
    private Paint textPaint;
    private Paint borderPaint;
    private Paint markPaint; // 用于字数标注的画笔
    private Paint backgroundPaint; // 背景画笔
    private List<String> lines;
    private float cellSize;
    private int rowCount;

    // 作文纸特有的常量
    private static final int COLS = 25; // 每行25格
    private static final float BORDER_WIDTH = 3f; // 边框宽度
    private static final int ROW_SPACING = 8; // 行间距（像素）
    private static final float MARK_TEXT_SIZE = 12f; // 字数标注文字大小（像素）
    private static final int TOP_PADDING = 20; // 顶部留白（像素）

    public RedGridPaperView(Context context) {
        super(context);
        init();
    }

    public RedGridPaperView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RedGridPaperView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置默认背景为白色
        setBackgroundColor(Color.WHITE);

        // 背景画笔（用于导出时确保背景是白色）
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);

        // 网格线画笔 - 淡红色，更细
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#FFAAAA")); // 淡红色
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);

        // 边框画笔 - 淡红色，更粗
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#FFAAAA")); // 淡红色
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_WIDTH);

        // 文字画笔 - 深灰色，稍微调小一点
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(40f);

        // 字数标注画笔 - 浅灰色，小字号
        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markPaint.setColor(Color.parseColor("#999999")); // 浅灰色
        markPaint.setTextSize(MARK_TEXT_SIZE);
        markPaint.setTextAlign(Paint.Align.CENTER); // 居中对齐

        lines = new ArrayList<>();
    }

    public void setParagraphs(List<ParagraphItem> paragraphs) {
        lines = TextProcessor.processParagraphs(paragraphs);
        rowCount = lines.size();
        invalidate();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算理想的格子大小：宽度/25
        int width = MeasureSpec.getSize(widthMeasureSpec);
        cellSize = width / (float) COLS;

        // 高度 = 顶部留白 + (行数 * 格子高度) + ((行数 - 1) * 行间距) + 底部留白（为字数标注留出空间）
        int desiredHeight = (int) (TOP_PADDING + rowCount * cellSize + (rowCount - 1) * ROW_SPACING + MARK_TEXT_SIZE + 20);

        // 考虑padding
        int height = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制白色背景（确保背景是纯白色）
        canvas.drawColor(Color.WHITE);

        // 绘制网格（考虑顶部留白）
        drawGrid(canvas);

        // 绘制外边框（考虑顶部留白）
        drawBorder(canvas);

        // 绘制文字（考虑顶部留白）
        drawText(canvas);

        // 绘制字数标注（考虑顶部留白）
        drawWordCountMarks(canvas);
    }

    private void drawGrid(Canvas canvas) {
        float width = getWidth();

        // 计算实际绘制区域（去掉边框宽度的影响）
        float startX = BORDER_WIDTH / 2f;
        float endX = width - BORDER_WIDTH / 2f;

        // 绘制每一行的网格
        for (int row = 0; row < rowCount; row++) {
            // 计算行的起始Y坐标（考虑顶部留白）
            float rowStartY = TOP_PADDING + row * (cellSize + ROW_SPACING) + BORDER_WIDTH / 2f;
            float rowEndY = rowStartY + cellSize;

            // 绘制竖线（每一行独立绘制）
            for (int i = 1; i < COLS; i++) {
                float x = i * cellSize;
                if (x >= startX && x <= endX) {
                    canvas.drawLine(x, rowStartY, x, rowEndY, gridPaint);
                }
            }

            // 绘制横线（每一行的上下边框）
            canvas.drawLine(startX, rowStartY, endX, rowStartY, gridPaint); // 上边框
            canvas.drawLine(startX, rowEndY, endX, rowEndY, gridPaint); // 下边框
        }
    }

    private void drawBorder(Canvas canvas) {
        float width = getWidth();

        // 绘制每一行的独立边框
        for (int row = 0; row < rowCount; row++) {
            // 计算行的起始和结束Y坐标（考虑顶部留白）
            float rowStartY = TOP_PADDING + row * (cellSize + ROW_SPACING);
            float rowEndY = rowStartY + cellSize;

            // 上边框
            canvas.drawLine(0, rowStartY, width, rowStartY, borderPaint);
            // 下边框
            canvas.drawLine(0, rowEndY, width, rowEndY, borderPaint);
            // 左边框
            canvas.drawLine(0, rowStartY, 0, rowEndY, borderPaint);
            // 右边框
            canvas.drawLine(width, rowStartY, width, rowEndY, borderPaint);
        }
    }

    private void drawText(Canvas canvas) {
        float textSize = cellSize * 0.6f; // 文字大小为格子的60%
        textPaint.setTextSize(textSize);

        // 计算文字基线位置（垂直居中）
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float textOffset = (cellSize - textHeight) / 2f - fontMetrics.ascent;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // 计算Y坐标（考虑顶部留白）
            float y = TOP_PADDING + i * (cellSize + ROW_SPACING) + textOffset;

            for (int j = 0; j < line.length() && j < COLS; j++) {
                char c = line.charAt(j);
                // 水平居中
                float textWidth = textPaint.measureText(String.valueOf(c));
                float x = j * cellSize + (cellSize - textWidth) / 2f;

                canvas.drawText(String.valueOf(c), x, y, textPaint);
            }
        }
    }

    private void drawWordCountMarks(Canvas canvas) {
        if (rowCount == 0) return;

        // 每5行标注一次：100, 200, 300, ...
        for (int row = 3; row < rowCount; row += 4) { // row 4 是第5行（从0开始计数）
            float x = getWidth() - cellSize / 2f; // 最后一格的中间位置

            // 计算标注的Y位置：在当前行的下方，紧挨着格子底部（考虑顶部留白）
            float rowBottomY = TOP_PADDING + (row + 1) * (cellSize + ROW_SPACING); // 当前行底部位置
            float y = rowBottomY + MARK_TEXT_SIZE; // 在行下方放置标注文字

            // 计算字数：行数（从1开始计数）* 25
            int wordCount = (row + 1) * COLS;

            // 格式化字数：100, 200, 300, ... 或者 100字, 200字
            String markText = wordCount + "字";

            // 如果字数超过1000，可以简化为1k字、2k字等
            if (wordCount >= 1000) {
                if (wordCount % 1000 == 0) {
                    markText = (wordCount / 1000) + "k字";
                } else {
                    markText = (wordCount / 1000) + "." + ((wordCount % 1000) / 100) + "k字";
                }
            }

            // 绘制字数标注
            canvas.drawText(markText, x, y, markPaint);
        }
    }

    /**
     * 生成作文纸的Bitmap，用于导出
     * 确保背景是白色，并包含顶部留白
     */
    public Bitmap createExportBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        // 先绘制白色背景
        canvas.drawColor(Color.WHITE);

        // 绘制网格（考虑顶部留白）
        drawGrid(canvas);

        // 绘制外边框（考虑顶部留白）
        drawBorder(canvas);

        // 绘制文字（考虑顶部留白）
        drawText(canvas);

        // 绘制字数标注（考虑顶部留白）
        drawWordCountMarks(canvas);

        return bitmap;
    }

    /**
     * 获取顶部留白大小
     */
    public int getTopPadding() {
        return TOP_PADDING;
    }
}