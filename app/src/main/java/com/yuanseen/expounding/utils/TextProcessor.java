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

    // 定义行数据类，包含格子内容和格子外标点
    public static class RowData {
        private List<String> grids; // 格子内的内容
        private String leftPunctuation; // 左侧格子外标点（上一行遗留的标点）
        private String rightPunctuation; // 右侧格子外标点（需要放到下一行的标点）

        public RowData() {
            this.grids = new ArrayList<>();
            this.leftPunctuation = "";
            this.rightPunctuation = "";
        }

        public List<String> getGrids() {
            return grids;
        }

        public void setGrids(List<String> grids) {
            this.grids = grids;
        }

        public String getLeftPunctuation() {
            return leftPunctuation;
        }

        public void setLeftPunctuation(String leftPunctuation) {
            this.leftPunctuation = leftPunctuation;
        }

        public String getRightPunctuation() {
            return rightPunctuation;
        }

        public void setRightPunctuation(String rightPunctuation) {
            this.rightPunctuation = rightPunctuation;
        }

        public boolean hasLeftPunctuation() {
            return leftPunctuation != null && !leftPunctuation.isEmpty();
        }

        public boolean hasRightPunctuation() {
            return rightPunctuation != null && !rightPunctuation.isEmpty();
        }

        // 转换为带标记的字符串用于显示（保留原有格式以兼容现有代码）
        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();

            // 添加左侧标点标记（用特殊符号标记，后续在View中解析）
            if (hasLeftPunctuation()) {
                sb.append("←").append(leftPunctuation).append("←");
            }

            // 添加格子内容
            for (int i = 0; i < grids.size(); i++) {
                if (i > 0) sb.append("‖");
                sb.append(grids.get(i));
            }

            // 添加右侧标点标记
            if (hasRightPunctuation()) {
                sb.append("→").append(rightPunctuation).append("→");
            }

            return sb.toString();
        }
    }

    public static List<RowData> processParagraphsToRows(List<ParagraphItem> paragraphs) {
        List<RowData> result = new ArrayList<>();
        String pendingLeftPunctuation = ""; // 待处理的标点（需要放在下一行左侧）

        for (ParagraphItem paragraph : paragraphs) {
            String text = paragraph.getText();
            TextAlignment alignment = paragraph.getAlignment();

            // 处理文本：保留空格但移除换行符
            String cleanText = text.replace("\n", "").replace("\r", "");

            // 如果文本为空，添加空行
            if (cleanText.isEmpty()) {
                RowData emptyRow = createEmptyRow();
                result.add(emptyRow);
                continue;
            }

            // 将文本转换为格子序列（每个格子是一个字符或两个英文字符或双标点）
            List<String> gridList = convertToGridList(cleanText);

            // 按每行25格分行的逻辑，考虑标点不能出现在行首的规则
            List<RowData> rows = splitIntoRowsWithPunctuationRule(gridList, pendingLeftPunctuation);

            // 更新待处理的标点（最后一行的右侧标点）
            if (!rows.isEmpty()) {
                RowData lastRow = rows.get(rows.size() - 1);
                pendingLeftPunctuation = lastRow.getRightPunctuation();
                lastRow.setRightPunctuation(""); // 清除最后一行的右侧标点，因为它是下一段的开始
            }

            // 根据对齐方式处理每行
            for (RowData row : rows) {
                applyAlignmentToRow(row, alignment);
                result.add(row);
            }
        }

        // 如果最后还有待处理的标点，创建一行空行来显示它
        if (!pendingLeftPunctuation.isEmpty()) {
            RowData lastRow = new RowData();
            lastRow.setLeftPunctuation(pendingLeftPunctuation);
            // 添加空格子
            for (int i = 0; i < ROW_LENGTH; i++) {
                lastRow.getGrids().add("　");
            }
            result.add(lastRow);
        }

        return result;
    }

    /**
     * 创建空行
     */
    private static RowData createEmptyRow() {
        RowData row = new RowData();
        for (int i = 0; i < ROW_LENGTH; i++) {
            row.getGrids().add("　");
        }
        return row;
    }

    /**
     * 将格子列表按行分割，同时处理标点不能出现在行首的规则
     */
    private static List<RowData> splitIntoRowsWithPunctuationRule(List<String> gridList, String initialLeftPunctuation) {
        List<RowData> rows = new ArrayList<>();
        int currentIndex = 0;
        String pendingRightPunctuation = ""; // 待处理的右侧标点

        while (currentIndex < gridList.size()) {
            RowData row = new RowData();

            // 计算这一行能放多少个格子
            int endIndex = Math.min(currentIndex + ROW_LENGTH, gridList.size());

            // 获取当前行的格子
            List<String> rowGrids = new ArrayList<>(gridList.subList(currentIndex, endIndex));

            // 检查是否需要将标点移到上一行的右侧
            // 如果当前行第一个字符是标点，说明上一行满了，这个标点应该放在上一行的右侧
            if (!rows.isEmpty() && !rowGrids.isEmpty() && isPunctuationCannotStartLine(rowGrids.get(0))) {
                // 将第一个标点移到上一行的右侧
                String punctuation = rowGrids.remove(0);
                rows.get(rows.size() - 1).setRightPunctuation(punctuation);

                // 重新计算结束索引，因为少了一个格子
                endIndex = currentIndex + 1; // 跳过了第一个标点，所以从下一个开始
                rowGrids = new ArrayList<>(gridList.subList(currentIndex + 1, Math.min(currentIndex + 1 + ROW_LENGTH, gridList.size())));
            }

            row.setGrids(rowGrids);
            rows.add(row);

            currentIndex = endIndex;
        }

        return rows;
    }

    /**
     * 根据对齐方式处理一行
     */
    private static void applyAlignmentToRow(RowData row, TextAlignment alignment) {
        int gridCount = row.getGrids().size();

        // 如果格子数已经是25，不需要对齐处理
        if (gridCount == ROW_LENGTH) {
            return;
        }

        List<String> newGrids = new ArrayList<>();

        switch (alignment) {
            case CENTER:
                int spacesNeeded = ROW_LENGTH - gridCount;
                int leftSpaces = spacesNeeded / 2;
                int rightSpaces = spacesNeeded - leftSpaces;

                // 左侧空格
                for (int i = 0; i < leftSpaces; i++) {
                    newGrids.add("　");
                }
                // 内容
                newGrids.addAll(row.getGrids());
                // 右侧空格
                for (int i = 0; i < rightSpaces; i++) {
                    newGrids.add("　");
                }
                break;

            case RIGHT:
                // 左侧空格
                for (int i = gridCount; i < ROW_LENGTH; i++) {
                    newGrids.add("　");
                }
                // 内容
                newGrids.addAll(row.getGrids());
                break;

            case LEFT:
            default:
                // 内容
                newGrids.addAll(row.getGrids());
                // 右侧空格
                for (int i = gridCount; i < ROW_LENGTH; i++) {
                    newGrids.add("　");
                }
                break;
        }

        row.setGrids(newGrids);
    }

    public static List<String> processParagraphs(List<ParagraphItem> paragraphs) {
        List<RowData> rows = processParagraphsToRows(paragraphs);
        List<String> result = new ArrayList<>();
        for (RowData row : rows) {
            result.add(row.toDisplayString());
        }
        return result;
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
}