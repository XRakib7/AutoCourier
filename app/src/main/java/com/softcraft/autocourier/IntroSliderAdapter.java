package com.softcraft.autocourier;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IntroSliderAdapter extends RecyclerView.Adapter<IntroSliderAdapter.SlideViewHolder> {

    private List<IntroSlide> slides;

    public IntroSliderAdapter(List<IntroSlide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        IntroSlide slide = slides.get(position);
        holder.icon.setImageResource(slide.getIconResId());
        holder.title.setText(slide.getTitle());
        holder.description.setText(slide.getDescription());
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, description;

        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivIcon);
            title = itemView.findViewById(R.id.tvTitle);
            description = itemView.findViewById(R.id.tvDescription);
        }
    }
}