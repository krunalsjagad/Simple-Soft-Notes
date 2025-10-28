package com.example.softnotesandcanvas.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.softnotesandcanvas.R;
// ✅ Import the single, consolidated Note model from the 'db' package
import com.example.softnotesandcanvas.db.Note;

import java.util.Objects;

// ✅ Updated ListAdapter to use the consolidated 'db.Note' model
public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {

    public NoteAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView content;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            itemView.setOnClickListener(v -> {
                // Optional: open editor. Implement as needed in your app.
            });
        }

        void bind(Note note) {
            title.setText(note.title != null ? note.title : "");
            // ✅ Use the standardized 'content' field
            content.setText(note.content != null ? note.content : "");
        }
    }

    // ✅ Updated DiffUtil.ItemCallback to use 'db.Note' and correct logic
    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            // ✅ Use .equals() for String comparison
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            // ✅ Use Objects.equals() for robust comparison of all fields
            return Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.content, newItem.content)
                    && Objects.equals(oldItem.updatedAt, newItem.updatedAt) // ✅ Use .equals() for Dates
                    && oldItem.isDeleted == newItem.isDeleted
                    && oldItem.syncStatus == newItem.syncStatus;
        }
    };
}
