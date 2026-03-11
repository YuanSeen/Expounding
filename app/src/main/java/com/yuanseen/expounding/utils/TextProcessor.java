package com.yuanseen.expounding.utils;

import com.yuanseen.expounding.ui.ParagraphItem;
import com.yuanseen.expounding.ui.TextAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextProcessor {

    private static final int ROW_LENGTH = 25; // 每行25格

    // 定义不能出现在行首的标点符号（这些符号需要留在上一行的格子外）
    private static final Set<Character> PUNCTUATION_CANNOT_START_LINE = new HashSet<>(Arrays.asList(
            '，', '。', '、', '；', '：', '？', '！', '”', '’', '）', '】', '》', '』', '〕', '}',
            ',', '.', ';', ':', '?', '!', ')', ']', '}'
    ));

    // 定义必须作为独立格子处理的标点（如引号、书名号等）
    private static final Set<Character> PUNCTUATION_SPECIAL = new HashSet<>(Arrays.asList(
            '“', '”', '‘', '’', '（', '）', '【', '】', '《', '》', '〈', '〉', '〔', '〕', '{', '}',
            '"', '\'', '(', ')', '[', ']'
    ));

    // 定义需要占两格的标点符号
    private static final Set<String> DOUBLE_GRID_PUNCTUATION = new HashSet<>(Arrays.asList(
            "——", "……"
    ));

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

            // 将文本转换为格子序列（每个格子是一个字符或两个英文字符或双标点）
            List<String> gridList = convertToGridList(cleanText);

            // 按每行25格分行的逻辑，考虑标点不能出现在行首的规则
            List<String> rows = splitIntoRowsWithPunctuationRule(gridList);

            // 根据对齐方式处理每行
            for (String row : rows) {
                result.add(applyAlignment(row, alignment));
            }
        }

        return result;
    }

    /**
     * 将格子列表按行分割，同时处理标点不能出现在行首的规则
     */
    private static List<String> splitIntoRowsWithPunctuationRule(List<String> gridList) {
        List<String> rows = new ArrayList<>();
        int currentIndex = 0;

        while (currentIndex < gridList.size()) {
            int endIndex = Math.min(currentIndex + ROW_LENGTH, gridList.size());

            // 检查是否需要调整行尾，避免标点出现在下一行行首
            if (endIndex < gridList.size()) {
                // 获取下一行的第一个格子
                String nextGrid = gridList.get(endIndex);

                // 检查下一行第一个格子是否是"不能出现在行首的标点"
                // 如果是，则将这个格子移动到当前行
                if (isPunctuationCannotStartLine(nextGrid)) {
                    // 增加结束索引，将这个格子包含在当前行
                    endIndex++;

                    // 如果这样导致当前行超过25格，需要特殊处理
                    if (endIndex - currentIndex > ROW_LENGTH + 1) {
                        // 这种情况很少见，但以防万一，我们最多允许当前行26格
                        // 但实际上，这种情况应该不会发生，因为标点通常只有一个字符
                        endIndex = currentIndex + ROW_LENGTH;
                    }
                }
            }

            // 获取当前行的格子子列表
            List<String> rowGrids = gridList.subList(currentIndex, Math.min(endIndex, gridList.size()));

            // 将格子列表合并为行字符串，格子之间用特殊分隔符标记
            StringBuilder rowBuilder = new StringBuilder();
            for (int i = 0; i < rowGrids.size(); i++) {
                if (i > 0) {
                    rowBuilder.append("‖");
                }
                rowBuilder.append(rowGrids.get(i));
            }

            rows.add(rowBuilder.toString());
            currentIndex = endIndex;
        }

        return rows;
    }

    /**
     * 判断一个格子是否包含不能出现在行首的标点
     */
    private static boolean isPunctuationCannotStartLine(String grid) {
        if (grid == null || grid.isEmpty()) {
            return false;
        }

        // 检查是否是占两格的标点
        if (DOUBLE_GRID_PUNCTUATION.contains(grid)) {
            return true;
        }

        // 获取格子的第一个字符
        char firstChar = grid.charAt(0);

        // 检查是否是标点符号
        return PUNCTUATION_CANNOT_START_LINE.contains(firstChar);
    }

    /**
     * 根据对齐方式处理一行
     */
    private static String applyAlignment(String line, TextAlignment alignment) {
        // 计算当前行的格子数量
        int gridCount = line.isEmpty() ? 0 : line.split("\\‖").length;

        switch (alignment) {
            case CENTER:
                return centerAlign(line, gridCount);
            case RIGHT:
                return rightAlign(line, gridCount);
            case LEFT:
            default:
                return leftAlign(line, gridCount);
        }
    }

    private static List<String> convertToGridList(String text) {
        List<String> gridList = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            // 处理空格 - 空格单独占一个格子
            if (c == ' ' || c == '　') { // 半角空格或全角空格
                gridList.add("　"); // 统一使用全角空格
                i++;
                continue;
            }

            // 检查是否是破折号（——）或省略号（……）
            if (c == '—' || c == '…') {
                // 检查是否是双标点
                if (i + 1 < text.length()) {
                    char nextChar = text.charAt(i + 1);
                    String doublePunct = String.valueOf(c) + nextChar;

                    // 如果是破折号或省略号
                    if (doublePunct.equals("——") || doublePunct.equals("……")) {
                        gridList.add(doublePunct);
                        i += 2;
                        continue;
                    }
                }
            }

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
                    if (!isChinese(nextChar) && nextChar != ' ' && nextChar != '　' &&
                            nextChar != '—' && nextChar != '…') {
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