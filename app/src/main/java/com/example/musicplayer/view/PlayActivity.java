package com.example.musicplayer.view;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.library.view.LrcView;
import com.example.musicplayer.R;
import com.example.musicplayer.app.Api;
import com.example.musicplayer.app.Constant;
import com.example.musicplayer.base.activity.BaseMvpActivity;
import com.example.musicplayer.contract.IPlayContract;
import com.example.musicplayer.entiy.DownloadInfo;
import com.example.musicplayer.entiy.DownloadSong;
import com.example.musicplayer.entiy.LocalSong;
import com.example.musicplayer.entiy.Song;
import com.example.musicplayer.event.DownloadEvent;
import com.example.musicplayer.event.SongCollectionEvent;
import com.example.musicplayer.event.SongStatusEvent;
import com.example.musicplayer.presenter.PlayPresenter;
import com.example.musicplayer.service.DownloadService;
import com.example.musicplayer.service.PlayerService;
import com.example.musicplayer.util.CommonUtil;
import com.example.musicplayer.util.DisplayUtil;
import com.example.musicplayer.util.FastBlurUtil;
import com.example.musicplayer.util.FileUtil;
import com.example.musicplayer.util.MediaUtil;
import com.example.musicplayer.util.ScreenUtil;
import com.example.musicplayer.widget.BackgroundAnimationRelativeLayout;
import com.example.musicplayer.widget.DiscView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.List;

import butterknife.BindView;

/**
 * ????????????
 */
public class PlayActivity extends BaseMvpActivity<PlayPresenter> implements IPlayContract.View {

    private final static String TAG = "PlayActivity";
    @BindView(R.id.tv_song)
    TextView mSongTv;
    @BindView(R.id.iv_back)
    ImageView mBackIv;
    @BindView(R.id.tv_singer)
    TextView mSingerTv;
    @BindView(R.id.btn_player)
    Button mPlayBtn;
    @BindView(R.id.btn_last)
    Button mLastBtn;
    @BindView(R.id.btn_order)
    Button mPlayModeBtn;
    @BindView(R.id.next)
    Button mNextBtn;
    @BindView(R.id.relative_root)
    BackgroundAnimationRelativeLayout mRootLayout;
    @BindView(R.id.btn_love)
    Button mLoveBtn;
    @BindView(R.id.seek)
    SeekBar mSeekBar;
    @BindView(R.id.tv_current_time)
    TextView mCurrentTimeTv;
    @BindView(R.id.tv_duration_time)
    TextView mDurationTimeTv;
    @BindView(R.id.disc_view)
    DiscView mDisc; //??????
    @BindView(R.id.iv_disc_background)
    ImageView mDiscImg; //????????????????????????
    @BindView(R.id.btn_get_img_lrc)
    Button mGetImgAndLrcBtn;//?????????????????????
    @BindView(R.id.lrcView)
    LrcView mLrcView; //???????????????View
    @BindView(R.id.downloadIv)
    ImageView mDownLoadIv; //??????

    private PlayPresenter mPresenter;


    private boolean isOnline; //???????????????????????????
    private int mListType; //????????????
    private int mPlayStatus;

    private int mPlayMode;//????????????

    private boolean isChange; //???????????????
    private boolean isSeek;//?????????????????????????????????????????????
    private boolean flag; //?????????????????????
    private int time;   //?????????????????????
    private boolean isPlaying;
    private Song mSong;
    private MediaPlayer mMediaPlayer;


    private RelativeLayout mPlayRelative;

    private String mLrc = null;
    private boolean isLove;//????????????????????????????????????
    private Bitmap mImgBmp;
    private List<LocalSong> mLocalSong;//?????????????????????????????????
    //??????
    private PlayerService.PlayStatusBinder mPlayStatusBinder;
    private DownloadService.DownloadBinder mDownloadBinder;

    //??????
    private ServiceConnection mPlayConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayStatusBinder = (PlayerService.PlayStatusBinder) service;
            //????????????
            mPlayMode = mPresenter.getPlayMode();//??????????????????
            mPlayStatusBinder.setPlayMode(mPlayMode);//????????????????????????

            isOnline = FileUtil.getSong().isOnline();
            if (isOnline) {
                mGetImgAndLrcBtn.setVisibility(View.GONE);
                setSingerImg(FileUtil.getSong().getImgUrl());
                if (mPlayStatus == Constant.SONG_PLAY) {
                    mDisc.play();
                    mPlayBtn.setSelected(true);
                    startUpdateSeekBarProgress();
                }
            } else {
                setLocalImg(mSong.getSinger());
                mSeekBar.setSecondaryProgress((int) mSong.getDuration());
            }
            mDurationTimeTv.setText(MediaUtil.formatTime(mSong.getDuration()));
            //???????????????
            mPlayStatusBinder.getMediaPlayer().setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mSeekBar.setSecondaryProgress(percent * mSeekBar.getProgress());
                }
            });
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mMusicHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!isChange) {
                mSeekBar.setProgress((int) mPlayStatusBinder.getCurrentTime());
                mCurrentTimeTv.setText(MediaUtil.formatTime(mSeekBar.getProgress()));
                startUpdateSeekBarProgress();
            }

        }
    };


    @Override
    protected void initView() {
        super.initView();
        EventBus.getDefault().register(this);
        CommonUtil.hideStatusBar(this, true);
        //????????????????????????
        getWindow().setEnterTransition(new Slide());
        getWindow().setExitTransition(new Slide());

        //??????????????????
        mPlayStatus = getIntent().getIntExtra(Constant.PLAYER_STATUS, 2);

        //????????????,????????????????????????
        Intent playIntent = new Intent(PlayActivity.this, PlayerService.class);
        Intent downIntent = new Intent(PlayActivity.this, DownloadService.class);
        bindService(playIntent, mPlayConnection, Context.BIND_AUTO_CREATE);
        bindService(downIntent, mDownloadConnection, Context.BIND_AUTO_CREATE);

        //????????????
        mSong = FileUtil.getSong();
        mListType = mSong.getListType();
        mSingerTv.setText(mSong.getSinger());
        mSongTv.setText(mSong.getSongName());
        mCurrentTimeTv.setText(MediaUtil.formatTime(mSong.getCurrentTime()));
        mSeekBar.setMax((int) mSong.getDuration());
        mSeekBar.setProgress((int) mSong.getCurrentTime());
        mDownLoadIv.setVisibility(mSong.isOnline() ? View.VISIBLE : View.GONE); //????????????????????????
        mDownLoadIv.setImageDrawable(mSong.isDownload() ? getDrawable(R.drawable.downloaded) : getDrawable(R.drawable.download_song));

        mPlayMode = mPresenter.getPlayMode();//??????????????????
        if (mPlayMode == Constant.PLAY_ORDER) {
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_order));
        } else if (mPlayMode == Constant.PLAY_RANDOM) {
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_random));
        } else {
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_single));
        }


    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadSuccessEvent(DownloadEvent event){
        if(event.getDownloadStatus() == Constant.TYPE_DOWNLOAD_SUCCESS){
            mDownLoadIv.setImageDrawable(
                    LitePal.where("songId=?", mSong.getSongId()).find(DownloadSong.class).size() != 0
                            ? getDrawable(R.drawable.downloaded)
                            : getDrawable(R.drawable.download_song));
        }
    }

    @Override
    protected PlayPresenter getPresenter() {
        //???Presenter????????????
        mPresenter = new PlayPresenter();
        return mPresenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void initData() {
        mPresenter.queryLove(mSong.getSongId()); //???????????????????????????????????????

        if (mPlayStatus == Constant.SONG_PLAY) {
            mDisc.play();
            mPlayBtn.setSelected(true);
            startUpdateSeekBarProgress();
        }
    }


    private void try2UpdateMusicPicBackground(final Bitmap bitmap) {
        new Thread(() -> {
            final Drawable drawable = getForegroundDrawable(bitmap);
            runOnUiThread(() -> {
                mRootLayout.setForeground(drawable);
                mRootLayout.beginAnimation();
            });
        }).start();
    }

    private Drawable getForegroundDrawable(Bitmap bitmap) {
        /*???????????????????????????????????????????????????????????????*/
        final float widthHeightSize = (float) (DisplayUtil.getScreenWidth(PlayActivity.this)
                * 1.0 / DisplayUtil.getScreenHeight(this) * 1.0);

        int cropBitmapWidth = (int) (widthHeightSize * bitmap.getHeight());
        int cropBitmapWidthX = (int) ((bitmap.getWidth() - cropBitmapWidth) / 2.0);

        /*??????????????????*/
        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, cropBitmapWidthX, 0, cropBitmapWidth,
                bitmap.getHeight());
        /*????????????*/
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(cropBitmap, bitmap.getWidth() / 50, bitmap
                .getHeight() / 50, false);
        /*?????????*/
        final Bitmap blurBitmap = FastBlurUtil.doBlur(scaleBitmap, 8, true);

        final Drawable foregroundDrawable = new BitmapDrawable(blurBitmap);
        /*????????????????????????????????????????????????????????????*/
        foregroundDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        return foregroundDrawable;
    }


    @Override
    protected void onClick() {
        //????????????
        mBackIv.setOnClickListener(v -> finish());
        //????????????????????????????????????
        mGetImgAndLrcBtn.setOnClickListener(v -> getSingerAndLrc());

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
                    mMediaPlayer = mPlayStatusBinder.getMediaPlayer();
                    mMediaPlayer.seekTo(seekBar.getProgress() * 1000);
                    startUpdateSeekBarProgress();
                } else {
                    time = seekBar.getProgress();
                    isSeek = true;
                }
                isChange = false;
                mCurrentTimeTv.setText(MediaUtil.formatTime(seekBar.getProgress()));
            }
        });

        mPlayModeBtn.setOnClickListener(v -> changePlayMode());

        //????????????????????????
        mPlayBtn.setOnClickListener(v -> {
            mMediaPlayer = mPlayStatusBinder.getMediaPlayer();
            if (mPlayStatusBinder.isPlaying()) {
                mPlayStatusBinder.pause();
                stopUpdateSeekBarProgress();
                flag = true;
                mPlayBtn.setSelected(false);
                mDisc.pause();
            } else if (flag) {
                mPlayStatusBinder.resume();
                flag = false;
                if (isSeek) {
                    Log.d(TAG, "onClick: " + time);
                    mMediaPlayer.seekTo(time * 1000);
                    isSeek = false;
                }
                mDisc.play();
                mPlayBtn.setSelected(true);
                startUpdateSeekBarProgress();
            } else {
                if (isOnline) {
                    mPlayStatusBinder.playOnline();
                } else {
                    mPlayStatusBinder.play(mListType);
                }
                mMediaPlayer.seekTo((int) mSong.getCurrentTime() * 1000);
                mDisc.play();
                mPlayBtn.setSelected(true);
                startUpdateSeekBarProgress();
            }
        });
        mNextBtn.setOnClickListener(v -> {
            mPlayStatusBinder.next();
            if (mPlayStatusBinder.isPlaying()) {
                mPlayBtn.setSelected(true);
            } else {
                mPlayBtn.setSelected(false);
            }
            mDisc.next();
        });
        mLastBtn.setOnClickListener(v -> {
            mPlayStatusBinder.last();
            mPlayBtn.setSelected(true);
            mDisc.last();
        });

        mLoveBtn.setOnClickListener(v -> {
            showLoveAnim();
            if (isLove) {
                mLoveBtn.setSelected(false);
                mPresenter.deleteFromLove(FileUtil.getSong().getSongId());
            } else {
                mLoveBtn.setSelected(true);
                mPresenter.saveToLove(FileUtil.getSong());
            }
            isLove = !isLove;
        });

        //??????????????????
        mDisc.setOnClickListener(v -> {
                    if (!isOnline) {
                        String lrc = FileUtil.getLrcFromNative(mSong.getSongName());
                        if (null == lrc) {
                            String qqId = mSong.getQqId();
                            if (Constant.SONG_ID_UNFIND.equals(qqId)) {//??????????????????
                                getLrcError(null);
                            } else if (null == qqId) {//?????????id????????????
                                mPresenter.getSongId(mSong.getSongName(), mSong.getDuration());
                            } else {//??????????????????
                                mPresenter.getLrc(qqId, Constant.SONG_LOCAL);
                            }
                        } else {
                            showLrc(lrc);
                        }
                    } else {
                        mPresenter.getLrc(mSong.getSongId(), Constant.SONG_ONLINE);
                    }
                }
        );
        //??????????????????
        mLrcView.setOnClickListener(v -> {
            mLrcView.setVisibility(View.GONE);
            mDisc.setVisibility(View.VISIBLE);
        });
        //????????????
        mDownLoadIv.setOnClickListener(v -> {
            if (mSong.isDownload()) {
                showToast(getString(R.string.downloded));
            } else {
                mDownloadBinder.startDownload(getDownloadInfoFromSong(mSong));
            }
        });

    }

    @Override
    public String getSingerName() {
        Song song = FileUtil.getSong();
        if (song.getSinger().contains("/")) {
            String[] s = song.getSinger().split("/");
            return s[0].trim();
        } else {
            return song.getSinger().trim();
        }

    }

    private String getSongName() {
        Song song = FileUtil.getSong();
        assert song != null;
        return song.getSongName().trim();
    }

    @Override
    public void getSingerAndLrc() {
        mGetImgAndLrcBtn.setText("????????????...");
        mPresenter.getSingerImg(getSingerName(), getSongName(), mSong.getDuration());
    }

    @Override
    public void setSingerImg(String ImgUrl) {
        Glide.with(this)
                .load(ImgUrl)
                .apply(RequestOptions.placeholderOf(R.drawable.welcome))
                .apply(RequestOptions.errorOf(R.drawable.welcome))
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mImgBmp = ((BitmapDrawable) resource).getBitmap();
                        //?????????????????????
                        if (!isOnline) {
                            //?????????????????????
                            FileUtil.saveImgToNative(PlayActivity.this, mImgBmp, getSingerName());
                            //?????????????????????????????????
                            LocalSong localSong = new LocalSong();
                            localSong.setPic(Api.STORAGE_IMG_FILE + FileUtil.getSong().getSinger() + ".jpg");
                            localSong.updateAll("songId=?", FileUtil.getSong().getSongId());
                        }

                        try2UpdateMusicPicBackground(mImgBmp);
                        setDiscImg(mImgBmp);
                        mGetImgAndLrcBtn.setVisibility(View.GONE);
                    }
                });

    }


    @Override
    public void showLove(final boolean love) {
        isLove = love;
        runOnUiThread(() -> {
            if (love) {
                mLoveBtn.setSelected(true);
            } else {
                mLoveBtn.setSelected(false);
            }
        });

    }

    @Override
    public void showLoveAnim() {
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(PlayActivity.this, R.animator.favorites_anim);
        animatorSet.setTarget(mLoveBtn);
        animatorSet.start();
    }

    @Override
    public void saveToLoveSuccess() {
        EventBus.getDefault().post(new SongCollectionEvent(true));
        CommonUtil.showToast(PlayActivity.this, getString(R.string.love_success));
    }

    @Override
    public void sendUpdateCollection() {
        EventBus.getDefault().post(new SongCollectionEvent(false));
    }


    //???????????????????????????
    private void setDiscImg(Bitmap bitmap) {
        mDiscImg.setImageDrawable(mDisc.getDiscDrawable(bitmap));
        int marginTop = (int) (DisplayUtil.SCALE_DISC_MARGIN_TOP * CommonUtil.getScreenHeight(PlayActivity.this));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mDiscImg
                .getLayoutParams();
        layoutParams.setMargins(0, marginTop, 0, 0);

        mDiscImg.setLayoutParams(layoutParams);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSongChanageEvent(SongStatusEvent event) {
        if (event.getSongStatus() == Constant.SONG_CHANGE) {
            mDisc.setVisibility(View.VISIBLE);
            mLrcView.setVisibility(View.GONE);
            mSong = FileUtil.getSong();
            mSongTv.setText(mSong.getSongName());
            mSingerTv.setText(mSong.getSinger());
            mDurationTimeTv.setText(MediaUtil.formatTime(mSong.getDuration()));
            mPlayBtn.setSelected(true);
            mSeekBar.setMax((int) mSong.getDuration());
            startUpdateSeekBarProgress();
            //???????????????
            mPlayStatusBinder.getMediaPlayer().setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mSeekBar.setSecondaryProgress(percent * mSeekBar.getProgress());
                }
            });
            mPresenter.queryLove(mSong.getSongId()); //???????????????????????????????????????
            if (mSong.isOnline()) {
                setSingerImg(mSong.getImgUrl());
            } else {
                setLocalImg(mSong.getSinger());//????????????
            }
        }
    }

    private void startUpdateSeekBarProgress() {
        /*??????????????????Message*/
        stopUpdateSeekBarProgress();
        mMusicHandler.sendEmptyMessageDelayed(0, 1000);
    }

    private void stopUpdateSeekBarProgress() {
        mMusicHandler.removeMessages(0);
    }

    private void setLocalImg(String singer) {
        String imgUrl = Api.STORAGE_IMG_FILE + MediaUtil.formatSinger(singer) + ".jpg";
        Glide.with(this)
                .load(imgUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        mGetImgAndLrcBtn.setVisibility(View.VISIBLE);
                        mGetImgAndLrcBtn.setText("?????????????????????");
                        setDiscImg(BitmapFactory.decodeResource(getResources(), R.drawable.default_disc));
                        mRootLayout.setBackgroundResource(R.drawable.background);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .apply(RequestOptions.placeholderOf(R.drawable.background))
                .apply(RequestOptions.errorOf(R.drawable.background))
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mGetImgAndLrcBtn.setVisibility(View.GONE);
                        mImgBmp = ((BitmapDrawable) resource).getBitmap();
                        try2UpdateMusicPicBackground(mImgBmp);
                        setDiscImg(mImgBmp);
                    }
                });
    }


    /**
     * ????????????
     *
     * @param lrc
     */
    @Override
    public void showLrc(final String lrc) {
        mDisc.setVisibility(View.GONE);
        mLrcView.setVisibility(View.VISIBLE);
        Log.d(TAG, "showLrc: "+mPlayStatusBinder.getMediaPlayer().getCurrentPosition());
        mLrcView.setLrc(lrc).setPlayer(mPlayStatusBinder.getMediaPlayer()).draw();

    }

    @Override
    public void getLrcError(String content) {
        showToast(getString(R.string.get_lrc_fail));
        mSong.setQqId(content);
        FileUtil.saveSong(mSong);
    }

    @Override
    public void setLocalSongId(String songId) {
        mSong.setQqId(songId);
        FileUtil.saveSong(mSong); //??????
    }

    @Override
    public void getSongIdSuccess(String songId) {
        setLocalSongId(songId);//??????????????????
        mPresenter.getLrc(songId, Constant.SONG_LOCAL);//????????????
    }

    @Override
    public void saveLrc(String lrc) {
        FileUtil.saveLrcToNative(lrc, mSong.getSongName());
    }


    //??????????????????
    private void changePlayMode() {
        View playModeView = LayoutInflater.from(this).inflate(R.layout.play_mode, null);
        ConstraintLayout orderLayout = playModeView.findViewById(R.id.orderLayout);
        ConstraintLayout randomLayout = playModeView.findViewById(R.id.randomLayout);
        ConstraintLayout singleLayout = playModeView.findViewById(R.id.singleLayout);
        TextView orderTv = playModeView.findViewById(R.id.orderTv);
        TextView randomTv = playModeView.findViewById(R.id.randomTv);
        TextView singleTv = playModeView.findViewById(R.id.singleTv);

        //????????????
        PopupWindow popupWindow = new PopupWindow(playModeView, ScreenUtil.dip2px(this, 130), ScreenUtil.dip2px(this, 150));
        //???????????????
        popupWindow.setBackgroundDrawable(getDrawable(R.color.transparent));
        //????????????
        popupWindow.setFocusable(true);
        //????????????????????????????????????
        popupWindow.setOutsideTouchable(true);
        popupWindow.update();
        //?????????????????????
        popupWindow.showAsDropDown(mPlayModeBtn, 0, -50);


        //??????????????????
        int mode = mPresenter.getPlayMode();
        if (mode == Constant.PLAY_ORDER) {
            orderTv.setSelected(true);
            randomTv.setSelected(false);
            singleTv.setSelected(false);
        } else if (mode == Constant.PLAY_RANDOM) {
            randomTv.setSelected(true);
            orderTv.setSelected(false);
            singleTv.setSelected(false);
        } else {
            singleTv.setSelected(true);
            randomTv.setSelected(false);
            orderTv.setSelected(false);
        }


        //????????????
        orderLayout.setOnClickListener(view -> {
            mPlayStatusBinder.setPlayMode(Constant.PLAY_ORDER); //????????????
            mPresenter.setPlayMode(Constant.PLAY_ORDER);
            popupWindow.dismiss();
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_order));

        });
        //????????????
        randomLayout.setOnClickListener(view -> {
            mPlayStatusBinder.setPlayMode(Constant.PLAY_RANDOM);
            mPresenter.setPlayMode(Constant.PLAY_RANDOM);
            popupWindow.dismiss();
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_random));
        });
        //????????????
        singleLayout.setOnClickListener(view -> {
            mPlayStatusBinder.setPlayMode(Constant.PLAY_SINGLE);
            mPresenter.setPlayMode(Constant.PLAY_SINGLE);
            popupWindow.dismiss();
            mPlayModeBtn.setBackground(getDrawable(R.drawable.play_mode_single));
        });


    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mPlayConnection);
        unbindService(mDownloadConnection);
        EventBus.getDefault().unregister(this);
        stopUpdateSeekBarProgress();

        //??????????????????
        mMusicHandler.removeCallbacksAndMessages(null);
    }

    private DownloadInfo getDownloadInfoFromSong(Song song){
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setSinger(song.getSinger());
        downloadInfo.setProgress(0);
        downloadInfo.setSongId(song.getSongId());
        downloadInfo.setUrl(song.getUrl());
        downloadInfo.setSongName(song.getSongName());
        downloadInfo.setDuration(song.getDuration());
        return downloadInfo;
    }

}
