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

    // 定义不能出现在行首的标点符号
    private static final Set<Character> PUNCTUATION_CANNOT_START_LINE = new HashSet<>(Arrays.asList(
            '，', '。', '、', '；', '：', '？', '！', '”', '’', '）', '】', '》', '』', '〕', '}',
            ',', '.', ';', ':', '?', '!', ')', ']', '}'
    ));

    // 定义需要占两格的标点符号
    private static final Set<String> DOUBLE_GRID_PUNCTUATION = new HashSet<>(Arrays.asList(
            "——", "……"
    ));

    // 简化后的行数据类：只有格子内容和右侧标点
    public static class RowData {
        private List<String> grids; // 格子内的内容
        private String rightPunctuation; // 右侧格子外标点

        public RowData() {
            this.grids = new ArrayList<>();
            this.rightPunctuation = "";
        }

        public List<String> getGrids() {
            return grids;
        }

        public void setGrids(List<String> grids) {
            this.grids = grids;
        }

        public String getRightPunctuation() {
            return rightPunctuation;
        }

        public void setRightPunctuation(String rightPunctuation) {
            this.rightPunctuation = rightPunctuation;
        }

        public boolean hasRightPunctuation() {
            return rightPunctuation != null && !rightPunctuation.isEmpty();
        }

        // 转换为带标记的字符串用于显示
        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();

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

        for (int paraIdx = 0; paraIdx < paragraphs.size(); paraIdx++) {
            ParagraphItem paragraph = paragraphs.get(paraIdx);
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

            // 将文本转换为格子序列
            List<String> gridList = convertToGridList(cleanText);

            // 按每行25格分行，处理标点不能出现在行首的规则
            List<RowData> rows = splitIntoRows(gridList);

            // 根据对齐方式处理每行
            for (RowData row : rows) {
                applyAlignmentToRow(row, alignment);
                result.add(row);
            }
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
     * 将格子列表分行为每行25格，处理标点不能出现在行首的规则
     */
    private static List<RowData> splitIntoRows(List<String> gridList) {
        List<RowData> rows = new ArrayList<>();
        int currentIndex = 0;

        while (currentIndex < gridList.size()) {
            RowData row = new RowData();

            // 计算当前行可以取的格子数
            int endIndex = Math.min(currentIndex + ROW_LENGTH, gridList.size());
            List<String> rowGrids = new ArrayList<>(gridList.subList(currentIndex, endIndex));

            // 处理行首标点问题：如果当前行第一个格子是不能出现在行首的标点
            if (!rows.isEmpty() && !rowGrids.isEmpty() && isPunctuationCannotStartLine(rowGrids.get(0))) {
                // 把这个标点从当前行移除
                String punctuation = rowGrids.remove(0);
                // 放到上一行的右侧
                rows.get(rows.size() - 1).setRightPunctuation(punctuation);
                // 如果还有下一个格子，补进来
                if (endIndex < gridList.size()) {
                    rowGrids.add(gridList.get(endIndex));
                    endIndex++;
                }
            }

            // 如果当前行没有格子（理论上不会发生，但以防万一）
            if (rowGrids.isEmpty()) {
                currentIndex = endIndex;
                continue;
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
        if (gridCount == ROW_LENGTH) {
            return;
        }

        List<String> newGrids = new ArrayList<>();
        switch (alignment) {
            case CENTER:
                int spacesNeeded = ROW_LENGTH - gridCount;
                int leftSpaces = spacesNeeded / 2;
                int rightSpaces = spacesNeeded - leftSpaces;
                for (int i = 0; i < leftSpaces; i++) {
                    newGrids.add("　");
                }
                newGrids.addAll(row.getGrids());
                for (int i = 0; i < rightSpaces; i++) {
                    newGrids.add("　");
                }
                break;
            case RIGHT:
                for (int i = gridCount; i < ROW_LENGTH; i++) {
                    newGrids.add("　");
                }
                newGrids.addAll(row.getGrids());
                break;
            case LEFT:
            default:
                newGrids.addAll(row.getGrids());
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
            // 处理空格
            if (c == ' ' || c == '　') {
                gridList.add("　");
                i++;
                continue;
            }
            // 处理双字符标点
            if (c == '—' || c == '…') {
                if (i + 1 < text.length()) {
                    char nextChar = text.charAt(i + 1);
                    String doublePunct = String.valueOf(c) + nextChar;
                    if (doublePunct.equals("——") || doublePunct.equals("……")) {
                        gridList.add(doublePunct);
                        i += 2;
                        continue;
                    }
                }
            }
            // 处理中文字符
            if (isChinese(c)) {
                gridList.add(String.valueOf(c));
                i++;
            } else {
                // 英文/数字：两个一组占一格
                StringBuilder grid = new StringBuilder();
                grid.append(c);
                if (i + 1 < text.length()) {
                    char nextChar = text.charAt(i + 1);
                    if (!isChinese(nextChar) && nextChar != ' ' && nextChar != '　'
                            && nextChar != '—' && nextChar != '…') {
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
        // 检查双格标点
        if (DOUBLE_GRID_PUNCTUATION.contains(grid)) {
            return true;
        }
        // 检查单字符标点
        char firstChar = grid.charAt(0);
        return PUNCTUATION_CANNOT_START_LINE.contains(firstChar);
    }
}