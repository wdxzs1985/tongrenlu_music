package info.tongrenlu.android.music;

import info.tongrenlu.android.loader.AlbumCoverLoader;
import info.tongrenlu.android.loader.JSONLoader;
import info.tongrenlu.android.music.adapter.MusicTrackListAdapter;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class MusicInfoActivity extends BaseActivity implements ActionSlideExpandableListView.OnActionClickListener, OnClickListener {

    public static final int ALBUM_COVER_LOADER = 0;
    public static final int ALBUM_INFO_LOADER = 1;

    private View mProgress = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private MusicTrackListAdapter mAdapter = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_music_info);

        final Intent intent = this.getIntent();
        String articleId = intent.getStringExtra("articleId");
        final String title = intent.getStringExtra("title");

        this.initAritcleTitle(title);

        this.mAdapter = new MusicTrackListAdapter();

        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (ActionSlideExpandableListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_download);

        final Button playAllButton = (Button) this.findViewById(R.id.action_play_all);
        playAllButton.setOnClickListener(this);
        final Button downloadAllButton = (Button) this.findViewById(R.id.action_download_all);
        downloadAllButton.setOnClickListener(this);

        Bundle bundle = new Bundle();
        bundle.putString("articleId", articleId);

        this.getSupportLoaderManager()
            .initLoader(ALBUM_COVER_LOADER,
                        bundle,
                        new AlbumCoverLoaderCallback());

        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.GONE);
        this.mEmpty.setVisibility(View.GONE);
        this.getSupportLoaderManager()
            .initLoader(ALBUM_INFO_LOADER,
                        bundle,
                        new TrackListLoaderCallback());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getSupportLoaderManager().destroyLoader(ALBUM_COVER_LOADER);
        this.getSupportLoaderManager().destroyLoader(ALBUM_INFO_LOADER);
    }

    private void initAritcleTitle(final String title) {
        final TextView articleTitle = (TextView) this.findViewById(R.id.article_title);
        articleTitle.setText(title);
    }

    @Override
    public void onClick(View itemView, View clickedView, int position) {
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            final TrackBean trackBean = (TrackBean) this.mListView.getItemAtPosition(position);
            this.playTrack(trackBean);
            break;
        case R.id.action_download:
            System.out.println("TODO download");
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(final View v) {
        if (!this.mAdapter.isEmpty()) {
            ArrayList<TrackBean> items = this.mAdapter.getItems();
            switch (v.getId()) {
            case R.id.action_play_all:
                this.playTrack(items);
                break;
            case R.id.action_download_all:
                // TODO download
                System.out.println("TODO download");
                break;
            default:
                break;
            }
        }
    }

    protected void playTrack(final TrackBean trackBean) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    protected void playTrack(final ArrayList<TrackBean> items) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList", items);
        serviceIntent.putExtra("position", 0);
        this.startService(serviceIntent);

        final Intent activityIntent = new Intent(this,
                                                 MusicPlayerActivity.class);
        this.startActivity(activityIntent);
    }

    private class AlbumCoverLoaderCallback implements LoaderCallbacks<Drawable> {

        @Override
        public Loader<Drawable> onCreateLoader(int loaderId, Bundle args) {
            switch (loaderId) {
            case ALBUM_COVER_LOADER:
                String articleId = args.getString("articleId");
                return new AlbumCoverLoader(MusicInfoActivity.this,
                                            articleId,
                                            HttpConstants.M_COVER);
            default:
                break;
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Drawable> loader, Drawable data) {
            if (loader instanceof AlbumCoverLoader) {
                final ImageView coverView = (ImageView) MusicInfoActivity.this.findViewById(R.id.article_cover);
                coverView.setImageDrawable(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Drawable> loader) {
            final ImageView coverView = (ImageView) MusicInfoActivity.this.findViewById(R.id.article_cover);
            coverView.setImageDrawable(null);
        }
    }

    private class TrackListLoaderCallback implements LoaderCallbacks<ArrayList<TrackBean>> {
        @Override
        public Loader<ArrayList<TrackBean>> onCreateLoader(int loaderId, Bundle args) {
            switch (loaderId) {
            case ALBUM_INFO_LOADER:
                String articleId = args.getString("articleId");
                final Uri uri = HttpConstants.getMusicInfoUri(MusicInfoActivity.this,
                                                              articleId);
                return new TrackListLoader(MusicInfoActivity.this, uri);
            default:
                break;
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<TrackBean>> loader, ArrayList<TrackBean> data) {
            if (CollectionUtils.isEmpty(data)) {
                MusicInfoActivity.this.mProgress.setVisibility(View.GONE);
                MusicInfoActivity.this.mListView.setVisibility(View.GONE);
                MusicInfoActivity.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                MusicInfoActivity.this.mProgress.setVisibility(View.GONE);
                MusicInfoActivity.this.mEmpty.setVisibility(View.GONE);
                MusicInfoActivity.this.mListView.setVisibility(View.VISIBLE);
                MusicInfoActivity.this.mAdapter.setItems(data);
                MusicInfoActivity.this.mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<TrackBean>> loader) {
            MusicInfoActivity.this.mAdapter.setItems(new ArrayList<TrackBean>());
        }
    }

    private static class TrackListLoader extends JSONLoader<ArrayList<TrackBean>> {

        public TrackListLoader(Context ctx, Uri uri) {
            super(ctx, uri, null);
        }

        @Override
        protected ArrayList<TrackBean> parseJSON(final JSONObject jsonData) throws JSONException {
            ArrayList<TrackBean> data = new ArrayList<TrackBean>();
            if (jsonData.getBoolean("result")) {
                final JSONArray items = jsonData.getJSONArray("playlist");
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject trackJSON = items.getJSONObject(i);
                    final TrackBean trackBean = new TrackBean();
                    if (trackJSON.has("articleId")) {
                        trackBean.setArticleId(trackJSON.getString("articleId"));
                    }

                    if (trackJSON.has("fileId")) {
                        trackBean.setFileId(trackJSON.getString("fileId"));
                    }

                    if (trackJSON.has("title")) {
                        trackBean.setTitle(trackJSON.getString("title"));
                    }

                    if (trackJSON.has("artist")) {
                        trackBean.setArtist(trackJSON.getString("artist"));
                    }
                    data.add(trackBean);
                }
            }
            return data;
        }
    }
}
