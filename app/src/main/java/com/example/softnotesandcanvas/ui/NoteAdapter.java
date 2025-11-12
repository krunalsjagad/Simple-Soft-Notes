package com.example.softnotesandcanvas.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.softnotesandcanvas.R;
import com.example.softnotesandcanvas.db.Note;

import java.io.File;
import java.util.Objects;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Note note);
    }
    private final OnItemClickListener listener;

    public NoteAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView content;
        private final ImageView canvasPreview; // <-- Add this

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            canvasPreview = itemView.findViewById(R.id.note_canvas_preview); // <-- Find this
        }

        void bind(final Note note, final OnItemClickListener listener) {
            title.setText(note.title != null ? note.title : "");

            // --- THIS IS THE NEW LOGIC ---
            // Check note type. Default to TEXT if type is null (for old notes)
            if (note.type == null || note.type.equals(Note.TYPE_TEXT)) {
                // This is a TEXT note
                content.setVisibility(View.VISIBLE);
                canvasPreview.setVisibility(View.GONE);

                if (note.content != null && !note.content.isEmpty()) {
                    // Render the HTML
                    content.setText(Html.fromHtml(note.content, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    content.setText(""); // Clear old content
                }

            } else if (note.type.equals(Note.TYPE_CANVAS)) {
                // This is a CANVAS note
                content.setVisibility(View.GONE);
                canvasPreview.setVisibility(View.VISIBLE);

                // Set a placeholder or clear old image
                canvasPreview.setImageResource(R.color.grey_200); // <-- Add a placeholder color in colors.xml

                // Load the bitmap
                if (note.canvasImagePath != null && !note.canvasImagePath.isEmpty()) {
                    try {
                        File file = new File(note.canvasImagePath);
                        if (file.exists()) {
                            // This is a quick load. For smoother scrolling,
                            // you could move this to a background thread or use a library like Glide/Picasso
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            canvasPreview.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        Log.e("NoteAdapter", "Error loading canvas preview", e);
                        // Show an error icon if it fails
                        canvasPreview.setImageResource(android.R.drawable.ic_menu_report_image);
                    }
                }
            }
            // --- END OF NEW LOGIC ---

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(note);
                }
            });
        }
    }

    // --- UPDATE THE DIFF CALLBACK ---
    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            // Compare all fields that affect the UI
            return Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.content, newItem.content)
                    && Objects.equals(oldItem.canvasImagePath, newItem.canvasImagePath)
                    && Objects.equals(oldItem.type, newItem.type)
                    && Objects.equals(oldItem.updatedAt, newItem.updatedAt)
                    && oldItem.isDeleted == newItem.isDeleted;
        }
    };
}