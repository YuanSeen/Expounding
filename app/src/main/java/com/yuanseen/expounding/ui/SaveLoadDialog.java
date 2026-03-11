// 新增文件: SaveLoadDialog.java
package com.yuanseen.expounding.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yuanseen.expounding.R;
import com.yuanseen.expounding.utils.SaveLoadManager;

import java.util.List;

public class SaveLoadDialog {
    private final Context context;
    private final SaveLoadManager saveLoadManager;
    private final OnFileActionListener listener;

    public interface OnFileActionListener {
        void onFileSaved(String fileName);
        void onFileLoaded(String fileName);
    }

    public SaveLoadDialog(Context context, OnFileActionListener listener) {
        this.context = context;
        this.saveLoadManager = new SaveLoadManager(context);
        this.listener = listener;
    }

    /**
     * 显示保存对话框
     */
    public void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("保存作文");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_save_load, null);
        builder.setView(view);

        EditText editFileName = view.findViewById(R.id.editFileName);
        TextView tvExistingFiles = view.findViewById(R.id.tvExistingFiles);
        ListView listViewFiles = view.findViewById(R.id.listViewFiles);
        TextView tvOverwriteWarning = view.findViewById(R.id.tvOverwriteWarning);

        // 加载已保存的文件列表
        List<String> savedFiles = saveLoadManager.getSavedFileNames();
        if (!savedFiles.isEmpty()) {
            tvExistingFiles.setVisibility(View.VISIBLE);
            listViewFiles.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_list_item_1, savedFiles);
            listViewFiles.setAdapter(adapter);

            // 点击列表项填充文件名
            listViewFiles.setOnItemClickListener((parent, view1, position, id) -> {
                String fileName = savedFiles.get(position);
                editFileName.setText(fileName);
                tvOverwriteWarning.setVisibility(View.VISIBLE);
            });
        }

        // 文件名输入监听
        editFileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String fileName = s.toString().trim();
                if (saveLoadManager.fileExists(fileName)) {
                    tvOverwriteWarning.setVisibility(View.VISIBLE);
                } else {
                    tvOverwriteWarning.setVisibility(View.GONE);
                }
            }
        });

        builder.setPositiveButton("保存", (dialog, which) -> {
            String fileName = editFileName.getText().toString().trim();
            if (fileName.isEmpty()) {
                Toast.makeText(context, "请输入文件名", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onFileSaved(fileName);
            }
        });

        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 显示加载对话框
     */
    public void showLoadDialog() {
        List<String> savedFiles = saveLoadManager.getSavedFileNames();

        if (savedFiles.isEmpty()) {
            Toast.makeText(context, "没有已保存的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("选择要加载的文件");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, savedFiles);

        builder.setAdapter(adapter, (dialog, which) -> {
            String fileName = savedFiles.get(which);
            if (listener != null) {
                listener.onFileLoaded(fileName);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示覆盖确认对话框
     */
    public void showOverwriteConfirmDialog(String fileName, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle("文件已存在")
                .setMessage("文件 \"" + fileName + "\" 已存在，是否覆盖？")
                .setPositiveButton("覆盖", (dialog, which) -> onConfirm.run())
                .setNegativeButton("取消", null)
                .show();
    }
}