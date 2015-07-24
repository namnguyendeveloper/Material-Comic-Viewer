package com.comicviewer.cedric.comicviewer.Info;

import android.app.Activity;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.comicviewer.cedric.comicviewer.Model.Comic;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.StorageManager;
import com.comicviewer.cedric.comicviewer.R;
import com.comicviewer.cedric.comicviewer.Utilities;
import com.gc.materialdesign.views.ButtonFlat;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

public class InfoActivity extends Activity {

    private Comic mComic = null;
    private TextView mTitleTextView;
    private ImageView mCoverImageView;
    private TextView mIssueNumberTextView;
    private TextView mYearTextView;
    private TextView mFilenameTextView;
    private TextView mFileSizeTextView;
    private TextView mPagesTextView;
    private ButtonFlat mEditButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        if (getActionBar()!=null)
            getActionBar().hide();

        StorageManager.setBackgroundColorPreference(this);

        if (getIntent().getParcelableExtra("Comic")!=null)
        {
            mComic = getIntent().getParcelableExtra("Comic");
        }

        if (Build.VERSION.SDK_INT>20)
        {
            getWindow().setStatusBarColor(Utilities.darkenColor(mComic.getComicColor()));
            getWindow().setNavigationBarColor(mComic.getComicColor());
        }

        initIDs();

        setTitleTextView();
        setComicCover();
        setIssueNumber();
        setYear();
        setPageCount();
        setFileInfo();

        if (StorageManager.hasWhiteBackgroundSet(this))
            setTextViewTextColors(getResources().getColor(R.color.BlueGreyDark));
        else
            setTextViewTextColors(getResources().getColor(R.color.White));
    }

    private void setFileInfo()
    {
        File archiveFile = new File(mComic.getFilePath()+"/"+mComic.getFileName());

        mFilenameTextView.setText(getString(R.string.filename)+":\n"+archiveFile.getName());
        mFileSizeTextView.setText(getString(R.string.file_size)+": "+archiveFile.length()/(1024*1024)+" mb");
    }

    private void setPageCount()
    {
        if (mComic.getPageCount()>0)
        {
            mPagesTextView.setText(getString(R.string.pages)+": "+mComic.getPageCount());
        }
        else
        {
            mPagesTextView.setVisibility(View.GONE);
        }
    }

    private void setYear()
    {
        if (mComic.getYear()!=-1)
        {
            mYearTextView.setText(getString(R.string.year)+": "+mComic.getEditedYear());
        }
        else
        {
            mYearTextView.setVisibility(View.GONE);
        }
    }

    private void setIssueNumber()
    {
        if (mComic.getIssueNumber()!=-1)
        {
            mIssueNumberTextView.setText(getString(R.string.issue_number)+": "+mComic.getEditedIssueNumber());
        }
        else
        {
            mIssueNumberTextView.setVisibility(View.GONE);
        }
    }

    private void setTitleTextView()
    {
        mTitleTextView.setText(mComic.getEditedTitle());
        mTitleTextView.setBackgroundColor(mComic.getComicColor());
        mTitleTextView.setTextColor(mComic.getTextColor());
    }

    private void setComicCover()
    {
        if (mComic.getCoverImage()!=null)
            ImageLoader.getInstance().displayImage(mComic.getCoverImage(),mCoverImageView);
    }

    private void initIDs()
    {
        mEditButton = (ButtonFlat) findViewById(R.id.edit_button);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
        mCoverImageView = (ImageView) findViewById(R.id.cover_image_view);
        mIssueNumberTextView = (TextView) findViewById(R.id.issue_number_text_view);
        mYearTextView = (TextView) findViewById(R.id.year_text_view);
        mFilenameTextView = (TextView) findViewById(R.id.filename_text_view);
        mFileSizeTextView = (TextView) findViewById(R.id.filesize_text_view);
        mPagesTextView = (TextView) findViewById(R.id.pages_text_view);
    }

    public void setTextViewTextColors(int color)
    {
        mTitleTextView.setTextColor(color);
        mIssueNumberTextView.setTextColor(color);
        mYearTextView.setTextColor(color);
        mFilenameTextView.setTextColor(color);
        mFileSizeTextView.setTextColor(color);
        mPagesTextView.setTextColor(color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

}
