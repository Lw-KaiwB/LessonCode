package com.kb.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kb.R;
import com.kb.model.HomeItemInfo;
import com.kb.util.Utils;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ArrayList<HomeItemInfo> itemInfos;
    private Class[] classs = {ScalableImageViewActivity.class};
    private int[] titleId = {R.string.title_scalable_image_view};
    private int[] messageId = {R.string.message_scalable_image_view};
    private String[] date = {"L11 20180726"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        mRecyclerView.setAdapter(new MyAdapter());
    }

    private void init() {
        itemInfos = new ArrayList<>();
        for (int i = 0; i < titleId.length; i++) {
            HomeItemInfo info = new HomeItemInfo();
            info.setTitleId(titleId[i]);
            if (i < messageId.length) {
                info.setMessageId(messageId[i]);
            } else {
                info.setMessageId(titleId[i]);
            }

            if (i < date.length) {
                info.setDate(date[i]);
            } else {
                info.setDate(Utils.getCurrentDateString());
            }

            info.setaClass(classs[i]);
            itemInfos.add(info);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_home, parent, false);
            return new HolderView(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            HolderView holderView = (HolderView) holder;
            final HomeItemInfo info = itemInfos.get(position);
            holderView.titleView.setText(info.getTitleId());
            holderView.messageView.setText(info.getMessageId());
            holderView.dateView.setText(info.getDate());
            holderView.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, info.getaClass());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemInfos == null ? 0 : itemInfos.size();
        }

        private class HolderView extends RecyclerView.ViewHolder {
            private LinearLayout itemView;
            private TextView titleView;
            private TextView messageView;
            private TextView dateView;

            public HolderView(View itemView) {
                super(itemView);
                this.itemView = (LinearLayout) itemView;//.findViewById(R.id.item_view_home_parent);
                titleView = itemView.findViewById(R.id.item_view_home_title);
                messageView = itemView.findViewById(R.id.item_view_home_message);
                dateView = itemView.findViewById(R.id.item_view_home_date);
            }
        }
    }
}
