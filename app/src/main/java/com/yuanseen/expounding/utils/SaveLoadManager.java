package com.yuanseen.expounding.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yuanseen.expounding.ui.ParagraphItem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SaveLoadManager {
    private static final String TAG = "SaveLoadManager";
    private static final String SAVE_DIR = "expounding_saves";
    private static final String FILE_EXTENSION = ".json";
    private final Context context;
    private final Gson gson;

    public SaveLoadManager(Context context) {
        this.context = context;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * 获取保存目录
     */
    private File getSaveDirectory() {
        File saveDir = new File(context.getExternalFilesDir(null), SAVE_DIR);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        return saveDir;
    }

    /**
     * 保存段落列表到文件
     * @param paragraphs 段落列表
     * @param fileName 文件名（不含扩展名）
     * @return 是否保存成功
     */
    public boolean saveToFile(List<ParagraphItem> paragraphs, String fileName) {
        try {
            // 确保文件名有效
            String safeFileName = sanitizeFileName(fileName);
            File file = new File(getSaveDirectory(), safeFileName + FILE_EXTENSION);

            // 转换为JSON
            String json = gson.toJson(paragraphs);

            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }

            Log.d(TAG, "保存成功: " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "保存失败", e);
            return false;
        }
    }

    /**
     * 从文件加载段落列表
     * @param fileName 文件名（不含扩展名）
     * @return 段落列表，失败返回null
     */
    public List<ParagraphItem> loadFromFile(String fileName) {
        try {
            String safeFileName = sanitizeFileName(fileName);
            File file = new File(getSaveDirectory(), safeFileName + FILE_EXTENSION);

            if (!file.exists()) {
                Log.e(TAG, "文件不存在: " + fileName);
                return null;
            }

            // 读取JSON
            StringBuilder jsonBuilder = new StringBuilder();
            try (FileReader reader = new FileReader(file)) {
                int ch;
                while ((ch = reader.read()) != -1) {
                    jsonBuilder.append((char) ch);
                }
            }

            // 解析JSON
            Type listType = new TypeToken<ArrayList<ParagraphItem>>() {}.getType();
            List<ParagraphItem> paragraphs = gson.fromJson(jsonBuilder.toString(), listType);

            Log.d(TAG, "加载成功: " + file.getAbsolutePath());
            return paragraphs;
        } catch (IOException e) {
            Log.e(TAG, "加载失败", e);
            return null;
        }
    }

    /**
     * 获取所有保存的文件名列表
     * @return 文件名列表（不含扩展名）
     */
    public List<String> getSavedFileNames() {
        List<String> fileNames = new ArrayList<>();
        File[] files = getSaveDirectory().listFiles((dir, name) -> name.endsWith(FILE_EXTENSION));

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                fileNames.add(name.substring(0, name.length() - FILE_EXTENSION.length()));
            }
        }
        return fileNames;
    }

    /**
     * 删除保存的文件
     * @param fileName 文件名（不含扩展名）
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        File file = new File(getSaveDirectory(), safeFileName + FILE_EXTENSION);
        return file.exists() && file.delete();
    }

    /**
     * 检查文件是否存在
     * @param fileName 文件名（不含扩展名）
     * @return 是否存在
     */
    public boolean fileExists(String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        File file = new File(getSaveDirectory(), safeFileName + FILE_EXTENSION);
        return file.exists();
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未命名_" + System.currentTimeMillis();
        }
        // 替换Windows和Linux中的非法文件名字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}