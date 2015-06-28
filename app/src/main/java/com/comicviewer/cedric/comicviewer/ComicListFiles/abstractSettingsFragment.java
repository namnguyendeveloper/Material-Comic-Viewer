package com.comicviewer.cedric.comicviewer.ComicListFiles;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.comicviewer.cedric.comicviewer.DrawerActivity;
import com.comicviewer.cedric.comicviewer.Model.Comic;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.CustomCheckBoxPreference;
import com.comicviewer.cedric.comicviewer.PreferenceFiles.PreferenceSetter;
import com.comicviewer.cedric.comicviewer.R;

import java.util.ArrayList;

/**
 * Created by CV on 28/06/2015.
 */
public abstract class AbstractSettingsFragment extends PreferenceFragment {

    public AbstractSettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        addFilenameFormatSettings();
        addUnhidePreference();
        addRemovePathsPreference();
        addAppThemeSettings();
        addFabColorPreference();
        addMangaPreference();
        addBackgroundChangeListener();
        addViewPagerAnimationPreference();
        addClearSavedComicDataPreference();

        if (PreferenceSetter.getBackgroundColorPreference(getActivity())!= getResources().getColor(R.color.WhiteBG))
        {
            PreferenceSetter.setBackgroundColorPreference(getActivity());
        }
        else
        {
            getActivity().getWindow().getDecorView().setBackgroundColor(getActivity().getResources().getColor(R.color.BlueGrey));
            if (Build.VERSION.SDK_INT>20)
                getActivity().getWindow().setNavigationBarColor(getResources().getColor(R.color.BlueGrey));
        }
    }

    private void addClearSavedComicDataPreference() {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("FunctionalityCategory");
        Preference clearDataPref = new Preference(getActivity());

        clearDataPref.setKey("clearData");
        clearDataPref.setTitle("Clear comic data");
        clearDataPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceSetter.saveComicList(getActivity(), new ArrayList<Comic>());
                Toast.makeText(getActivity(), "Data cleared!", Toast.LENGTH_LONG);
                return true;
            }
        });
        targetCategory.addPreference(clearDataPref);
    }

    private void addFabColorPreference()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("LayoutCategory");

        final ListPreference accentColorListPreference = new ListPreference(getActivity());
        accentColorListPreference.setKey(PreferenceSetter.ACCENT_COLOR);

        CharSequence[] entries = {
                getString(R.string.app_theme_setting),
                getString(R.string.teal),
                getString(R.string.red),
                getString(R.string.orange),
                getString(R.string.blue),
                getString(R.string.pink),
                getString(R.string.purple),
                getString(R.string.deep_purple),
                getString(R.string.green),
                getString(R.string.yellow),
                getString(R.string.gold),
                getString(R.string.indigo)};
        CharSequence[] entryValues = {
                ""+PreferenceSetter.getAppThemeColor(getActivity()),
                ""+getResources().getColor(R.color.Teal),
                ""+getResources().getColor(R.color.Red),
                ""+getResources().getColor(R.color.Orange),
                ""+getResources().getColor(R.color.Blue),
                ""+getResources().getColor(R.color.Pink),
                ""+getResources().getColor(R.color.Purple),
                ""+getResources().getColor(R.color.DeepPurple),
                ""+getResources().getColor(R.color.Green),
                ""+getResources().getColor(R.color.Yellow),
                ""+getResources().getColor(R.color.Gold),
                ""+getResources().getColor(R.color.Indigo)};

        accentColorListPreference.setEntries(entries);
        accentColorListPreference.setEntryValues(entryValues);
        accentColorListPreference.setDefaultValue("" + PreferenceSetter.getAppThemeColor(getActivity()));

        accentColorListPreference.setTitle(getString(R.string.app_accent_color_setting));
        accentColorListPreference.setDialogTitle(getString(R.string.choose_accent_color));

        accentColorListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object newValue) {

                PreferenceSetter.saveAppAccentColor(getActivity(), (CharSequence) newValue);
                return false;
            }
        });

        targetCategory.addPreference(accentColorListPreference);

    }

    private void addViewPagerAnimationPreference()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("ReadCategory");

        ListPreference viewPagerAnimationPreference = new ListPreference(getActivity());


        viewPagerAnimationPreference.setKey(PreferenceSetter.VIEWPAGER_ANIMATION_SETTING);
        viewPagerAnimationPreference.setTitle(getString(R.string.page_flip_animation_setting));
        viewPagerAnimationPreference.setEntries(getResources().getTextArray(R.array.Viewpager_animations));
        viewPagerAnimationPreference.setEntryValues(getResources().getTextArray(R.array.Viewpager_animations));
        viewPagerAnimationPreference.setDefaultValue(getString(R.string.none));

        targetCategory.addPreference(viewPagerAnimationPreference);
    }

    private void addBackgroundChangeListener()
    {
        Preference backgroundPref = (Preference) findPreference("backgroundColor");
        backgroundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String color = (String) newValue;

                PreferenceSetter.setBackgroundColorPreference(getActivity(), color);
                if (PreferenceSetter.getBackgroundColorPreference(getActivity())!= getResources().getColor(R.color.WhiteBG)) {
                    ((DrawerActivity) getActivity()).setDrawerBackgroundColor(PreferenceSetter.getBackgroundColorPreference(getActivity()));
                    PreferenceSetter.setBackgroundColorPreference(getActivity());
                }
                else
                    ((DrawerActivity)getActivity()).setDrawerBackgroundColor(getResources().getColor(R.color.BlueGrey));

                return true;
            }
        });

    }

    private void addFilenameFormatSettings()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("FunctionalityCategory");

        ListPreference fileFormatList = new ListPreference(getActivity());

        String[] entries = getActivity().getResources().getStringArray(R.array.File_formats);

        CharSequence[] charSequences = new CharSequence[entries.length];

        for (int i=0;i<charSequences.length;i++)
        {
            charSequences[i] = entries[i];
        }

        fileFormatList.setKey(PreferenceSetter.FILE_FORMAT_SETTING);
        fileFormatList.setSummary(getString(R.string.extra_chars_removed_note));
        fileFormatList.setEntries(charSequences);
        fileFormatList.setEntryValues(charSequences);
        fileFormatList.setTitle(getString(R.string.file_format_setting));
        fileFormatList.setDialogTitle(getString(R.string.choose_file_format));
        fileFormatList.setDefaultValue(getActivity().getResources().getString(R.string.file_format_1));

        targetCategory.addPreference(fileFormatList);
    }

    private void addAppThemeSettings()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("LayoutCategory");

        final ListPreference appThemeListPreference = new ListPreference(getActivity());
        appThemeListPreference.setKey(PreferenceSetter.APP_THEME_COLOR);

        CharSequence[] entries = {
                getString(R.string.teal),
                getString(R.string.red),
                getString(R.string.orange),
                getString(R.string.blue),
                getString(R.string.pink),
                getString(R.string.purple),
                getString(R.string.deep_purple),
                getString(R.string.green),
                getString(R.string.yellow),
                getString(R.string.gold),
                getString(R.string.indigo)};
        CharSequence[] entryValues = {""+getResources().getColor(R.color.Teal),
                ""+getResources().getColor(R.color.Red),
                ""+getResources().getColor(R.color.Orange),
                ""+getResources().getColor(R.color.Blue),
                ""+getResources().getColor(R.color.Pink),
                ""+getResources().getColor(R.color.Purple),
                ""+getResources().getColor(R.color.DeepPurple),
                ""+getResources().getColor(R.color.Green),
                ""+getResources().getColor(R.color.Yellow),
                ""+getResources().getColor(R.color.Gold),
                ""+getResources().getColor(R.color.Indigo)};

        appThemeListPreference.setEntries(entries);
        appThemeListPreference.setEntryValues(entryValues);
        appThemeListPreference.setDefaultValue("" + PreferenceSetter.getAppThemeColor(getActivity()));

        appThemeListPreference.setTitle(getString(R.string.app_theme_setting));
        appThemeListPreference.setSummary(getString(R.string.restart_note));
        appThemeListPreference.setDialogTitle(getString(R.string.choose_app_color));

        appThemeListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object newValue) {

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.warning))
                        .content(getString(R.string.restart_dialog))
                        .negativeColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .positiveColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .positiveText(getString(R.string.accept))
                        .negativeText(getString(R.string.cancel))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                PreferenceSetter.saveAppThemeColor(getActivity(), (CharSequence) newValue);
                                appThemeListPreference.setOnPreferenceChangeListener(null);
                                getActivity().finish();
                            }
                        })
                        .show();

                return false;
            }
        });

        targetCategory.addPreference(appThemeListPreference);

    }

    private void addMangaPreference()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("FunctionalityCategory");

        CustomCheckBoxPreference mangaPreference = new CustomCheckBoxPreference(getActivity());


        mangaPreference.setKey(PreferenceSetter.MANGA_SETTING);
        mangaPreference.setSummary(getString(R.string.manga_setting_note));
        mangaPreference.setTitle(getString(R.string.manga_setting));
        mangaPreference.setDefaultValue(false);

        targetCategory.addPreference(mangaPreference);
    }

    private void addUnhidePreference()
    {
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("FunctionalityCategory");

        Preference unhideListPreference = new Preference(getActivity());

        unhideListPreference.setTitle(getString(R.string.unhide_setting));

        unhideListPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                ArrayList<String> hiddenPaths = PreferenceSetter.getHiddenFiles(getActivity());

                CharSequence[] charSequences = new CharSequence[hiddenPaths.size()];

                for (int i = 0; i < charSequences.length; i++) {
                    charSequences[i] = hiddenPaths.get(i);
                }

                new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.unhide_files))
                        .positiveColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .positiveText(getString(R.string.unhide))
                        .negativeColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .negativeText(getString(R.string.cancel))
                        .items(charSequences)
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                                materialDialog.dismiss();
                                for (int i = 0; i < charSequences.length; i++) {
                                    PreferenceSetter.removeHiddenPath(getActivity(), charSequences[i].toString());
                                }

                                return false;
                            }
                        }).show();
                return true;
            }
        });



        targetCategory.addPreference(unhideListPreference);
    }

    private void addRemovePathsPreference()
    {
        Preference removePathsPreference = new Preference(getActivity());

        removePathsPreference.setTitle(getString(R.string.remove_filepaths_setting));
        removePathsPreference.setSummary(getString(R.string.path_preference_summary));


        removePathsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final String defaultPath = Environment.getExternalStorageDirectory().toString() + "/ComicViewer";
                ArrayList<String> filePaths = PreferenceSetter.getFilePathsFromPreferences(getActivity());

                if (!filePaths.contains(defaultPath))
                    filePaths.add(defaultPath);

                CharSequence[] charSequences = new CharSequence[filePaths.size()];

                for (int i = 0; i < charSequences.length; i++) {
                    charSequences[i] = filePaths.get(i);
                }


                new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.remove_filepaths))
                        .positiveColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .positiveText(getString(R.string.remove))
                        .negativeColor(PreferenceSetter.getAppThemeColor(getActivity()))
                        .negativeText(getString(R.string.cancel))
                        .items(charSequences)
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                                materialDialog.dismiss();
                                for (int i = 0; i < charSequences.length; i++) {
                                    if (!charSequences.toString().equals(defaultPath))
                                        PreferenceSetter.removeFilePath(getActivity(), charSequences[i].toString());
                                }

                                return false;
                            }
                        }).show();
                return true;
            }
        });


        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("FunctionalityCategory");

        targetCategory.addPreference(removePathsPreference);
    }

}