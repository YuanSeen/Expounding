package com.yuanseen.expounding.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

import com.yuanseen.expounding.R;
import com.yuanseen.expounding.utils.TextProcessor;

import java.util.ArrayList;
import java.util.List;

public class RedGridPaperView extends View {

    private Paint gridPaint;
    private Paint textPaint;
    private Paint borderPaint;
    private Paint markPaint; // 用于字数标注的画笔
    private Paint punctuationPaint; // 新增：用于格子外标点的画笔
    private List<TextProcessor.RowData> rowDataList; // 修改：使用RowData而不是String
    private float cellSize;
    private int rowCount;

    // 作文纸特有的常量
    private static final int COLS = 25; // 每行25格
    private static final float BORDER_WIDTH = 3f; // 边框宽度
    private static final int ROW_SPACING = 8; // 行间距（像素）
    private static final float MARK_TEXT_SIZE = 12f; // 字数标注文字大小（像素）
    private static final int TOP_PADDING = 20; // 顶部留白（像素）

    // 左右两侧留白宽度（用于放置标点）
    private static final float LEFT_PUNCTUATION_WIDTH = 30f; // 左侧标点区域宽度
    private static final float RIGHT_PUNCTUATION_WIDTH = 30f; // 右侧标点区域宽度

    // 实际绘制区域的起始X坐标（从左侧标点区域之后开始）
    private float drawStartX = LEFT_PUNCTUATION_WIDTH;
    // 实际绘制区域的结束X坐标（到右侧标点区域之前）
    private float drawEndX;

    // 最小行数设置
    private int minRows = 4; // 默认最小显示4行
    private boolean forceMinRows = true; // 是否强制显示最小行数

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

        // 文字画笔 - 深灰色
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(40f);

        // 标点画笔 - 用于格子外标点
        punctuationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        punctuationPaint.setColor(Color.parseColor("#333333"));
        punctuationPaint.setTextSize(40f);
        punctuationPaint.setTextAlign(Paint.Align.CENTER);

        // 字数标注画笔 - 浅灰色，小字号
        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markPaint.setColor(Color.parseColor("#999999")); // 浅灰色
        markPaint.setTextSize(MARK_TEXT_SIZE);
        markPaint.setTextAlign(Paint.Align.CENTER); // 居中对齐

        rowDataList = new ArrayList<>();
    }

    /**
     * 设置是否强制显示最小行数
     */
    public void setForceMinRows(boolean force) {
        this.forceMinRows = force;
        invalidate();
        requestLayout();
    }

    /**
     * 设置最小行数
     */
    public void setMinRows(int rows) {
        this.minRows = rows;
        invalidate();
        requestLayout();
    }

    public void setParagraphs(List<ParagraphItem> paragraphs) {
        // 使用新的处理方法
        rowDataList = TextProcessor.processParagraphsToRows(paragraphs);
        rowCount = rowDataList.size();
        invalidate();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算可用的绘制宽度（减去左右留白）
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        float availableWidth = totalWidth - LEFT_PUNCTUATION_WIDTH - RIGHT_PUNCTUATION_WIDTH;

        // 计算格子大小：可用宽度/25
        cellSize = availableWidth / (float) COLS;

        // 设置实际绘制区域的结束X坐标
        drawEndX = totalWidth - RIGHT_PUNCTUATION_WIDTH;
        drawStartX = LEFT_PUNCTUATION_WIDTH;

        // 决定实际显示的行数
        int displayRows = rowCount;
        if (forceMinRows && displayRows < minRows) {
            displayRows = minRows;
        }

        // 高度 = 顶部留白 + (行数 * 格子高度) + ((行数 - 1) * 行间距) + 底部留白（为字数标注留出空间）
        int desiredHeight = (int) (TOP_PADDING + displayRows * cellSize +
                (displayRows - 1) * ROW_SPACING + MARK_TEXT_SIZE + 20);

        // 考虑padding
        int height = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(totalWidth, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制白色背景（确保背景是纯白色）
        canvas.drawColor(Color.WHITE);

        // 决定实际显示的行数
        int displayRows = rowCount;
        if (forceMinRows && displayRows < minRows) {
            displayRows = minRows;
        }

        // 绘制网格（考虑顶部留白和左右留白）
        drawGrid(canvas, displayRows);

        // 绘制外边框（考虑顶部留白和左右留白）
        drawBorder(canvas, displayRows);

        // 绘制文字和标点（考虑顶部留白和左右留白）
        drawTextAndPunctuation(canvas);

        // 绘制字数标注（考虑顶部留白）
        drawWordCountMarks(canvas, displayRows);
    }

    private void drawGrid(Canvas canvas, int displayRows) {
        // 计算实际绘制区域（从左侧留白之后开始，到右侧留白之前结束）
        float startX = drawStartX + BORDER_WIDTH / 2f;
        float endX = drawEndX - BORDER_WIDTH / 2f;

        // 绘制每一行的网格
        for (int row = 0; row < displayRows; row++) {
            // 计算行的起始Y坐标（考虑顶部留白）
            float rowStartY = TOP_PADDING + row * (cellSize + ROW_SPACING) + BORDER_WIDTH / 2f;
            float rowEndY = rowStartY + cellSize;

            // 绘制竖线（每一行独立绘制）
            for (int i = 1; i < COLS; i++) {
                float x = drawStartX + i * cellSize;
                if (x >= startX && x <= endX) {
                    canvas.drawLine(x, rowStartY, x, rowEndY, gridPaint);
                }
            }

            // 绘制横线（每一行的上下边框）
            canvas.drawLine(startX, rowStartY, endX, rowStartY, gridPaint); // 上边框
            canvas.drawLine(startX, rowEndY, endX, rowEndY, gridPaint); // 下边框
        }
    }

    private void drawBorder(Canvas canvas, int displayRows) {
        // 绘制每一行的独立边框
        for (int row = 0; row < displayRows; row++) {
            // 计算行的起始和结束Y坐标（考虑顶部留白）
            float rowStartY = TOP_PADDING + row * (cellSize + ROW_SPACING);
            float rowEndY = rowStartY + cellSize;

            // 上边框
            canvas.drawLine(drawStartX, rowStartY, drawEndX, rowStartY, borderPaint);
            // 下边框
            canvas.drawLine(drawStartX, rowEndY, drawEndX, rowEndY, borderPaint);
            // 左边框
            canvas.drawLine(drawStartX, rowStartY, drawStartX, rowEndY, borderPaint);
            // 右边框
            canvas.drawLine(drawEndX, rowStartY, drawEndX, rowEndY, borderPaint);
        }
    }

    private void drawTextAndPunctuation(Canvas canvas) {
        float textSize = cellSize * 0.6f; // 文字大小为格子的60%
        textPaint.setTextSize(textSize);
        punctuationPaint.setTextSize(textSize);

        // 计算文字基线位置（垂直居中）
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float textOffset = (cellSize - textHeight) / 2f - fontMetrics.ascent;

        for (int i = 0; i < rowDataList.size(); i++) {
            TextProcessor.RowData rowData = rowDataList.get(i);
            List<String> grids = rowData.getGrids();

            // 计算Y坐标
            float y = TOP_PADDING + i * (cellSize + ROW_SPACING) + textOffset;

            // 绘制左侧标点（如果有）
            if (rowData.hasLeftPunctuation()) {
                float leftPunctX = drawStartX / 2f; // 左侧留白区域中心
                float leftPunctY = y;
                canvas.drawText(rowData.getLeftPunctuation(), leftPunctX, leftPunctY, punctuationPaint);
            }

            // 绘制格子内的文字
            for (int j = 0; j < grids.size() && j < COLS; j++) {
                String grid = grids.get(j);

                // 计算该格子的X起始位置（从左侧留白之后开始）
                float cellX = drawStartX + j * cellSize;

                if (!grid.equals("　")) { // 如果不是空格才绘制
                    // 计算整个格子的中心位置
                    float cellCenterX = cellX + cellSize / 2f;

                    // 计算文字宽度
                    float textWidth = textPaint.measureText(grid);

                    // 计算X坐标，使文字在格子中居中
                    float x = cellCenterX - textWidth / 2f;

                    canvas.drawText(grid, x, y, textPaint);
                }
            }

            // 绘制右侧标点（如果有）
            if (rowData.hasRightPunctuation()) {
                float rightPunctX = drawEndX + RIGHT_PUNCTUATION_WIDTH / 2f; // 右侧留白区域中心
                float rightPunctY = y;
                canvas.drawText(rowData.getRightPunctuation(), rightPunctX, rightPunctY, punctuationPaint);
            }
        }
    }

    private void drawWordCountMarks(Canvas canvas, int displayRows) {
        if (displayRows == 0) return;

        // 每5行标注一次：100, 200, 300, ...
        for (int row = 3; row < displayRows; row += 4) { // row 4 是第5行（从0开始计数）
            // 标注位置在最后一格的中心
            float x = drawStartX + (COLS - 0.5f) * cellSize;

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

        // 决定实际显示的行数
        int displayRows = rowCount;
        if (forceMinRows && displayRows < minRows) {
            displayRows = minRows;
        }

        // 绘制网格（考虑顶部留白）
        drawGrid(canvas, displayRows);

        // 绘制外边框（考虑顶部留白）
        drawBorder(canvas, displayRows);

        // 绘制文字和标点（考虑顶部留白）
        drawTextAndPunctuation(canvas);

        // 绘制字数标注（考虑顶部留白）
        drawWordCountMarks(canvas, displayRows);

        return bitmap;
    }

    /**
     * 获取顶部留白大小
     */
    public int getTopPadding() {
        return TOP_PADDING;
    }

    /**
     * 获取左侧留白大小
     */
    public float getLeftPunctuationWidth() {
        return LEFT_PUNCTUATION_WIDTH;
    }

    /**
     * 获取右侧留白大小
     */
    public float getRightPunctuationWidth() {
        return RIGHT_PUNCTUATION_WIDTH;
    }

    /**
     * 获取当前实际显示的行数
     */
    public int getDisplayRows() {
        if (forceMinRows && rowCount < minRows) {
            return minRows;
        }
        return rowCount;
    }
}