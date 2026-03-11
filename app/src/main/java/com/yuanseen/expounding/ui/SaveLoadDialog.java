package com.yuanseen.expounding.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.yuanseen.expounding.R;
import com.yuanseen.expounding.utils.SaveLoadManager;

import java.util.List;

public class SaveLoadDialog {
    private final Context context;
    private final SaveLoadManager saveLoadManager;
    private final OnFileActionListener listener;
    private ArrayAdapter<String> fileListAdapter;
    private List<String> savedFiles;

    public interface OnFileActionListener {
        void onFileSaved(String fileName);
        void onFileLoaded(String fileName);
        void onFileDeleted(String fileName);
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
        ScrollView scrollViewFiles = view.findViewById(R.id.scrollViewFiles);
        ListView listViewFiles = view.findViewById(R.id.listViewFiles);
        TextView tvOverwriteWarning = view.findViewById(R.id.tvOverwriteWarning);

        // 加载已保存的文件列表
        savedFiles = saveLoadManager.getSavedFileNames();
        if (!savedFiles.isEmpty()) {
            tvExistingFiles.setVisibility(View.VISIBLE);
            scrollViewFiles.setVisibility(View.VISIBLE);
            listViewFiles.setVisibility(View.VISIBLE);

            fileListAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_list_item_1, savedFiles);
            listViewFiles.setAdapter(fileListAdapter);

            // 设置ListView的高度自适应
            setListViewHeightBasedOnChildren(listViewFiles);

            // 点击列表项填充文件名
            listViewFiles.setOnItemClickListener((parent, view1, position, id) -> {
                String fileName = savedFiles.get(position);
                editFileName.setText(fileName);
                tvOverwriteWarning.setVisibility(View.VISIBLE);
            });

            // 长按列表项删除文件
            listViewFiles.setOnItemLongClickListener((parent, view1, position, id) -> {
                String fileName = savedFiles.get(position);
                showDeleteConfirmDialog(fileName, position);
                return true;
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

        builder.setNeutralButton("刷新列表", (dialog, which) -> {
            // 刷新文件列表
            refreshFileList();
            // 重新显示对话框
            showSaveDialog();
        });

        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 显示加载对话框
     */
    public void showLoadDialog() {
        savedFiles = saveLoadManager.getSavedFileNames();

        if (savedFiles.isEmpty()) {
            Toast.makeText(context, "没有已保存的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("选择要加载的文件（长按可删除）");

        fileListAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, savedFiles);

        builder.setAdapter(fileListAdapter, (dialog, which) -> {
            String fileName = savedFiles.get(which);
            if (listener != null) {
                listener.onFileLoaded(fileName);
            }
        });

        // 创建对话框
        AlertDialog dialog = builder.create();
        dialog.show();

        // 获取ListView并设置长按监听
        ListView listView = dialog.getListView();
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String fileName = savedFiles.get(position);
            dialog.dismiss(); // 先关闭当前对话框
            showDeleteConfirmDialog(fileName, position);
            return true;
        });
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(String fileName, int position) {
        new AlertDialog.Builder(context)
                .setTitle("删除文件")
                .setMessage("确定要删除文件 \"" + fileName + "\" 吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    boolean success = saveLoadManager.deleteFile(fileName);
                    if (success) {
                        Toast.makeText(context, "文件已删除: " + fileName, Toast.LENGTH_SHORT).show();
                        // 刷新文件列表
                        refreshFileList();
                        // 如果有关注删除事件的监听器，调用它
                        if (listener != null) {
                            listener.onFileDeleted(fileName);
                        }
                    } else {
                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 刷新文件列表
     */
    private void refreshFileList() {
        savedFiles = saveLoadManager.getSavedFileNames();
        if (fileListAdapter != null) {
            fileListAdapter.clear();
            fileListAdapter.addAll(savedFiles);
            fileListAdapter.notifyDataSetChanged();
        }
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

    /**
     * 动态设置ListView的高度，使其能够正确显示在ScrollView中
     */
    private void setListViewHeightBasedOnChildren(ListView listView) {
        if (fileListAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < fileListAdapter.getCount(); i++) {
            View listItem = fileListAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (fileListAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}