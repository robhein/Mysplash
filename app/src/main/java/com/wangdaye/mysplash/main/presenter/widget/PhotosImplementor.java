package com.wangdaye.mysplash.main.presenter.widget;

import android.content.Context;

import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.common.data.api.PhotoApi;
import com.wangdaye.mysplash.common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash.common.data.service.PhotoService;
import com.wangdaye.mysplash.common.i.model.PhotosModel;
import com.wangdaye.mysplash.common.i.presenter.PhotosPresenter;
import com.wangdaye.mysplash.common.basic.activity.MysplashActivity;
import com.wangdaye.mysplash.common.ui.adapter.PhotoAdapter;
import com.wangdaye.mysplash.common.utils.helper.NotificationHelper;
import com.wangdaye.mysplash.common.utils.ValueUtils;
import com.wangdaye.mysplash.common.i.view.PhotosView;
import com.wangdaye.mysplash.main.model.widget.PhotosObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Photos implementor.
 *
 * */

public class PhotosImplementor
        implements PhotosPresenter {

    private PhotosModel model;
    private PhotosView view;

    private OnRequestPhotosListener listener;

    public PhotosImplementor(PhotosModel model, PhotosView view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void requestPhotos(Context c, int page, boolean refresh) {
        if (!model.isRefreshing() && !model.isLoading()) {
            if (refresh) {
                model.setRefreshing(true);
            } else {
                model.setLoading(true);
            }
            switch (model.getPhotosType()) {
                case PhotosObject.PHOTOS_TYPE_NEW:
                    if (model.isRandomType()) {
                        requestNewPhotosRandom(c, page, refresh);
                    } else {
                        requestNewPhotosOrders(c, page, refresh);
                    }
                    break;

                case PhotosObject.PHOTOS_TYPE_FEATURED:
                    if (model.isRandomType()) {
                        requestFeaturePhotosRandom(c, page, refresh);
                    } else {
                        requestFeaturePhotosOrders(c, page, refresh);
                    }
                    break;
            }
        }
    }

    @Override
    public void cancelRequest() {
        if (listener != null) {
            listener.cancel();
        }
        model.getService().cancel();
        model.setRefreshing(false);
        model.setLoading(false);
    }

    @Override
    public void refreshNew(Context c, boolean notify) {
        if (notify) {
            view.setRefreshing(true);
        }
        requestPhotos(c, model.getPhotosPage(), true);
    }

    @Override
    public void loadMore(Context c, boolean notify) {
        if (notify) {
            view.setLoading(true);
        }
        requestPhotos(c, model.getPhotosPage(), false);
    }

    @Override
    public void initRefresh(Context c) {
        cancelRequest();
        refreshNew(c, false);
        view.initRefreshStart();
    }

    @Override
    public boolean canLoadMore() {
        return !model.isRefreshing() && !model.isLoading() && !model.isOver();
    }

    @Override
    public boolean isRefreshing() {
        return model.isRefreshing();
    }

    @Override
    public boolean isLoading() {
        return model.isLoading();
    }

    @Override
    public Object getRequestKey() {
        return null;
    }

    @Override
    public void setRequestKey(Object k) {
        // do nothing.
    }

    @Override
    public int getPhotosType() {
        return model.getPhotosType();
    }

    @Override
    public String getPhotosOrder() {
        return model.getPhotosOrder();
    }

    @Override
    public void setOrder(String key) {
        model.setPhotosOrder(key);
    }

    @Override
    public String getOrder() {
        return model.getPhotosOrder();
    }

    @Override
    public void setPage(int page) {
        model.setPhotosPage(page);
    }

    @Override
    public void setPageList(List<Integer> pageList) {
        model.setPageList(pageList);
    }

    @Override
    public void setOver(boolean over) {
        model.setOver(over);
        view.setPermitLoading(!over);
    }

    @Override
    public void setActivityForAdapter(MysplashActivity a) {
        model.getAdapter().setActivity(a);
    }

    @Override
    public PhotoAdapter getAdapter() {
        return model.getAdapter();
    }

    private void requestNewPhotosOrders(Context c, int page, boolean refresh) {
        page = Math.max(1, refresh ? 1 : page + 1);
        listener = new OnRequestPhotosListener(c, page, refresh, false);
        model.getService()
                .requestPhotos(
                        page,
                        Mysplash.DEFAULT_PER_PAGE,
                        model.getPhotosOrder(),
                        listener);
    }

    private void requestNewPhotosRandom(Context c, int page, boolean refresh) {
        if (refresh) {
            page = 0;
            model.setPageList(ValueUtils.getPageListByCategory(Mysplash.CATEGORY_TOTAL_NEW));
        }
        listener = new OnRequestPhotosListener(c, page, refresh, true);
        model.getService()
                .requestPhotos(
                        model.getPageList().get(page),
                        Mysplash.DEFAULT_PER_PAGE,
                        PhotoApi.ORDER_BY_LATEST,
                        listener);
    }

    private void requestFeaturePhotosOrders(Context c, int page, boolean refresh) {
        page = Math.max(1, refresh ? 1 : page + 1);
        listener = new OnRequestPhotosListener(c, page, refresh, false);
        model.getService()
                .requestCuratePhotos(
                        page,
                        Mysplash.DEFAULT_PER_PAGE,
                        model.getPhotosOrder(),
                        listener);
    }

    private void requestFeaturePhotosRandom(Context c, int page, boolean refresh) {
        if (refresh) {
            page = 0;
            model.setPageList(ValueUtils.getPageListByCategory(Mysplash.CATEGORY_TOTAL_FEATURED));
        }
        listener = new OnRequestPhotosListener(c, page, refresh, true);
        model.getService()
                .requestCuratePhotos(
                        model.getPageList().get(page),
                        Mysplash.DEFAULT_PER_PAGE,
                        PhotoApi.ORDER_BY_LATEST,
                        listener);
    }

    // interface.

    private class OnRequestPhotosListener implements PhotoService.OnRequestPhotosListener {
        // data
        private Context c;
        private int page;
        private boolean refresh;
        private boolean random;
        private boolean canceled;

        OnRequestPhotosListener(Context c, int page, boolean refresh, boolean random) {
            this.c = c;
            this.page = page;
            this.refresh = refresh;
            this.random = random;
            this.canceled = false;
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRequestPhotosSuccess(Call<List<Photo>> call, Response<List<Photo>> response) {
            if (canceled) {
                return;
            }

            model.setRefreshing(false);
            model.setLoading(false);
            if (refresh) {
                view.setRefreshing(false);
            } else {
                view.setLoading(false);
            }
            if (response.isSuccessful()
                    && model.getAdapter().getRealItemCount() + response.body().size() > 0) {
                if (random) {
                    model.setPhotosPage(page + 1);
                } else {
                    model.setPhotosPage(page);
                }
                if (refresh) {
                    model.getAdapter().clearItem();
                    setOver(false);
                }
                for (int i = 0; i < response.body().size(); i ++) {
                    model.getAdapter().insertItem(response.body().get(i));
                }
                if (response.body().size() < Mysplash.DEFAULT_PER_PAGE) {
                    setOver(true);
                }
                view.requestPhotosSuccess();
            } else {
                view.requestPhotosFailed(c.getString(R.string.feedback_load_nothing_tv));
            }
        }

        @Override
        public void onRequestPhotosFailed(Call<List<Photo>> call, Throwable t) {
            if (canceled) {
                return;
            }
            model.setRefreshing(false);
            model.setLoading(false);
            if (refresh) {
                view.setRefreshing(false);
            } else {
                view.setLoading(false);
            }
            NotificationHelper.showSnackbar(
                    c.getString(R.string.feedback_load_failed_toast)
                            + " (" + t.getMessage() + ")");
            view.requestPhotosFailed(c.getString(R.string.feedback_load_failed_tv));
        }
    }
}
