package com.comicviewer.cedric.comicviewer.PreferenceFiles;

import android.app.Activity;
import android.os.Bundle;

import com.comicviewer.cedric.comicviewer.R;


public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, SettingsFragment.newInstance()).commit();

        getActionBar().setTitle("Settings");
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.TealDark));

    }

    @Override

    public void onResume()
    {
        super.onResume();
    }

}