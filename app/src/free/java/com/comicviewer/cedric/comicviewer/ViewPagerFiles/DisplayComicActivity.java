package com.comicviewer.cedric.comicviewer.ViewPagerFiles;


import android.content.DialogInterface;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.comicviewer.cedric.comicviewer.CollectionActions;
import com.comicviewer.cedric.comicviewer.Model.Collection;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.StorageManager;
import com.comicviewer.cedric.comicviewer.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.lang.System;
import java.util.ArrayList;
import java.util.Random;


/**
 * The activity to display a fullscreen comic
 */
public class DisplayComicActivity extends AbstractDisplayComicActivity {

    InterstitialAd mInterstitialAd;
    AdRequest mAdRequest;

    @Override
    protected void initializeAd() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.admob_ad_unit_id));
        requestNewInterstitial();

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                showAd();
            }
        });
    }

    private void requestNewInterstitial() {
        mAdRequest = new AdRequest.Builder()
                .addTestDevice("AD58B57A983A628FE754A186B509F4C9")
                .addTestDevice("1D3A97B7E240BD8F05EE1F3BF1B68454")
                .build();
        mInterstitialAd.loadAd(mAdRequest);
    }

    @Override
    protected void showAd() {
        Random random = new Random(System.currentTimeMillis());
        int randInt = random.nextInt(9);

        if (randInt<3) {
            if (mInterstitialAd.isLoaded())
                mInterstitialAd.show();
        }

    }

    @Override
    protected void setPagerAnimation() {

    }

    @Override
    void showChooseCollectionsDialog() {
        ArrayList<Collection> collections = StorageManager.getCollectionList(this);
        final String newCollection = "Add new collection";

        CharSequence[] collectionNames;
        if (collections.size()<2) {
            collectionNames = new CharSequence[collections.size() + 1];
            collectionNames[collectionNames.length-1] = newCollection;
        }
        else
        {
            collectionNames = new CharSequence[collections.size()];
        }

        for (int i=0;i<collections.size();i++)
        {
            collectionNames[i] = collections.get(i).getName();
        }


        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title("Choose collection")
                .titleColor(getResources().getColor(R.color.Black))
                .itemColor(getResources().getColor(R.color.GreyDark))
                .backgroundColor(getResources().getColor(R.color.White))
                .items(collectionNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        if (charSequence.equals(newCollection)) {
                            MaterialDialog dialog = new MaterialDialog.Builder(DisplayComicActivity.this)
                                    .title("Create new collection")
                                    .titleColor(getResources().getColor(R.color.Black))
                                    .backgroundColor(getResources().getColor(R.color.White))
                                    .input("Collection name", "", false, new MaterialDialog.InputCallback() {
                                        @Override
                                        public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                                            StorageManager.createCollection(DisplayComicActivity.this, charSequence.toString());
                                            CollectionActions.addComicToCollection(DisplayComicActivity.this, charSequence.toString(), mCurrentComic);
                                        }
                                    })
                                    .positiveText(getString(R.string.confirm))
                                    .positiveColor(StorageManager.getAppThemeColor(DisplayComicActivity.this))
                                    .negativeColor(StorageManager.getAppThemeColor(DisplayComicActivity.this))
                                    .negativeText(getString(R.string.cancel))
                                    .dismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            setSystemVisibilitySettings();
                                        }
                                    })
                                    .show();
                        } else {
                            CollectionActions.addComicToCollection(DisplayComicActivity.this, charSequence.toString(), mCurrentComic);
                        }
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setSystemVisibilitySettings();
                    }
                })
                .show();
    }
}
