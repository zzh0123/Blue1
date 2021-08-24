package com.blue1.bean;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.blue1.R;


/**
 * Created by ZengZeHong on 2017/5/15.
 */

public class ToastHolder extends RecyclerView.ViewHolder{
    private TextView tvToast;
    public ToastHolder(View itemView) {
        super(itemView);
        tvToast = (TextView)itemView.findViewById(R.id.tv_toast);
    }

    public TextView getTvToast() {
        return tvToast;
    }

    public void setTvToast(TextView tvToast) {
        this.tvToast = tvToast;
    }
}