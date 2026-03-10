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

            // 按25格分行的逻辑
            int startIndex = 0;
            while (startIndex < cleanText.length()) {
                int endIndex = Math.min(startIndex + ROW_LENGTH, cleanText.length());
                String rawLine = cleanText.substring(startIndex, endIndex);

                // 根据对齐方式处理每行
                String processedLine;
                switch (alignment) {
                    case CENTER:
                        processedLine = centerAlign(rawLine);
                        break;
                    case RIGHT:
                        processedLine = rightAlign(rawLine);
                        break;
                    case LEFT:
                    default:
                        processedLine = leftAlign(rawLine);
                        break;
                }

                result.add(processedLine);
                startIndex += ROW_LENGTH;
            }
        }

        return result;
    }

    private static String createEmptyLine(TextAlignment alignment) {
        switch (alignment) {
            case CENTER:
                return centerAlign("");
            case RIGHT:
                return rightAlign("");
            case LEFT:
            default:
                return leftAlign("");
        }
    }

    private static String leftAlign(String line) {
        return line + repeatChar('　', ROW_LENGTH - line.length()); // 使用全角空格
    }

    private static String centerAlign(String line) {
        int spacesNeeded = ROW_LENGTH - line.length();
        int leftSpaces = spacesNeeded / 2;
        int rightSpaces = spacesNeeded - leftSpaces;

        return repeatChar('　', leftSpaces) + line + repeatChar('　', rightSpaces);
    }

    private static String rightAlign(String line) {
        return repeatChar('　', ROW_LENGTH - line.length()) + line;
    }

    private static String repeatChar(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}