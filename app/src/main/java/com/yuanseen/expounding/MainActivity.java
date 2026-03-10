package com.yuanseen.expounding;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 100;

    private RedGridPaperView redGridPaper;
    private RecyclerView recyclerView;
    private ParagraphAdapter adapter;
    private List<ParagraphItem> paragraphs;
    private Button btnAddParagraph;
    private Button btnApply;
    private Button btnExportText;
    private Button btnExportImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupListeners();
        checkPermission();
    }

    private void initViews() {
        redGridPaper = findViewById(R.id.redGridPaper);
        recyclerView = findViewById(R.id.recyclerView);
        btnAddParagraph = findViewById(R.id.btnAddParagraph);
        btnApply = findViewById(R.id.btnApply);
        btnExportText = findViewById(R.id.btnExportText);
        btnExportImage = findViewById(R.id.btnExportImage);
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
            }

            @Override
            public void onAlignmentChanged(int position, TextAlignment alignment) {
                paragraphs.get(position).setAlignment(alignment);
            }

            @Override
            public void onTextChanged(int position, String text) {
                paragraphs.get(position).setText(text);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddParagraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paragraphs.add(new ParagraphItem("", TextAlignment.LEFT));
                adapter.notifyItemInserted(paragraphs.size() - 1);
                recyclerView.smoothScrollToPosition(paragraphs.size() - 1);
            }
        });

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 确保所有EditText的内容都已保存
                saveAllEditTextChanges();
                // 应用排版
                redGridPaper.setParagraphs(paragraphs);
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
    }

    private void saveAllEditTextChanges() {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof ParagraphAdapter.ViewHolder) {
                ParagraphAdapter.ViewHolder viewHolder = (ParagraphAdapter.ViewHolder) holder;
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    paragraphs.get(position).setText(
                            viewHolder.editText.getText().toString()
                    );
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

        // 构建文本内容
        StringBuilder content = new StringBuilder();
        for (ParagraphItem paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text != null && !text.isEmpty()) {
                content.append(text).append("\n\n");
            }
        }

        if (content.length() == 0) {
            Toast.makeText(this, "没有可导出的文本内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "作文文本_" + timeStamp + ".txt";

        try {
            File file;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore 保存到 Downloads 目录
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
                        fos.write(content.toString().getBytes());
                        Toast.makeText(this, "文本已导出到：Downloads/" + fileName, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Android 9 及以下保存到外部存储
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                file = new File(downloadsDir, fileName);
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content.toString());
                    Toast.makeText(this, "文本已导出到：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportImageToFile() {
        // 先保存所有编辑框的内容并更新视图
        saveAllEditTextChanges();
        redGridPaper.setParagraphs(paragraphs);

        // 等待视图绘制完成
        redGridPaper.post(new Runnable() {
            @Override
            public void run() {
                // 创建Bitmap
                Bitmap bitmap = Bitmap.createBitmap(
                        redGridPaper.getWidth(),
                        redGridPaper.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

                // 将视图绘制到Bitmap上
                Canvas canvas = new Canvas(bitmap);
                redGridPaper.draw(canvas);

                // 生成文件名
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "作文纸_" + timeStamp + ".png";

                try {
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
                        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        if (!picturesDir.exists()) {
                            picturesDir.mkdirs();
                        }
                        File file = new File(picturesDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            Toast.makeText(MainActivity.this, "图片已导出到：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    bitmap.recycle();
                }
            }
        });
    }
}