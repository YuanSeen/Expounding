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

        for (int pIndex = 0; pIndex < paragraphs.size(); pIndex++) {
            ParagraphItem paragraph = paragraphs.get(pIndex);
            String text = paragraph.getText();
            TextAlignment alignment = paragraph.getAlignment();

            // 处理文本：保留空格但移除换行符
            String cleanText = text.replace("\n", "").replace("\r", "");

            // 将文本转换为格子序列
            List<String> gridList = convertToGridList(cleanText);

            // 按每行25格分行的逻辑，考虑标点不能出现在行首的规则
            List<RowData> rows = splitIntoRowsWithPunctuationRule(gridList, pendingLeftPunctuation);

            // 更新待处理的标点（最后一行的右侧标点）
            if (!rows.isEmpty()) {
                RowData lastRow = rows.get(rows.size() - 1);
                pendingLeftPunctuation = lastRow.getRightPunctuation();
                lastRow.setRightPunctuation(""); // 清除最后一行的右侧标点

                // 根据对齐方式处理每行
                for (RowData row : rows) {
                    applyAlignmentToRow(row, alignment);
                }

                // **新增：如果是最后一段，并且有pending标点，创建一个新行**
                if (pIndex == paragraphs.size() - 1 && !pendingLeftPunctuation.isEmpty()) {
                    RowData lastRowWithPunct = new RowData();
                    lastRowWithPunct.setLeftPunctuation(pendingLeftPunctuation);
                    // 添加空格子
                    for (int i = 0; i < ROW_LENGTH; i++) {
                        lastRowWithPunct.getGrids().add("　");
                    }
                    result.add(lastRowWithPunct);
                    pendingLeftPunctuation = "";
                } else {
                    result.addAll(rows);
                }
            } else {
                // **新增：处理空段落**
                // 如果文本为空且没有待处理的标点，才添加空行
                if (cleanText.isEmpty() && pendingLeftPunctuation.isEmpty()) {
                    RowData emptyRow = createEmptyRow();
                    result.add(emptyRow);
                }
                // 如果文本为空但有pending标点，创建一个带左侧标点的行
                else if (cleanText.isEmpty() && !pendingLeftPunctuation.isEmpty()) {
                    RowData rowWithPunct = new RowData();
                    rowWithPunct.setLeftPunctuation(pendingLeftPunctuation);
                    for (int i = 0; i < ROW_LENGTH; i++) {
                        rowWithPunct.getGrids().add("　");
                    }
                    applyAlignmentToRow(rowWithPunct, alignment);
                    result.add(rowWithPunct);
                    pendingLeftPunctuation = "";
                }
            }
        }

        // **移除原来的最后处理代码，因为已经在循环中处理了**
        // 如果最后还有待处理的标点，创建一行空行来显示它
        // if (!pendingLeftPunctuation.isEmpty()) { ... }

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
        String pendingPunctuation = initialLeftPunctuation; // 待处理的左侧标点

        while (currentIndex < gridList.size()) {
            RowData row = new RowData();

            // 如果有待处理的左侧标点，先设置
            if (!pendingPunctuation.isEmpty()) {
                row.setLeftPunctuation(pendingPunctuation);
                pendingPunctuation = "";
            }

            // 获取这一行应该有的格子数
            int targetCount = ROW_LENGTH;

            // 计算这一行的结束索引
            int endIndex = Math.min(currentIndex + targetCount, gridList.size());

            // 获取当前行的格子
            List<String> rowGrids = new ArrayList<>(gridList.subList(currentIndex, endIndex));

            // 检查当前行是否以不能放在行首的标点开头（且不是第一行）
            if (!rows.isEmpty() && !rowGrids.isEmpty() && isPunctuationCannotStartLine(rowGrids.get(0))) {
                // 将这个标点放到上一行的右侧
                String punctuation = rowGrids.remove(0);
                rows.get(rows.size() - 1).setRightPunctuation(punctuation);

                // 调整结束索引：因为少了一个格子，所以可以多取一个
                if (endIndex < gridList.size()) {
                    // 如果能多取一个，就加上
                    rowGrids.add(gridList.get(endIndex));
                    endIndex++;
                }
            }

            // **新增：检查当前行是否为空行（没有格子且没有左侧标点）**
            // 如果当前行没有格子，但有左侧标点，仍然需要创建行
            // 如果既没有格子也没有左侧标点，则跳过这一行
            if (rowGrids.isEmpty() && !row.hasLeftPunctuation()) {
                // 这一行是空的，跳过它，继续处理下一批格子
                currentIndex = endIndex;
                continue;
            }

            row.setGrids(rowGrids);
            rows.add(row);

            // 更新当前索引到结束位置
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