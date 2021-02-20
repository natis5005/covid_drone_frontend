package com.CovidDrone.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.CovidDrone.R;
import com.CovidDrone.UseCases.Request;


import java.util.ArrayList;

public class RequestRecyclerAdapter extends RecyclerView.Adapter<RequestRecyclerAdapter.ViewHolder>{

    private ArrayList<Request> mRequests;
    private RequestRecyclerClickListener mRequestRecyclerClickListener;

    public RequestRecyclerAdapter(ArrayList<Request> requests, RequestRecyclerClickListener requestRecyclerClickListener) {
        this.mRequests = requests;
        mRequestRecyclerClickListener = requestRecyclerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chatroom_list_item, parent, false);
        return new ViewHolder(view, mRequestRecyclerClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        (holder).requestTitle.setText(mRequests.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return mRequests.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        TextView requestTitle;
        RequestRecyclerClickListener clickListener;

        public ViewHolder(View itemView, RequestRecyclerClickListener clickListener) {
            super(itemView);
            requestTitle = itemView.findViewById(R.id.chatroom_title);
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onRequestSelected(getAdapterPosition());
        }
    }

    public interface RequestRecyclerClickListener {
        void onRequestSelected(int position);
    }
}

















