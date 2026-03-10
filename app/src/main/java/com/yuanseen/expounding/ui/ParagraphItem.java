package com.yuanseen.expounding.ui;

public class ParagraphItem {
    private String text;
    private TextAlignment alignment;

    public ParagraphItem(String text, TextAlignment alignment) {
        this.text = text;
        this.alignment = alignment;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TextAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment;
    }
}