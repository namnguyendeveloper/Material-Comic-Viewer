package com.comicviewer.cedric.comicviewer.ViewPagerFiles;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.comicviewer.cedric.comicviewer.R;
import com.comicviewer.cedric.comicviewer.Utilities;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.lingala.zip4j.core.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */
public class ComicPageFragment extends Fragment {

    // The imageview for the comic
    private TouchImageView mFullscreenComicView;

    // The path to the .cbr file (Directory + filename)
    private String mComicArchivePath;

    // The filename of the file in the archive according to JUnrar
    // (Notice that the filename also contains folders in the archive delimited by '\' instead of '/' )
    private String mImageFileName;

    // int to keep track of the number of the page in the comic
    private int mPageNumber;
    
    // The spinner to show when an image is loading
    ProgressBarCircularIndeterminate mSpinner;

    public static ComicPageFragment newInstance(String comicPath, String pageFileName, int page)
    {
        ComicPageFragment fragment = new ComicPageFragment();
        Bundle args = new Bundle();
        args.putString("ComicArchive", comicPath);
        args.putString("Page", pageFileName);
        args.putInt("PageNumber",page);
        fragment.setArguments(args);
        return fragment;
    }

    public ComicPageFragment() {
        // Required empty public constructor

    }

    //Function to load an image, gets called after extracting the image
    public void loadImage(String filename)
    {
        
        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity()).build();
            ImageLoader.getInstance().init(config);
        }
        mFullscreenComicView.setImageBitmap(null);
        if (filename!=null) {
            if (getActivity()!=null) {
                String imagePath = "file:///" + getActivity().getFilesDir().getPath() + "/" + filename;
                Log.d("loadImage", imagePath);
                ImageLoader.getInstance().displayImage(imagePath, mFullscreenComicView, new SimpleImageLoadingListener(){
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
                        if (mSpinner!=null)
                            mSpinner.setVisibility(View.VISIBLE);
                        mFullscreenComicView.setZoom(1);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        if (mSpinner!=null)
                            mSpinner.setVisibility(View.GONE);
                        mFullscreenComicView.setZoom(1);
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (mSpinner!=null)
                            mSpinner.setVisibility(View.GONE);
                        mFullscreenComicView.setZoom(1);
                    }
                });

            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.comic_displaypage_fragment,container, false);

        Bundle args = getArguments();

        mSpinner = (ProgressBarCircularIndeterminate) rootView.findViewById(R.id.spinner);
        mComicArchivePath = args.getString("ComicArchive");
        mImageFileName = args.getString("Page");
        mPageNumber = args.getInt("PageNumber");
        mFullscreenComicView = (TouchImageView) rootView.findViewById(R.id.fullscreen_comic);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        File file = new File(mComicArchivePath);

        if (Utilities.isZipArchive(file))
            new ExtractZipTask().execute();
        else
            new ExtractRarTask().execute();
    }


    private class ExtractRarTask extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... comicVar) {

            File comic = new File(mComicArchivePath);
            try {
                Archive arch = new Archive(comic);
                List<FileHeader> fileheaders = arch.getFileHeaders();

                for (int j = 0; j < fileheaders.size(); j++) {

                    String extractedImageFile = fileheaders.get(j).getFileNameString().substring(fileheaders.get(j).getFileNameString().lastIndexOf("\\")+1);

                    if (fileheaders.get(j).getFileNameString().equals(mImageFileName))
                    {
                        // get rid of special chars
                        if (extractedImageFile.contains("#"))
                            extractedImageFile = extractedImageFile.replaceAll("#","");
                        
                        File outputPage = new File(getActivity().getFilesDir(), extractedImageFile);
                        
                        if (!outputPage.exists()) {
                            FileOutputStream osPage = new FileOutputStream(outputPage);
                            arch.extractFile(fileheaders.get(j), osPage);
                        }

                        Log.d("Extract rar",extractedImageFile);
                        return extractedImageFile;
                    }
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        public void onPostExecute(String file)
        {
            loadImage(file);
        }

    }

    private class ExtractZipTask extends AsyncTask<Void, Void, String>
    {
        @Override
        protected String doInBackground(Void... comicVar) {

            try {
                ZipFile zipFile = new ZipFile(mComicArchivePath);
                
                List<net.lingala.zip4j.model.FileHeader> fileHeaders = zipFile.getFileHeaders();
                
                for (int i=0;i<fileHeaders.size();i++)
                {
                    String extractedImageFile;
                    if (fileHeaders.get(i).getFileName().contains("\\"))
                    {
                        extractedImageFile = fileHeaders.get(i).getFileName().substring(fileHeaders.get(i).getFileName().lastIndexOf("\\")+1);
                    }
                    else
                    {
                        extractedImageFile = fileHeaders.get(i).getFileName();
                    }

                    if (extractedImageFile.contains("/"))
                        extractedImageFile = extractedImageFile.substring(extractedImageFile.lastIndexOf("/")+1);

                    
                    Log.d("ExtractZip",extractedImageFile);
                    Log.d("mImageFileName", mImageFileName);
                    
                    if (extractedImageFile.equals(mImageFileName))
                    {
                        // get rid of special chars
                        if (extractedImageFile.contains("#"))
                            extractedImageFile = extractedImageFile.replaceAll("#","");

                        File outputPage = new File(getActivity().getFilesDir(), fileHeaders.get(i).getFileName());

                        if (!outputPage.exists()) {
                            //FileOutputStream osPage = new FileOutputStream(outputPage);
                            String extractPath = getActivity().getFilesDir().getAbsolutePath();
                            Log.d("Extractpath",extractPath);
                            zipFile.extractFile(fileHeaders.get(i), extractPath);
                        }

                        return fileHeaders.get(i).getFileName();
                    }
                }
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        public void onPostExecute(String file)
        {
            loadImage(file);
        }

    }



}
