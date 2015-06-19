package com.comicviewer.cedric.comicviewer.CloudFiles;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.box.androidsdk.content.models.BoxItem;
import com.comicviewer.cedric.comicviewer.Model.CloudService;
import com.comicviewer.cedric.comicviewer.Model.GoogleDriveObject;
import com.comicviewer.cedric.comicviewer.Model.ObjectType;
import com.comicviewer.cedric.comicviewer.NavigationManager;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.PreferenceSetter;
import com.comicviewer.cedric.comicviewer.R;
import com.comicviewer.cedric.comicviewer.Utilities;

import java.util.ArrayList;

/**
 * Created by CV on 19/06/2015.
 */
public class BoxAdapter extends RecyclerView.Adapter {

    private BoxActivity mActivity;
    private ArrayList<BoxItem> mBoxItemList;
    private LayoutInflater mInflater;
    private CloudService mCloudService;

    public BoxAdapter(BoxActivity activity, CloudService cloudService)
    {
        mActivity = activity;
        mCloudService = cloudService;
        mBoxItemList = new ArrayList<>();
        this.mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getItemViewType(int position)
    {
        if (mBoxItemList.get(position).getType().equals("folder"))
            return 0;
        else
            return 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        if (viewType==0) {
            v = mInflater.inflate(R.layout.cloud_folder_card, parent, false);
            CloudFolderViewHolder cloudFolderViewHolder = new CloudFolderViewHolder(v);
            cloudFolderViewHolder.mCardView.setCardBackgroundColor(Utilities.darkenColor(PreferenceSetter.getAppThemeColor(mActivity)));
            if (PreferenceSetter.getBackgroundColorPreference(mActivity) == mActivity.getResources().getColor(R.color.WhiteBG))
                cloudFolderViewHolder.mDownloadTextView.setTextColor(mActivity.getResources().getColor(R.color.Black));
            addFolderClickListener(cloudFolderViewHolder);
            addDownloadFolderClickListener(cloudFolderViewHolder);
            return cloudFolderViewHolder;
        }
        else {
            v = mInflater.inflate(R.layout.file_card, parent, false);
            CloudFileViewHolder cloudFileViewHolder = new CloudFileViewHolder(v);
            if (PreferenceSetter.getBackgroundColorPreference(mActivity) == mActivity.getResources().getColor(R.color.WhiteBG))
                cloudFileViewHolder.mDownloadTextView.setTextColor(mActivity.getResources().getColor(R.color.Black));
            cloudFileViewHolder.mCardView.setCardBackgroundColor(PreferenceSetter.getAppThemeColor(mActivity));
            addFileClickListener(cloudFileViewHolder);
            return cloudFileViewHolder;
        }
    }

    private void addDownloadFolderClickListener(final CloudFolderViewHolder cloudFolderViewHolder)
    {
        cloudFolderViewHolder.mDownloadFolderButton.setVisibility(View.GONE);
        cloudFolderViewHolder.mDownloadTextView.setVisibility(View.GONE);
        cloudFolderViewHolder.mSwipeLayout.setLeftSwipeEnabled(false);
        cloudFolderViewHolder.mSwipeLayout.setRightSwipeEnabled(false);
    }

    public void addFolderClickListener(final CloudFolderViewHolder cloudFolderViewHolder)
    {
        cloudFolderViewHolder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationManager.getInstance().pushPathToCloudStack(cloudFolderViewHolder.getBoxEntry().getId());
                mActivity.refresh();
            }
        });
    }

    private void addFileClickListener(final CloudFileViewHolder cloudFileViewHolder) {

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BoxItem entry = cloudFileViewHolder.getBoxEntry();

                MaterialDialog materialDialog = new MaterialDialog.Builder(mActivity)
                        .title(mActivity.getString(R.string.download_file))
                        .content(mActivity.getString(R.string.download_request)+" \""+entry.getName()+"\"?")
                        .positiveColor(PreferenceSetter.getAppThemeColor(mActivity))
                        .positiveText(mActivity.getString(R.string.confirm))
                        .negativeColor(PreferenceSetter.getAppThemeColor(mActivity))
                        .negativeText(mActivity.getString(R.string.cancel))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Toast.makeText(mActivity, mActivity.getString(R.string.download_started), Toast.LENGTH_SHORT).show();
                                DownloadFileService.startActionDownload(mActivity, entry, mCloudService);
                            }
                        }).show();
            }
        };

        cloudFileViewHolder.mCardView.setOnClickListener(clickListener);
        cloudFileViewHolder.mDownloadButton.setOnClickListener(clickListener);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mBoxItemList.get(position).getType().equals("folder"))
        {
            CloudFolderViewHolder cloudFolderViewHolder = (CloudFolderViewHolder) holder;
            cloudFolderViewHolder.setBoxEntry(mBoxItemList.get(position));
            cloudFolderViewHolder.mFolderNameTextView.setText(cloudFolderViewHolder.getBoxEntry().getName());
        }
        else
        {
            CloudFileViewHolder cloudFileViewHolder = (CloudFileViewHolder) holder;
            cloudFileViewHolder.setBoxEntry(mBoxItemList.get(position));
            cloudFileViewHolder.mFileNameTextView.setText(cloudFileViewHolder.getBoxEntry().getName());
        }
    }

    @Override
    public int getItemCount() {
        return mBoxItemList.size();
    }
    public void clear()
    {
        mBoxItemList.clear();
        notifyDataSetChanged();
    }

    public void addBoxObject(BoxItem object)
    {
        mBoxItemList.add(object);
        notifyItemInserted(mBoxItemList.size()-1);
    }

}