package com.yuanseen.expounding;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuanseen.expounding.ui.ParagraphAdapter;
import com.yuanseen.expounding.ui.ParagraphItem;
import com.yuanseen.expounding.ui.RedGridPaperView;
import com.yuanseen.expounding.ui.TextAlignment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.yuanseen.expounding.ui.SaveLoadDialog;
import com.yuanseen.expounding.utils.SaveLoadManager;
import android.app.AlertDialog;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final long AUTO_APPLY_DELAY = 500; // 延迟500ms后应用排版，避免频繁刷新

    private RedGridPaperView redGridPaper;
    private RecyclerView recyclerView;
    private ParagraphAdapter adapter;
    private List<ParagraphItem> paragraphs;
    private Button btnAddParagraph;
    private Button btnExportText;
    private Button btnExportImage;

    private Handler autoApplyHandler = new Handler(Looper.getMainLooper());
    private Runnable autoApplyRunnable;
    private Button btnSave;
    private Button btnLoad;
    private SaveLoadManager saveLoadManager;
    private SaveLoadDialog saveLoadDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupListeners();
        checkPermission();

        // 初始化保存管理
        saveLoadManager = new SaveLoadManager(this);
        saveLoadDialog = new SaveLoadDialog(this, new SaveLoadDialog.OnFileActionListener() {
            @Override
            public void onFileSaved(String fileName) {
                saveToFile(fileName);
            }

            @Override
            public void onFileLoaded(String fileName) {
                loadFromFile(fileName);
            }
        });

        // 设置 RedGridPaperView 最小显示4行
        redGridPaper.setMinRows(4);
        redGridPaper.setForceMinRows(true);

        // 初始应用一次，显示4行空白格
        redGridPaper.setParagraphs(paragraphs);

        // 初始化自动应用排版任务
        autoApplyRunnable = new Runnable() {
            @Override
            public void run() {
                applyParagraphsToPaper();
            }
        };
    }

    private void initViews() {
        redGridPaper = findViewById(R.id.redGridPaper);
        recyclerView = findViewById(R.id.recyclerView);
        btnAddParagraph = findViewById(R.id.btnAddParagraph);
        btnExportText = findViewById(R.id.btnExportText);
        btnExportImage = findViewById(R.id.btnExportImage);

        // 新增保存和加载按钮
        btnSave = findViewById(R.id.btnSave);
        btnLoad = findViewById(R.id.btnLoad);
    }

    private void setupRecyclerView() {
        paragraphs = new ArrayList<>();
        // 默认添加一个段落
        paragraphs.add(new ParagraphItem("", TextAlignment.LEFT));

        adapter = new ParagraphAdapter(paragraphs, new ParagraphAdapter.OnParagraphActionListener() {
            @Override
            public void onRemoveClick(int position) {
                paragraphs.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, paragraphs.size());

                // 段落被删除后，自动应用排版
                applyParagraphsToPaper();
            }

            @Override
            public void onSplitClick(int position, int cursorPosition) {
                splitParagraph(position, cursorPosition);
            }

            @Override
            public void onAlignmentChanged(int position, TextAlignment alignment) {
                paragraphs.get(position).setAlignment(alignment);
                // 对齐方式改变，自动应用排版
                applyParagraphsToPaper();
            }

            @Override
            public void onTextChanged(int position, String text) {
                paragraphs.get(position).setText(text);
                // 延迟应用排版，避免快速输入时频繁刷新
                autoApplyHandler.removeCallbacks(autoApplyRunnable);
                autoApplyHandler.postDelayed(autoApplyRunnable, AUTO_APPLY_DELAY);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    /**
     * 在指定位置拆分段落
     * @param position 当前段落的位置
     * @param cursorPosition 光标位置，用于拆分文本
     */
    private void splitParagraph(int position, int cursorPosition) {
        if (position < 0 || position >= paragraphs.size()) {
            return;
        }

        ParagraphItem currentParagraph = paragraphs.get(position);
        String currentText = currentParagraph.getText();

        // 如果光标位置无效（例如没有焦点），默认为文本末尾
        if (cursorPosition < 0 || cursorPosition > currentText.length()) {
            cursorPosition = currentText.length();
        }

        // 获取前半部分和后半部分文本
        String firstPart = currentText.substring(0, cursorPosition);
        String secondPart = currentText.substring(cursorPosition);

        // 更新当前段落为前半部分
        currentParagraph.setText(firstPart);

        // 立即更新当前项的 EditText 显示
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder instanceof ParagraphAdapter.ViewHolder) {
            ParagraphAdapter.ViewHolder viewHolder = (ParagraphAdapter.ViewHolder) holder;
            // 临时移除 TextWatcher，避免触发 onTextChanged
            viewHolder.editText.removeTextChangedListener(viewHolder.textWatcher);
            viewHolder.editText.setText(firstPart);
            viewHolder.editText.addTextChangedListener(viewHolder.textWatcher);
            // 将光标移到末尾
            viewHolder.editText.setSelection(firstPart.length());
        }

        // 在后半部分创建新段落（使用当前段落的对齐方式）
        ParagraphItem newParagraph = new ParagraphItem(secondPart, currentParagraph.getAlignment());

        // 在当前位置后面插入新段落
        paragraphs.add(position + 1, newParagraph);

        // 通知适配器插入新项
        adapter.notifyItemInserted(position + 1);
        adapter.notifyItemRangeChanged(position, paragraphs.size() - position);

        // 滚动到新段落
        recyclerView.smoothScrollToPosition(position + 1);

        // 清除任何待处理的自动应用排版任务
        autoApplyHandler.removeCallbacks(autoApplyRunnable);

        // 立即应用排版
        applyParagraphsToPaper();

        // 可选：显示提示信息
        Toast.makeText(this, "已分段", Toast.LENGTH_SHORT).show();
    }
    private void setupListeners() {
        btnAddParagraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paragraphs.add(new ParagraphItem("", TextAlignment.LEFT));
                adapter.notifyItemInserted(paragraphs.size() - 1);
                recyclerView.smoothScrollToPosition(paragraphs.size() - 1);

                // 当添加段落后，自动应用排版
                applyParagraphsToPaper();
            }
        });

        btnExportText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportTextToFile();
            }
        });

        btnExportImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportImageToFile();
            }
        });

        // 新增保存和加载按钮监听器
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 先保存所有编辑框的内容
                saveAllEditTextChanges();
                // 显示保存对话框
                saveLoadDialog.showSaveDialog();
            }
        });

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLoadDialog.showLoadDialog();
            }
        });
    }

    /**
     * 保存到文件
     */
    private void saveToFile(String fileName) {
        // 再次确保数据是最新的
        saveAllEditTextChanges();

        // 检查文件是否已存在
        if (saveLoadManager.fileExists(fileName)) {
            saveLoadDialog.showOverwriteConfirmDialog(fileName, () -> {
                performSave(fileName);
            });
        } else {
            performSave(fileName);
        }
    }

    private void performSave(String fileName) {
        boolean success = saveLoadManager.saveToFile(paragraphs, fileName);
        if (success) {
            Toast.makeText(this, "保存成功: " + fileName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从文件加载
     */
    private void loadFromFile(String fileName) {
        List<ParagraphItem> loadedParagraphs = saveLoadManager.loadFromFile(fileName);

        if (loadedParagraphs != null) {
            // 清空当前段落
            paragraphs.clear();

            // 添加加载的段落
            paragraphs.addAll(loadedParagraphs);

            // 确保至少有一个段落
            if (paragraphs.isEmpty()) {
                paragraphs.add(new ParagraphItem("", TextAlignment.LEFT));
            }

            // 通知适配器更新
            adapter.notifyDataSetChanged();

            // 应用排版
            applyParagraphsToPaper();

            Toast.makeText(this, "加载成功: " + fileName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 将段落应用到作文纸视图
     */
    private void applyParagraphsToPaper() {
        // 确保所有EditText的内容都已保存
        saveAllEditTextChanges();
        // 应用排版
        redGridPaper.setParagraphs(paragraphs);
    }

    private void saveAllEditTextChanges() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof ParagraphAdapter.ViewHolder) {
                ParagraphAdapter.ViewHolder viewHolder = (ParagraphAdapter.ViewHolder) holder;
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String editTextContent = viewHolder.editText.getText().toString();
                    String paragraphContent = paragraphs.get(position).getText();

                    // 只有当内容不同时才更新，避免不必要的刷新
                    if (!editTextContent.equals(paragraphContent)) {
                        paragraphs.get(position).setText(editTextContent);
                    }
                }
            }
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要存储权限，使用MediaStore
            return;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能导出文件", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void exportTextToFile() {
        // 先保存所有编辑框的内容
        saveAllEditTextChanges();

        // 构建文本内容 - 移除段落间的空行
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < paragraphs.size(); i++) {
            ParagraphItem paragraph = paragraphs.get(i);
            String text = paragraph.getText();
            if (text != null && !text.isEmpty()) {
                content.append(text);
                // 如果不是最后一段，添加一个换行符（不分段，只换行）
                if (i < paragraphs.size() - 1) {
                    content.append("\n");
                }
            }
        }

        if (content.length() == 0) {
            Toast.makeText(this, "没有可导出的文本内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 复制到剪贴板
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("作文文本", content.toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();

        // 可选：同时显示文本预览
        String previewText = content.length() > 50 ?
                content.substring(0, 50) + "..." : content.toString();
        Toast.makeText(this, "预览: " + previewText, Toast.LENGTH_LONG).show();
    }

    private void exportImageToFile() {
        // 先保存所有编辑框的内容并更新视图
        saveAllEditTextChanges();
        redGridPaper.setParagraphs(paragraphs);

        // 等待视图绘制完成
        redGridPaper.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // 使用专门的方法创建导出用的Bitmap（确保白色背景）
                    Bitmap bitmap = redGridPaper.createExportBitmap();

                    // 生成文件名
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "作文纸_" + timeStamp + ".png";

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ 使用 MediaStore 保存到 Pictures 目录
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                        android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                Toast.makeText(MainActivity.this, "图片已导出到：Pictures/" + fileName, Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        // Android 9 及以下保存到外部存储
                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED) {

                            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            if (!picturesDir.exists()) {
                                picturesDir.mkdirs();
                            }
                            File file = new File(picturesDir, fileName);
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                Toast.makeText(MainActivity.this, "图片已导出到：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "需要存储权限才能导出图片", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除所有回调，避免内存泄漏
        if (autoApplyHandler != null) {
            autoApplyHandler.removeCallbacksAndMessages(null);
        }
    }
}