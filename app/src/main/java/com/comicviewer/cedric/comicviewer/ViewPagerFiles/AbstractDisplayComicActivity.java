package com.comicviewer.cedric.comicviewer.ViewPagerFiles;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.comicviewer.cedric.comicviewer.CollectionActions;
import com.comicviewer.cedric.comicviewer.ComicLoader;
import com.comicviewer.cedric.comicviewer.Extractor;
import com.comicviewer.cedric.comicviewer.Model.Collection;
import com.comicviewer.cedric.comicviewer.Model.Comic;
import com.comicviewer.cedric.comicviewer.MultiColorDrawable;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.StorageManager;
import com.comicviewer.cedric.comicviewer.R;
import com.comicviewer.cedric.comicviewer.Utilities;
import com.devspark.robototextview.widget.RobotoTextView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by CV on 28/06/2015.
 */
public abstract class AbstractDisplayComicActivity extends AppCompatActivity {

    public static final String LAST_PAGE = "specialLastPageComicViewer";

    //The comic to be displayed
    protected Comic mCurrentComic;

    protected ArrayList<Comic> mNextComics;

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

    protected FloatingActionMenu mMenuFab;

    protected FloatingActionButton mGoToPageFab;
    protected FloatingActionButton mExitFab;
    protected FloatingActionButton mFavoriteFab;
    protected FloatingActionButton mAddToCollectionFab;
    protected FloatingActionButton mNextComicFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_comic);

        initializeAd();

        if (StorageManager.getBooleanSetting(this, StorageManager.FORCE_PORTRAIT_SETTING, false))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        mMenuFab = (FloatingActionMenu) findViewById(R.id.menu_fab);
        mGoToPageFab = (FloatingActionButton) findViewById(R.id.go_to_page_fab);
        mExitFab = (FloatingActionButton) findViewById(R.id.exit_fab);
        mFavoriteFab = (FloatingActionButton) findViewById(R.id.favorite_fab);
        mAddToCollectionFab = (FloatingActionButton) findViewById(R.id.add_to_collection_fab);
        mNextComicFab = (FloatingActionButton) findViewById(R.id.next_comic_fab);

        mHandler = new Handler();

        Intent intent = getIntent();

        int lastReadPage;

        if (!ImageLoader.getInstance().isInited())
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        ImageLoader.getInstance().clearMemoryCache();
        ImageLoader.getInstance().clearDiskCache();


        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            File file = new File(uri.getPath());
            Comic comic = new Comic(file.getName(), new File(file.getParent()).getPath());
            ComicLoader.loadComicSync(this, comic);
            mCurrentComic = comic;
        } else {
            mCurrentComic = intent.getParcelableExtra("Comic");
        }

        if (intent.getParcelableArrayListExtra("NextComics") != null)
            mNextComics = intent.getParcelableArrayListExtra("NextComics");
        else
            mNextComics = null;

        if (StorageManager.getDynamicBackgroundSetting(this))
            getWindow().getDecorView().setBackgroundColor(mCurrentComic.getComicColor());
        else
            getWindow().getDecorView().setBackgroundColor(StorageManager.getReadingBackgroundSetting(this));

        setSystemVisibilitySettings();
        mMangaComic = shouldUseMangaPosition();

        mPageCount = mCurrentComic.getPageCount();

        mPages = new ArrayList<>();

        mPageIndicator = (RobotoTextView) findViewById(R.id.page_indicator);

        loadImageNames();

        mPageNumberSetting = StorageManager.getPageNumberSetting(this);

        mPager = (ComicViewPager) findViewById(R.id.comicpager);
        mPager.setOffscreenPageLimit(4);
        mPagerAdapter = new ComicStatePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        if (mMangaComic) {
            mPager.setCurrentItem(mCurrentComic.getPageCount());
        }

        if (StorageManager.getComicPositionsMap(this).containsKey(mCurrentComic.getFileName())) {
            lastReadPage = StorageManager.getComicPositionsMap(this).get(mCurrentComic.getFileName());
            if (mMangaComic) {
                mPager.setCurrentItem(mPageCount - lastReadPage);
            } else {
                mPager.setCurrentItem(lastReadPage);
            }
        }

        setPagerAnimation();


        boolean showInRecentsPref = getPreferences(Context.MODE_PRIVATE).getBoolean("useRecents", true);

        if (showInRecentsPref && Build.VERSION.SDK_INT > 20) {
            new SetTaskDescriptionTask().execute();
        }


        if (StorageManager.getBooleanSetting(this, StorageManager.KEEP_SCREEN_ON, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mMenuFab.setMenuButtonColorNormal(StorageManager.getAccentColor(this));
        mMenuFab.setMenuButtonColorPressed(Utilities.darkenColor(StorageManager.getAccentColor(this)));
        mMenuFab.setMenuButtonColorRipple(Utilities.lightenColor(StorageManager.getAccentColor(this)));


        if (Build.VERSION.SDK_INT > 18) {
            setFabClickListeners();
        }
    }

    ;

    public void setSystemVisibilitySettings() {
        if (Build.VERSION.SDK_INT > 18 && !StorageManager.getBooleanSetting(this, StorageManager.TOOLBAR_OPTION, false)) {

            hideSystemUI();

            setLayoutParams(getResources().getConfiguration());

            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        // The system bars are visible. Make any desired
                        // adjustments to your UI, such as showing the action bar or
                        // other navigational controls.
                        mMenuFab.setVisibility(View.VISIBLE);
                        mMenuFab.showMenuButton(true);

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideSystemUI();
                            }
                        }, 5000);
                    }
                }
            });
        } else if (StorageManager.getBooleanSetting(this, StorageManager.TOOLBAR_OPTION, false)) {
            //toolbar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);
            toolbar.showOverflowMenu();
            getSupportActionBar().setTitle(mCurrentComic.getTitle());
            if (StorageManager.getReadingBackgroundSetting(this) == getResources().getColor(R.color.White))
                toolbar.setBackgroundColor(getResources().getColor(R.color.Black));
        }
    }

    protected abstract void initializeAd();

    protected abstract void showAd();

    protected abstract void setPagerAnimation();

    private void setFabClickListeners() {

        setGoToPageClickListener();
        setExitFabClickListener();
        setFavoriteClickListener();
        setAddToCollectionClickListener();
        setNextComicClickListener();
    }

    private void setNextComicClickListener() {
        mNextComicFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuFab.close(true);
                showNextComicDialog();
            }
        });
    }

    private void setAddToCollectionClickListener() {
        mAddToCollectionFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuFab.close(true);
                showChooseCollectionsDialog();
            }
        });
    }

    protected void setFavoriteClickListener()
    {
        if (StorageManager.getFavoriteComics(this).contains(mCurrentComic.getFileName()))
        {
            mFavoriteFab.setLabelText("Unfavorite");
            mFavoriteFab.setImageResource(R.drawable.fab_star);
            mFavoriteFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StorageManager.removeFavoriteComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName());
                    setFavoriteClickListener();
                }
            });
        }
        else
        {
            mFavoriteFab.setLabelText("Favorite");
            mFavoriteFab.setImageResource(R.drawable.fab_star_outline);
            mFavoriteFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StorageManager.saveFavoriteComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName());
                    setFavoriteClickListener();
                }
            });
        }
    }

    private void setExitFabClickListener() {
        mExitFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuFab.close(true);
                finish();
            }
        });
    }

    private void setGoToPageClickListener()
    {
        mGoToPageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuFab.close(true);
                showGoToPageDialog();
            }
        });
    }

    public void showNextComicDialog()
    {
        if (mNextComics!=null && mNextComics.size()>0) {
            new MaterialDialog.Builder(this)
                    .title("Next comic")
                    .titleColor(getResources().getColor(R.color.Black))
                    .backgroundColor(getResources().getColor(R.color.White))
                    .content("Are you sure you want to open the next comic?")
                    .contentColor(getResources().getColor(R.color.GreyDark))
                    .positiveText(getString(R.string.confirm))
                    .positiveColor(StorageManager.getAppThemeColor(AbstractDisplayComicActivity.this))
                    .negativeText(getString(R.string.cancel))
                    .negativeColor(StorageManager.getAppThemeColor(AbstractDisplayComicActivity.this))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Intent intent = new Intent(AbstractDisplayComicActivity.this, DisplayComicActivity.class);

                            intent.putExtra("Comic", mNextComics.get(0));
                            ArrayList<Comic> newNextComics = new ArrayList<Comic>();
                            for (int j=1;j<mNextComics.size();j++)
                            {
                                newNextComics.add(mNextComics.get(j));
                            }
                            intent.putExtra("NextComics", newNextComics);
                            if (StorageManager.getBooleanSetting(AbstractDisplayComicActivity.this, StorageManager.USES_RECENTS, true)) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            }
                            startActivity(intent);
                            finish();
                        }
                    }).show();
        }
        else
        {
            Toast.makeText(AbstractDisplayComicActivity.this, "No next comics found!", Toast.LENGTH_SHORT).show();
        }
    }

    public void goToNextPage()
    {
        if (mMangaComic)
            goToLeftPage();
        else
            goToRightPage();
    }

    public void goToPreviousPage()
    {
        if (mMangaComic)
            goToRightPage();
        else
            goToLeftPage();
    }

    public void goToRightPage()
    {
        int currentPos = mPager.getCurrentItem();
        mPager.setCurrentItem(currentPos + 1);
    }

    public void goToLeftPage()
    {
        int currentPos = mPager.getCurrentItem();
        mPager.setCurrentItem(currentPos-1);
    }

    abstract void showChooseCollectionsDialog();

    public void showGoToPageDialog()
    {

        CharSequence[] pages = new CharSequence[mPageCount];

        for (int i = 0; i < mPageCount; i++) {
            pages[i] = "" + (i + 1);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(AbstractDisplayComicActivity.this)
                .title(getString(R.string.go_to_page))
                .titleColor(getResources().getColor(R.color.Black))
                .backgroundColor(getResources().getColor(R.color.White))
                .negativeColor(StorageManager.getAppThemeColor(AbstractDisplayComicActivity.this))
                .negativeText(getString(R.string.cancel))
                .items(pages)
                .itemColor(getResources().getColor(R.color.GreyDark))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        materialDialog.dismiss();
                        int pos = i;
                        if (mMangaComic)
                            pos = (mPageCount - 1) - i;
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
        mMenuFab.close(true);
        mMenuFab.hideMenuButton(true);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {

        if (Build.VERSION.SDK_INT>18 && !StorageManager.getBooleanSetting(this, StorageManager.TOOLBAR_OPTION, false))
        {
            hideSystemUI();
            setLayoutParams(configuration);
        }

        mMenuFab.hideMenuButton(true);

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
        else
        {
            params.setMargins(0, 0, (int)fortysixDP, (int)fortysixDP);
        }
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mMenuFab.setLayoutParams(params);
    }




    private class ComicPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            if (mPages.get(position).equals(LAST_PAGE))
                setPageNumberHidden(true);
            else
                setPageNumberHidden(false);

            if (mMangaComic)
            {
                position = getMangaPagePosition(position);
            }

            setPageNumber();

            if (StorageManager.getDynamicBackgroundSetting(AbstractDisplayComicActivity.this) && Build.VERSION.SDK_INT>15) {
                int[] topBottomColors = {mPagerAdapter.getPageTopColor(position), mPagerAdapter.getPageBottomColor(position)};
                MultiColorDrawable drawable = new MultiColorDrawable(topBottomColors, MultiColorDrawable.Orientation.VERTICAL);
                getWindow().getDecorView().setBackground(drawable);
            }

            int pagesRead = StorageManager.getPagesReadForComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName());

            if (pagesRead == 0) {
                StorageManager.incrementNumberOfComicsStarted(AbstractDisplayComicActivity.this, 1);
            }

            StorageManager.saveComicPosition(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), position);

            if (position + 1 > pagesRead) {
                StorageManager.savePagesForComic(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), position + 1);
                if (position + 1 >= mCurrentComic.getPageCount()) {
                    StorageManager.incrementNumberOfComicsRead(AbstractDisplayComicActivity.this, 1);
                    StorageManager.saveLongestReadComic(AbstractDisplayComicActivity.this,
                            mCurrentComic.getFileName(),
                            mCurrentComic.getPageCount(),
                            mCurrentComic.getTitle(),
                            mCurrentComic.getIssueNumber());
                }
                StorageManager.incrementPagesForSeries(AbstractDisplayComicActivity.this, mCurrentComic.getTitle(), 1);
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    private int getMangaPagePosition(int regularPosition)
    {
        return mPageCount-regularPosition;
    }

    private void setPageNumber()
    {
        int pageNumber;

        if (mMangaComic)
            pageNumber = getMangaPagePosition(mPager.getCurrentItem())+1;
        else
            pageNumber = mPager.getCurrentItem()+1;

        final String currentPageText = " "+pageNumber+" "+getString(R.string.of)+" "+mPageCount+" ";

        if (mPageNumberSetting.equals(getString(R.string.page_number_setting_1)) && mPageCount>0)
        {
            mPageIndicator.setText(currentPageText);
        }
        else if (mPageNumberSetting.equals(getString(R.string.page_number_setting_2)) && mPageCount>0)
        {
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

    private void setPageNumberHidden(boolean enable)
    {
        if (enable)
            mPageIndicator.setVisibility(View.INVISIBLE);
        else
            mPageIndicator.setVisibility(View.VISIBLE);
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
        ArrayList<String> pages = Extractor.loadImageNamesFromComic(this, mCurrentComic);

        if (mMangaComic)
        {
            if (pages.size()>0) {
                ArrayList<String> mangaPages = new ArrayList<>();
                for (int i=pages.size()-1;i>=0;i--)
                {
                    mangaPages.add(pages.get(i));
                }

                mangaPages.add(0, LAST_PAGE);
                mPages = mangaPages;
            }
        }
        else {
            pages.add(LAST_PAGE);
            mPages = pages;
        }

    }


    private class ComicStatePagerAdapter extends FragmentStatePagerAdapter
    {
        FragmentManager mFragmentManager;
        private int[] mPageTopColors = new int[Math.max(1,mPageCount+1)];
        private int[] mPageBottomColors = new int[Math.max(1,mPageCount+1)];

        public ComicStatePagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
            for (int i=0;i<mPageTopColors.length;i++) {
                mPageTopColors[i] = mCurrentComic.getComicColor();
                mPageBottomColors[i] = mCurrentComic.getComicColor();
            }
        }

        @Override
        public Fragment getItem(final int position) {


            if (mPages.get(position).equals(LAST_PAGE))
                return LastComicPageFragment.newInstance(mCurrentComic, mNextComics);
            else
            {
                String filename = mCurrentComic.getFileName();
                String comicPath = mCurrentComic.getFilePath() + "/" + filename;
                final ComicPageFragment fragment = ComicPageFragment.newInstance(comicPath, mPages.get(position), position);
                return fragment;
            }

        }

        public void setPageTopColor(int pos, int color)
        {
            if (mMangaComic)
                mPageTopColors[getMangaPagePosition(pos)] = color;
            else
                mPageTopColors[pos] = color;
        }

        public int getPageTopColor(int pos)
        {
            return mPageTopColors[pos];
        }

        public void setPageBottomColor(int pos, int color)
        {
            if (mMangaComic)
                mPageBottomColors[getMangaPagePosition(pos)] = color;
            else
                mPageBottomColors[pos] = color;
        }

        public int getPageBottomColor(int pos)
        {
            return mPageBottomColors[pos];
        }

        @Override
        public int getCount() {
            return mPageCount+1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if(object instanceof ComicPageFragment) {
                ComicPageFragment fragment = (ComicPageFragment) object;
                if (fragment.mBitmap != null && !fragment.mBitmap.isRecycled())
                    fragment.mBitmap.recycle();

                if (fragment.mFullscreenComicView != null && fragment.mFullscreenComicView.getDrawable()!=null) {
                    Bitmap bitmap = ((BitmapDrawable) fragment.mFullscreenComicView.getDrawable()).getBitmap();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        bitmap = null;
                    }
                }
            }
            super.destroyItem(container,position,object);
        }
    }

    public void setPagerBottomPageColor(int pos, int color)
    {
        mPagerAdapter.setPageBottomColor(pos, color);
    }

    public void setPagerTopPageColor(int pos, int color)
    {
        mPagerAdapter.setPageTopColor(pos, color);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setSystemVisibilitySettings();

        mPageNumberSetting = StorageManager.getPageNumberSetting(this);

        if (mPages.get(mPager.getCurrentItem()).equals(LAST_PAGE))
            setPageNumberHidden(true);
        else
            setPageNumberHidden(false);
        setPageNumber();

        setPagerAnimation();

        mPager.setOnPageChangeListener(new ComicPageChangeListener());

        if (mPageCount<1)
        {
            showErrorDialog();
        }
    }

    private void showErrorDialog()
    {
        MaterialDialog materialDialog = new MaterialDialog.Builder(AbstractDisplayComicActivity.this)
                .title("Error")
                .content("This file can not be opened by comic viewer")
                .positiveText("Accept")
                .positiveColor(StorageManager.getAppThemeColor(AbstractDisplayComicActivity.this))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        AbstractDisplayComicActivity.this.finish();
                    }
                }).show();
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

        if (shouldUseMangaPosition())
        {
            pageNumber = mCurrentComic.getPageCount()-1-mPager.getCurrentItem();
        }
        StorageManager.saveComicPosition(AbstractDisplayComicActivity.this, mCurrentComic.getFileName(), pageNumber);

    }

    private boolean shouldUseMangaPosition()
    {
        return (StorageManager.getBooleanSetting(AbstractDisplayComicActivity.this, StorageManager.MANGA_SETTING, false)
                && !StorageManager.isNormalComic(AbstractDisplayComicActivity.this, mCurrentComic))
                || (!(StorageManager.getBooleanSetting(AbstractDisplayComicActivity.this, StorageManager.MANGA_SETTING, false))
                && StorageManager.isMangaComic(AbstractDisplayComicActivity.this, mCurrentComic));
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

        if (StorageManager.getBooleanSetting(this, StorageManager.VOLUME_KEY_OPTION, false) && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!StorageManager.getBooleanSetting(this, StorageManager.REVERSE_VOLUME_KEYS, false))
                    goToNextPage();
                else
                    goToPreviousPage();
            }

            return true;

        }
        else if (StorageManager.getBooleanSetting(this, StorageManager.VOLUME_KEY_OPTION, false) && keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!StorageManager.getBooleanSetting(this, StorageManager.REVERSE_VOLUME_KEYS, false))
                    goToPreviousPage();
                else
                    goToNextPage();
            }
            return true;
        }
        else
        {
            return super.dispatchKeyEvent(event);
        }
    }
}
