package com.yuanseen.expounding.utils;

import com.yuanseen.expounding.ui.ParagraphItem;
import com.yuanseen.expounding.ui.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class TextProcessor {

    private static final int ROW_LENGTH = 25; // 每行25格

    public static List<String> processParagraphs(List<ParagraphItem> paragraphs) {
        List<String> result = new ArrayList<>();

        for (ParagraphItem paragraph : paragraphs) {
            String text = paragraph.getText();
            TextAlignment alignment = paragraph.getAlignment();

            // 处理文本：保留空格但移除换行符
            String cleanText = text.replace("\n", "").replace("\r", "");

            // 如果文本为空，添加空行
            if (cleanText.isEmpty()) {
                result.add(createEmptyLine(alignment));
                continue;
            }

            // 将文本转换为格子序列
            List<String> gridList = convertToGridList(cleanText);

            // 按每行25格分行的逻辑
            int startIndex = 0;
            while (startIndex < gridList.size()) {
                int endIndex = Math.min(startIndex + ROW_LENGTH, gridList.size());
                List<String> rowGrids = gridList.subList(startIndex, endIndex);

                // 将格子列表合并为行字符串，格子之间用特殊分隔符标记
                StringBuilder rowBuilder = new StringBuilder();
                for (int i = 0; i < rowGrids.size(); i++) {
                    if (i > 0) {
                        rowBuilder.append("‖"); // 使用特殊分隔符标记格子边界
                    }
                    rowBuilder.append(rowGrids.get(i));
                }
                String rawLine = rowBuilder.toString();

                // 根据对齐方式处理每行
                String processedLine;
                int gridCount = rowGrids.size();

                switch (alignment) {
                    case CENTER:
                        processedLine = centerAlign(rawLine, gridCount);
                        break;
                    case RIGHT:
                        processedLine = rightAlign(rawLine, gridCount);
                        break;
                    case LEFT:
                    default:
                        processedLine = leftAlign(rawLine, gridCount);
                        break;
                }

                result.add(processedLine);
                startIndex += ROW_LENGTH;
            }
        }

        return result;
    }

    private static List<String> convertToGridList(String text) {
        List<String> gridList = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            // 判断是否为中文字符
            if (isChinese(c)) {
                gridList.add(String.valueOf(c));
                i++;
            } else {
                // 字母、数字，两个一组
                StringBuilder grid = new StringBuilder();
                grid.append(c);

                // 检查下一个字符
                if (i + 1 < text.length()) {
                    char nextChar = text.charAt(i + 1);
                    if (!isChinese(nextChar)) {
                        grid.append(nextChar);
                        i += 2;
                    } else {
                        i++;
                    }
                } else {
                    i++;
                }

                gridList.add(grid.toString());
            }
        }

        return gridList;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private static String createEmptyLine(TextAlignment alignment) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i > 0) line.append("‖");
            line.append("　");
        }
        return line.toString();
    }

    private static String leftAlign(String line, int gridCount) {
        StringBuilder result = new StringBuilder(line);
        for (int i = gridCount; i < ROW_LENGTH; i++) {
            if (result.length() > 0) result.append("‖");
            result.append("　");
        }
        return result.toString();
    }

    private static String centerAlign(String line, int gridCount) {
        int spacesNeeded = ROW_LENGTH - gridCount;
        int leftSpaces = spacesNeeded / 2;
        int rightSpaces = spacesNeeded - leftSpaces;

        StringBuilder result = new StringBuilder();

        // 左侧空格
        for (int i = 0; i < leftSpaces; i++) {
            if (result.length() > 0) result.append("‖");
            result.append("　");
        }

        // 内容
        if (result.length() > 0 && line.length() > 0) result.append("‖");
        result.append(line);

        // 右侧空格
        for (int i = 0; i < rightSpaces; i++) {
            result.append("‖");
            result.append("　");
        }

        return result.toString();
    }

    private static String rightAlign(String line, int gridCount) {
        StringBuilder result = new StringBuilder();

        // 左侧空格
        for (int i = gridCount; i < ROW_LENGTH; i++) {
            if (result.length() > 0) result.append("‖");
            result.append("　");
        }

        // 内容
        if (result.length() > 0 && line.length() > 0) result.append("‖");
        result.append(line);

        return result.toString();
    }
}