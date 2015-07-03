package com.comicviewer.cedric.comicviewer.ViewPagerFiles;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.comicviewer.cedric.comicviewer.ComicLoader;
import com.comicviewer.cedric.comicviewer.Extractor;
import com.comicviewer.cedric.comicviewer.Model.Comic;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.PreferenceSetter;
import com.comicviewer.cedric.comicviewer.R;
import com.comicviewer.cedric.comicviewer.Utilities;
import com.devspark.robototextview.widget.RobotoTextView;
import com.melnykov.fab.FloatingActionButton;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by CV on 28/06/2015.
 */
public abstract class AbstractDisplayComicActivity extends AppCompatActivity{

    //The comic to be displayed
    protected Comic mCurrentComic;

    //The number of pages of the comic
    protected int mPageCount;

    protected ComicViewPager mPager;
    protected ComicStatePagerAdapter mPagerAdapter;

    //Arraylist containing the filenamestrings of the fileheaders of the pages
    protected ArrayList<String> mPages;

    protected RobotoTextView mPageIndicator;

    protected String mPageNumberSetting;

    protected Handler mHandler;

    protected boolean mMangaComic = false;

    protected FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_comic);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getWindow().getDecorView().setBackgroundColor(PreferenceSetter.getReadingBackgroundSetting(this));

        initializeAd();

        if (PreferenceSetter.getForcePortraitSetting(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        mFab = (FloatingActionButton) findViewById(R.id.fab);

        mHandler = new Handler();

        Intent intent = getIntent();

        int lastReadPage;


        if (intent.getAction()!= null && intent.getAction().equals(Intent.ACTION_VIEW))
        {
            Uri uri = intent.getData();
            File file = new File(uri.getPath());
            Comic comic = new Comic(file.getName(), new File(file.getParent()).getPath());
            ComicLoader.loadComicSync(this, comic);
            mCurrentComic = comic;
        }
        else {
            mCurrentComic = intent.getParcelableExtra("Comic");
        }


        if (Build.VERSION.SDK_INT>18 &&!PreferenceSetter.getToolbarOption(this)) {

            hideSystemUI();

            setLayoutParams(getResources().getConfiguration());

            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        // The system bars are visible. Make any desired
                        // adjustments to your UI, such as showing the action bar or
                        // other navigational controls.
                        mFab.setVisibility(View.VISIBLE);
                        mFab.show(true);

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideSystemUI();
                            }
                        }, 5000);
                    }
                }
            });
        }
        else if (PreferenceSetter.getToolbarOption(this))
        {
            //toolbar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);
            toolbar.showOverflowMenu();
            getSupportActionBar().setTitle(mCurrentComic.getTitle());
            if (PreferenceSetter.getReadingBackgroundSetting(this)==getResources().getColor(R.color.White))
                toolbar.setBackgroundColor(getResources().getColor(R.color.Black));
        }

        mPageCount = mCurrentComic.getPageCount();

        mPages = new ArrayList<>();

        mPageIndicator = (RobotoTextView) findViewById(R.id.page_indicator);

        loadImageNames();

        mPageNumberSetting = PreferenceSetter.getPageNumberSetting(this);

        mPager =  (ComicViewPager) findViewById(R.id.comicpager);
        mPager.setOffscreenPageLimit(2);
        mPagerAdapter = new ComicStatePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mMangaComic = false;
        if ((PreferenceSetter.getMangaSetting(this) && !PreferenceSetter.isNormalComic(this,mCurrentComic))
                || (!(PreferenceSetter.getMangaSetting(this)) && PreferenceSetter.isMangaComic(this, mCurrentComic)))
        {
            mPager.setCurrentItem(mCurrentComic.getPageCount()-1);
            mMangaComic = true;
        }

        if (PreferenceSetter.getReadComics(this).containsKey(mCurrentComic.getFileName()))
        {
            lastReadPage = PreferenceSetter.getReadComics(this).get(mCurrentComic.getFileName());
            if (mMangaComic)
            {
                mPager.setCurrentItem(mPageCount-1-lastReadPage);
            }
            else {
                mPager.setCurrentItem(lastReadPage);
            }
        }

        setPagerAnimation();


        boolean showInRecentsPref = getPreferences(Context.MODE_PRIVATE).getBoolean("useRecents",true);

        if (showInRecentsPref && Build.VERSION.SDK_INT>20) {
            new SetTaskDescriptionTask().execute();
        }


        if (PreferenceSetter.getScreenOnSetting(this))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if ((mCurrentComic.getColorSetting().equals(getString(R.string.card_color_setting_1))
                || mCurrentComic.getColorSetting().equals(getString(R.string.card_color_setting_2))) && mCurrentComic.getComicColor()!=-1)
        {
            mFab.setColorNormal(mCurrentComic.getComicColor());
            mFab.setColorPressed(Utilities.darkenColor(mCurrentComic.getComicColor()));
            mFab.setColorRipple(Utilities.lightenColor(mCurrentComic.getComicColor()));
        }
        else
        {
            mFab.setColorNormal(PreferenceSetter.getAccentColor(this));
            mFab.setColorPressed(Utilities.darkenColor(PreferenceSetter.getAccentColor(this)));
            mFab.setColorRipple(Utilities.lightenColor(PreferenceSetter.getAccentColor(this)));
        }

        if (Build.VERSION.SDK_INT>18)
        {
            setFabClickListener();
        }
    };

    protected abstract void initializeAd();

    protected abstract void showAd();

    protected abstract void setPagerAnimation();

    private void setFabClickListener() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSystemUI();

                showGoToPageDialog();
            }
        });
    }

    public void goToNextPage()
    {
        int currentPos = mPager.getCurrentItem();
        if (mMangaComic)
            mPager.setCurrentItem(currentPos-1);
        else
            mPager.setCurrentItem(currentPos+1);
    }

    public void goToPreviousPage()
    {
        int currentPos = mPager.getCurrentItem();
        if (mMangaComic)
            mPager.setCurrentItem(currentPos+1);
        else
            mPager.setCurrentItem(currentPos-1);
    }

    public void showGoToPageDialog()
    {

        CharSequence[] pages = new CharSequence[mPageCount];

        for (int i = 0; i < mPageCount; i++) {
            pages[i] = "" + (i + 1);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(AbstractDisplayComicActivity.this)
                .title(getString(R.string.go_to_page))
                .negativeColor(PreferenceSetter.getAppThemeColor(AbstractDisplayComicActivity.this))
                .negativeText(getString(R.string.cancel))
                .items(pages)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        materialDialog.dismiss();
                        int pos = i;
                        if (mMangaComic)
                            pos = (mPageCount-1) - i;
                        mPager.setCurrentItem(pos);
                    }
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId()) {
            case R.id.go_to_page_menu_item:
                showGoToPageDialog();
                return true;
        }
        return false;
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        mFab.hide(true);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {

        mFab.setVisibility(View.INVISIBLE);


        if (Build.VERSION.SDK_INT>18 && !PreferenceSetter.getToolbarOption(this))
        {
            hideSystemUI();
            setLayoutParams(configuration);
        }


        if (mFab.isVisible())
            mFab.hide(true);

        super.onConfigurationChanged(configuration);

    }

    private void setLayoutParams(Configuration configuration)
    {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        Display display = this.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float sixteenDP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, outMetrics);
        float fortysixDP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, outMetrics);


        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            params.setMargins(0, 0, (int)fortysixDP, (int)sixteenDP);
        }
        else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            params.setMargins(0, 0, (int)sixteenDP, (int)fortysixDP);
        }
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mFab.setLayoutParams(params);
    }




    private class ComicPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {


            if ((PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this) && !PreferenceSetter.isNormalComic(AbstractDisplayComicActivity.this,mCurrentComic))
                    || (!(PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this)) && PreferenceSetter.isMangaComic(AbstractDisplayComicActivity.this, mCurrentComic)))
            {
                position = mPageCount-1-position;
            }

            setPageNumber();

            int pagesRead = PreferenceSetter.getPagesReadForComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName());

            if (pagesRead==0)
            {
                PreferenceSetter.incrementNumberOfComicsStarted(AbstractDisplayComicActivity.this, 1);
            }


            PreferenceSetter.saveLastReadComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), position);

            if (position+1> pagesRead)
            {
                PreferenceSetter.savePagesForComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), position+1);
                if (position+1 >= mCurrentComic.getPageCount())
                {
                    PreferenceSetter.incrementNumberOfComicsRead(AbstractDisplayComicActivity.this, 1);
                    PreferenceSetter.saveLongestReadComic(AbstractDisplayComicActivity.this,
                            mCurrentComic.getFileName(),
                            mCurrentComic.getPageCount(),
                            mCurrentComic.getTitle(),
                            mCurrentComic.getIssueNumber());
                }
                PreferenceSetter.incrementPagesForSeries(AbstractDisplayComicActivity.this, mCurrentComic.getTitle(), 1);
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    private void setPageNumber()
    {
        int pageNumber = mPager.getCurrentItem()+1;

        if ((PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this) && !PreferenceSetter.isNormalComic(AbstractDisplayComicActivity.this,mCurrentComic))
                || (!(PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this)) && PreferenceSetter.isMangaComic(AbstractDisplayComicActivity.this, mCurrentComic)))
        {
            pageNumber = mCurrentComic.getPageCount()-mPager.getCurrentItem();
        }

        if (mPageNumberSetting.equals(getString(R.string.page_number_setting_1)) && mPageCount>0)
        {
            mPageIndicator.setVisibility(View.VISIBLE);
            mPageIndicator.setText(" "+pageNumber+" "+getString(R.string.of)+" "+mPageCount+" ");
        }
        else if (mPageNumberSetting.equals(getString(R.string.page_number_setting_2)) && mPageCount>0)
        {
            final String currentPageText = " "+pageNumber+" "+getString(R.string.of)+" "+mPageCount+" ";
            mPageIndicator.setVisibility(View.VISIBLE);
            mPageIndicator.setText(currentPageText);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mPageIndicator.getText().equals(currentPageText))
                    {
                        mPageIndicator.setVisibility(View.INVISIBLE);
                        mPageIndicator.setText("");
                    }
                }
            },3000);
        }
        else
        {
            mPageIndicator.setVisibility(View.INVISIBLE);
            mPageIndicator.setText("");
        }
    }

    private class SetTaskDescriptionTask extends AsyncTask
    {

        @Override
        protected Object doInBackground(Object[] params) {

            if (!ImageLoader.getInstance().isInited()) {
                ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(AbstractDisplayComicActivity.this).build();
                ImageLoader.getInstance().init(config);
            }

            if (Build.VERSION.SDK_INT>20) {
                ActivityManager.TaskDescription tdscr = null;
                try {
                    ImageSize size = new ImageSize(64, 64);
                    tdscr = new ActivityManager.TaskDescription(mCurrentComic.getTitle(),
                            ImageLoader.getInstance().loadImageSync(mCurrentComic.getCoverImage(), size),
                            mCurrentComic.getComicColor());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (tdscr != null)
                    setTaskDescription(tdscr);
            }

            return null;
        }
    }

    /**
     * Function to get the filenamestrings of the files in the archive
     */
    private void loadImageNames()
    {
        mPages = Extractor.loadImageNamesFromComic(this, mCurrentComic);
    }


    private class ComicStatePagerAdapter extends FragmentStatePagerAdapter
    {
        FragmentManager mFragmentManager;

        public ComicStatePagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public Fragment getItem(int position) {

            String filename = mCurrentComic.getFileName();
            String comicPath = mCurrentComic.getFilePath()+ "/" + filename;
            ComicPageFragment fragment = ComicPageFragment.newInstance(comicPath, mPages.get(position), position);
            return fragment;
        }

        @Override
        public int getCount() {
            return mPageCount;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mPageNumberSetting = PreferenceSetter.getPageNumberSetting(this);
        setPageNumber();

        setPagerAnimation();

        mPager.setOnPageChangeListener(new ComicPageChangeListener());

        if (mPageCount<1)
        {
            MaterialDialog materialDialog = new MaterialDialog.Builder(AbstractDisplayComicActivity.this)
                    .title("Error")
                    .content("This file can not be opened by comic viewer")
                    .positiveText("Accept")
                    .positiveColor(PreferenceSetter.getAppThemeColor(AbstractDisplayComicActivity.this))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            AbstractDisplayComicActivity.this.finish();
                        }
                    }).show();

        }
    }


    private void removeExtractedFiles() {

        File archive = new File(mCurrentComic.getFilePath()+"/"+mCurrentComic.getFileName());
        if (!archive.isDirectory())
            for (int i = 0; i < mPages.size(); i++) {
                if (i != 0) {
                    try {
                        String filename = mPages.get(i);
                        if (filename.contains("#"))
                            filename = filename.replaceAll("#", "");
                        File file = new File(getFilesDir().getPath() + "/" + Utilities.removeExtension(mCurrentComic.getFileName()) + "/" + filename);
                        if (file.delete())
                            Log.d("DisplayComic Onstop", "Deleted file " + filename);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

    }

    @Override
    public void onPause()
    {
        super.onPause();

        int pageNumber = mPager.getCurrentItem();

        if ((PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this) && !PreferenceSetter.isNormalComic(AbstractDisplayComicActivity.this,mCurrentComic))
                || (!(PreferenceSetter.getMangaSetting(AbstractDisplayComicActivity.this)) && PreferenceSetter.isMangaComic(AbstractDisplayComicActivity.this, mCurrentComic)))
        {
            pageNumber = mCurrentComic.getPageCount()-1-mPager.getCurrentItem();
        }
        PreferenceSetter.saveLastReadComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), pageNumber);

    }

    @Override
    public void onDestroy()
    {
        removeExtractedFiles();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if (Build.VERSION.SDK_INT>20)
            finishAfterTransition();
        else
            finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (PreferenceSetter.getVolumeKeyPreference(this) && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if (action == KeyEvent.ACTION_DOWN) {
                int page = mPager.getCurrentItem();
                if (mMangaComic) {
                    page++;
                    if (page >= 0 && page < mPageCount) {
                        mPager.setCurrentItem(page);
                    }
                } else {
                    page--;
                    if (page >= 0 && page < mPageCount) {
                        mPager.setCurrentItem(page);
                    }
                }
            }

            return true;

        }
        else if (PreferenceSetter.getVolumeKeyPreference(this) && keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            if (action == KeyEvent.ACTION_DOWN) {
                int page = mPager.getCurrentItem();
                if (mMangaComic) {
                    page--;
                    if (page >= 0 && page < mPageCount) {
                        mPager.setCurrentItem(page);
                    }
                } else {
                    page++;
                    if (page >= 0 && page < mPageCount) {
                        mPager.setCurrentItem(page);
                    }
                }
            }
            return true;
        }
        else
        {
            return super.dispatchKeyEvent(event);
        }
    }
}
