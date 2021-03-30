package com.example.musicplayer.view.main.download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.musicplayer.R;
import com.example.musicplayer.adapter.TabAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * <pre>
 *     author : 残渊
 *     time   : 2019/09/16
 *     desc   : 下载模块
 * </pre>
 */

public class DownloadFragment extends Fragment {
    @BindView(R.id.backIv)
    ImageView backIv;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.page)
    ViewPager page;
    private Unbinder unbinder;
    private List<String> mTitleList;
    private List<Fragment> mFragments;
    private TabAdapter mAdapter;
    private String[] mTitles = {"已下载", "正在下载"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        unbinder = ButterKnife.bind(this, view);
        initTab();
        onClick();
        return view;

    }

    private void initTab() {
        mTitleList = new ArrayList<>();
        mFragments = new ArrayList<>();
        mTitleList.addAll(Arrays.asList(mTitles));
        mFragments.add(new DownloadMusicFragment());
        mFragments.add(new DownloadingFragment());
        mAdapter = new TabAdapter(getChildFragmentManager(), mFragments, mTitleList);
        page.setAdapter(mAdapter);
        tabLayout.setupWithViewPager(page);
    }
    private void onClick(){
        backIv.setOnClickListener(view -> Objects.requireNonNull(getActivity()).getSupportFragmentManager().popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
