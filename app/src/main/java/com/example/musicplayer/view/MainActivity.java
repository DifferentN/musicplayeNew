package com.example.musicplayer.view;


import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.andexert.library.RippleView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.musicplayer.R;
import com.example.musicplayer.app.Constant;
import com.example.musicplayer.base.activity.BaseActivity;
import com.example.musicplayer.entiy.Song;
import com.example.musicplayer.event.OnlineSongErrorEvent;
import com.example.musicplayer.event.SongStatusEvent;
import com.example.musicplayer.service.DownloadService;
import com.example.musicplayer.service.PlayerService;
import com.example.musicplayer.util.CommonUtil;
import com.example.musicplayer.util.FileUtil;
import com.example.musicplayer.util.ServiceUtil;
import com.example.musicplayer.view.main.MainFragment;
import com.example.musicplayer.view.search.SearchContentFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.sb_progress)
    SeekBar mSeekBar;
    @BindView(R.id.tv_song_name)
    TextView mSongNameTv;
    @BindView(R.id.tv_singer)
    TextView mSingerTv;
    @BindView(R.id.song_next)
    RippleView mNextIv;
    @BindView(R.id.btn_player)
    Button mPlayerBtn;
    @BindView(R.id.circle_img)
    CircleImageView mCoverIv;
    @BindView(R.id.linear_player)
    LinearLayout mLinear;

    private boolean isChange; //???????????????
    private boolean isSeek;//?????????????????????????????????????????????
    private boolean flag; //?????????????????????
    private int time;   //?????????????????????

    private boolean isExistService;//??????????????????


    private ObjectAnimator mCircleAnimator;//??????
    private Song mSong;
    private MediaPlayer mMediaPlayer;
    private Thread mSeekBarThread;
    private PlayerService.PlayStatusBinder mPlayStatusBinder;
    private DownloadService.DownloadBinder mDownloadBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayStatusBinder = (PlayerService.PlayStatusBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {


        }
    };

    //??????????????????
    private ServiceConnection mDownloadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDownloadBinder = (DownloadService.DownloadBinder) iBinder;
            if(isExistService) seekBarStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        //??????
        unbindService(connection);
        unbindService(mDownloadConnection);

        //???????????????????????????????????????
        Intent playIntent = new Intent(MainActivity.this, PlayerService.class);
        //Android 8.0??????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(playIntent);
        } else {
            startService(playIntent);
        }


        EventBus.getDefault().unregister(this);
        if (mSeekBarThread != null || mSeekBarThread.isAlive()) mSeekBarThread.interrupt();
        Song song = FileUtil.getSong();
        song.setCurrentTime(mPlayStatusBinder.getCurrentTime());
        FileUtil.saveSong(song);


    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        LitePal.getDatabase();


        //??????????????????
        mCircleAnimator = ObjectAnimator.ofFloat(mCoverIv, "rotation", 0.0f, 360.0f);
        mCircleAnimator.setDuration(30000);
        mCircleAnimator.setInterpolator(new LinearInterpolator());
        mCircleAnimator.setRepeatCount(-1);
        mCircleAnimator.setRepeatMode(ValueAnimator.RESTART);


        mSong = FileUtil.getSong();
        if (mSong.getSongName() != null) {
            Log.d(TAG, "initView: " + mSong.toString());
            mLinear.setVisibility(View.VISIBLE);
            mSongNameTv.setText(mSong.getSongName());
            mSingerTv.setText(mSong.getSinger());
            mSeekBar.setMax((int) mSong.getDuration());
            mSeekBar.setProgress((int) mSong.getCurrentTime());
            if (mSong.getImgUrl() == null) {
                CommonUtil.setSingerImg(MainActivity.this, mSong.getSinger(), mCoverIv);
            } else {
                Glide.with(this)
                        .load(mSong.getImgUrl())
                        .apply(RequestOptions.placeholderOf(R.drawable.welcome))
                        .apply(RequestOptions.errorOf(R.drawable.welcome))
                        .into(mCoverIv);
            }
        } else {
            mSongNameTv.setText(getString(R.string.app_name));
            mSingerTv.setText(getString(R.string.welcome_start));
            mCoverIv.setImageResource(R.drawable.jay);
        }

        //???????????????????????????
        if(ServiceUtil.isServiceRunning(this,PlayerService.class.getName())){
            mPlayerBtn.setSelected(true);
            mCircleAnimator.start();
            isExistService = true;
        }

        //????????????
        initService();
        addMainFragment();
    }

    private void initService(){
        //????????????
        Intent playIntent = new Intent(MainActivity.this, PlayerService.class);
        Intent downIntent = new Intent(MainActivity.this, DownloadService.class);

        //??????????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(playIntent);
        }else {
            startService(playIntent);
        }
        bindService(playIntent, connection, Context.BIND_AUTO_CREATE);
        bindService(downIntent, mDownloadConnection, Context.BIND_AUTO_CREATE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOnlineSongErrorEvent(OnlineSongErrorEvent event) {
        showToast(getString(R.string.error_out_of_copyright));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSongStatusEvent(SongStatusEvent event) {
        int status = event.getSongStatus();
        if (status == Constant.SONG_RESUME) {
            mPlayerBtn.setSelected(true);
            mCircleAnimator.resume();
            seekBarStart();
        } else if (status == Constant.SONG_PAUSE) {
            mPlayerBtn.setSelected(false);
            mCircleAnimator.pause();
        } else if (status == Constant.SONG_CHANGE) {
            mSong = FileUtil.getSong();
            mSongNameTv.setText(mSong.getSongName());
            mSingerTv.setText(mSong.getSinger());
            mSeekBar.setMax((int) mSong.getDuration());
            mPlayerBtn.setSelected(true);
            mCircleAnimator.start();
            seekBarStart();
            if (!mSong.isOnline()) {
                CommonUtil.setSingerImg(MainActivity.this, mSong.getSinger(), mCoverIv);
            } else {
                Glide.with(MainActivity.this)
                        .load(mSong.getImgUrl())
                        .apply(RequestOptions.placeholderOf(R.drawable.welcome))
                        .apply(RequestOptions.errorOf(R.drawable.welcome))
                        .into(mCoverIv);
            }
        }

    }


    @Override
    protected void initData() {

    }

    @Override
    protected void onClick() {
        //????????????????????????
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //????????????????????????????????????????????????Thread???????????????????????????
                isChange = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlayStatusBinder.isPlaying()) {
                    mPlayStatusBinder.getMediaPlayer().seekTo(seekBar.getProgress() * 1000);
                } else {
                    time = seekBar.getProgress();
                    isSeek = true;
                }
                isChange = false;
                seekBarStart();
            }
        });

        //??????????????????????????????
        mPlayerBtn.setOnClickListener(v -> {
            mMediaPlayer = mPlayStatusBinder.getMediaPlayer();
            if (mPlayStatusBinder.isPlaying()) {
                time = mMediaPlayer.getCurrentPosition();
                mPlayStatusBinder.pause();
                flag = true;
            } else if (flag) {
                mPlayStatusBinder.resume();
                flag = false;
                if (isSeek) {
                    mMediaPlayer.seekTo(time * 1000);
                    isSeek = false;
                }
            } else {//???????????????????????????????????????
                if (FileUtil.getSong().isOnline()) {
                    mPlayStatusBinder.playOnline();
                } else {
                    mPlayStatusBinder.play(FileUtil.getSong().getListType());
                }
                mMediaPlayer = mPlayStatusBinder.getMediaPlayer();
                mMediaPlayer.seekTo((int) mSong.getCurrentTime() * 1000);
            }
        });
        //?????????
        mNextIv.setOnClickListener(v -> {
            if (FileUtil.getSong().getSongName() != null) mPlayStatusBinder.next();
            if (mPlayStatusBinder.isPlaying()) {
                mPlayerBtn.setSelected(true);
            } else {
                mPlayerBtn.setSelected(false);
            }
        });

        //?????????????????????????????????????????????
        mLinear.setOnClickListener(v -> {
            if (FileUtil.getSong().getSongName() != null) {
                Intent toPlayActivityIntent = new Intent(MainActivity.this, PlayActivity.class);

                //????????????
                if (mPlayStatusBinder.isPlaying()) {
                    Song song = FileUtil.getSong();
                    song.setCurrentTime(mPlayStatusBinder.getCurrentTime());
                    FileUtil.saveSong(song);
                    toPlayActivityIntent.putExtra(Constant.PLAYER_STATUS, Constant.SONG_PLAY);
                } else {
                    //????????????
                    Song song = FileUtil.getSong();
                    song.setCurrentTime(mSeekBar.getProgress());
                    FileUtil.saveSong(song);
                }
                if (FileUtil.getSong().getImgUrl() != null) {
                    toPlayActivityIntent.putExtra(SearchContentFragment.IS_ONLINE, true);
                }
                //??????????????????21
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(toPlayActivityIntent,
                            ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                } else {
                    startActivity(toPlayActivityIntent);
                }

            } else {
                showToast(getString(R.string.welcome_start));
            }
        });
    }

    private void addMainFragment() {
        MainFragment mainFragment = new MainFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, mainFragment);
        transaction.commit();
    }


    private void seekBarStart() {
        mSeekBarThread = new Thread(new SeekBarThread());
        mSeekBarThread.start();
    }

    class SeekBarThread implements Runnable {
        @Override
        public void run() {
            if (mPlayStatusBinder != null) {
                while (!isChange && mPlayStatusBinder.isPlaying()) {
                    mSeekBar.setProgress((int) mPlayStatusBinder.getCurrentTime());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
