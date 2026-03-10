package com.yuanseen.expounding.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuanseen.expounding.R;
import java.util.List;

public class ParagraphAdapter extends RecyclerView.Adapter<ParagraphAdapter.ViewHolder> {
    private List<ParagraphItem> paragraphs;
    private static OnParagraphActionListener listener;

    public interface OnParagraphActionListener {
        void onRemoveClick(int position);
        void onSplitClick(int position, int cursorPosition);
        void onAlignmentChanged(int position, TextAlignment alignment);
        void onTextChanged(int position, String text);
    }

    public ParagraphAdapter(List<ParagraphItem> paragraphs, OnParagraphActionListener listener) {
        this.paragraphs = paragraphs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paragraph, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParagraphItem item = paragraphs.get(position);

        // 设置文本，避免触发TextWatcher
        holder.editText.removeTextChangedListener(holder.textWatcher);
        holder.editText.setText(item.getText());
        holder.editText.setHint("请输入第" + (position + 1) + "段文本...");
        holder.editText.addTextChangedListener(holder.textWatcher);

        // 设置对齐方式
        holder.radioGroup.setOnCheckedChangeListener(null); // 先移除监听器
        switch (item.getAlignment()) {
            case LEFT:
                holder.radioLeft.setChecked(true);
                break;
            case CENTER:
                holder.radioCenter.setChecked(true);
                break;
            case RIGHT:
                holder.radioRight.setChecked(true);
                break;
        }

        // 设置对齐方式监听器
        holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            TextAlignment alignment;
            if (checkedId == R.id.radioLeft) {
                alignment = TextAlignment.LEFT;
            } else if (checkedId == R.id.radioCenter) {
                alignment = TextAlignment.CENTER;
            } else {
                alignment = TextAlignment.RIGHT;
            }
            if (listener != null) {
                listener.onAlignmentChanged(holder.getAdapterPosition(), alignment);
            }
        });

        // 删除按钮
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemoveClick(pos);
            }
        });

        // 分段按钮
        holder.btnSplit.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                int cursorPos = holder.editText.getSelectionStart();
                listener.onSplitClick(pos, cursorPos);
            }
        });

        // 如果只有一个段落，禁用删除按钮
        holder.btnRemove.setEnabled(paragraphs.size() > 1);
    }

    @Override
    public int getItemCount() {
        return paragraphs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText editText;
        RadioGroup radioGroup;
        RadioButton radioLeft;
        RadioButton radioCenter;
        RadioButton radioRight;
        Button btnRemove;
        Button btnSplit;
        public TextWatcher textWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.editText);
            radioGroup = itemView.findViewById(R.id.alignmentGroup);
            radioLeft = itemView.findViewById(R.id.radioLeft);
            radioCenter = itemView.findViewById(R.id.radioCenter);
            radioRight = itemView.findViewById(R.id.radioRight);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnSplit = itemView.findViewById(R.id.btnSplit);

            // 创建TextWatcher
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // 不需要处理
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // 不需要处理
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // 这里回调文本变化
                    if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        listener.onTextChanged(getAdapterPosition(), s.toString());
                    }
                }
            };
        }
    }
}