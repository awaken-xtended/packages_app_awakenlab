/*
 * Copyright (C) 2022 Yet Another AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.custom.fragments;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.R;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.custom.colorpicker.ColorPickerPreference;
import com.android.settings.custom.preference.CustomSeekBarPreference;
import com.android.settings.custom.preference.SwitchPreference;

import java.lang.CharSequence;

import org.json.JSONException;
import org.json.JSONObject;

@SearchIndexable
public class MonetSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String OVERLAY_CATEGORY_ACCENT_COLOR =
            "android.theme.customization.accent_color";
    private static final String OVERLAY_CATEGORY_SYSTEM_PALETTE =
            "android.theme.customization.system_palette";
    private static final String OVERLAY_CATEGORY_THEME_STYLE =
            "android.theme.customization.theme_style";
    private static final String OVERLAY_COLOR_SOURCE =
            "android.theme.customization.color_source";
    private static final String OVERLAY_COLOR_BOTH =
            "android.theme.customization.color_both";
     private static final String OVERLAY_LUMINANCE_FACTOR =
            "android.theme.customization.luminance_factor";
    private static final String OVERLAY_CHROMA_FACTOR =
            "android.theme.customization.chroma_factor";
    private static final String OVERLAY_TINT_BACKGROUND =
            "android.theme.customization.tint_background";        
    private static final String COLOR_SOURCE_PRESET = "preset";
    private static final String COLOR_SOURCE_HOME = "home_wallpaper";
    private static final String COLOR_SOURCE_LOCK = "lock_wallpaper";

    private static final String PREF_THEME_STYLE = "theme_style";
    private static final String PREF_COLOR_SOURCE = "color_source";
    private static final String PREF_ACCENT_COLOR = "accent_color";
    private static final String PREF_LUMINANCE_FACTOR = "luminance_factor";
    private static final String PREF_CHROMA_FACTOR = "chroma_factor";
    private static final String PREF_TINT_BACKGROUND = "tint_background";

    private ListPreference mThemeStylePref;
    private ListPreference mColorSourcePref;
    private ColorPickerPreference mAccentColorPref;
    private CustomSeekBarPreference mLuminancePref;
    private CustomSeekBarPreference mChromaPref;
    private SwitchPreference mTintBackgroundPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.custom_settings_monet);

        mThemeStylePref = findPreference(PREF_THEME_STYLE);
        mColorSourcePref = findPreference(PREF_COLOR_SOURCE);
        mAccentColorPref = findPreference(PREF_ACCENT_COLOR);
        mLuminancePref = findPreference(PREF_LUMINANCE_FACTOR);
        mChromaPref = findPreference(PREF_CHROMA_FACTOR);
        mTintBackgroundPref = findPreference(PREF_TINT_BACKGROUND);

    updatePreferences();

        mThemeStylePref.setOnPreferenceChangeListener(this);
        mColorSourcePref.setOnPreferenceChangeListener(this);
        mAccentColorPref.setOnPreferenceChangeListener(this);
        mLuminancePref.setOnPreferenceChangeListener(this);
        mChromaPref.setOnPreferenceChangeListener(this);
        mTintBackgroundPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    private void updatePreferences() {    
        final String overlayPackageJson = Settings.Secure.getStringForUser(
                getActivity().getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT);
        if (overlayPackageJson != null && !overlayPackageJson.isEmpty()) {
            try {
                final JSONObject object = new JSONObject(overlayPackageJson);
                final String style = object.optString(OVERLAY_CATEGORY_THEME_STYLE, null);
                final String source = object.optString(OVERLAY_COLOR_SOURCE, null);
                final String color = object.optString(OVERLAY_CATEGORY_SYSTEM_PALETTE, null);
                 final boolean both = object.optInt(OVERLAY_COLOR_BOTH, 0) == 1;
                final boolean tintBG = object.optInt(OVERLAY_TINT_BACKGROUND, 0) == 1;
                final float lumin = (float) object.optDouble(OVERLAY_LUMINANCE_FACTOR, 1d);
                final float chroma = (float) object.optDouble(OVERLAY_CHROMA_FACTOR, 1d); 
                // style handling
                boolean styleUpdated = false;
                if (style != null && !style.isEmpty()) {
                    for (CharSequence value : mThemeStylePref.getEntryValues()) {
                        if (value.toString().equals(style)) {
                            styleUpdated = true;
                            break;
                        }
                    }
                    if (styleUpdated) {
                        updateListByValue(mThemeStylePref, style);
                    }
                }
                if (!styleUpdated) {
                    updateListByValue(mThemeStylePref,
                            mThemeStylePref.getEntryValues()[0].toString());
                }
                // color handling
                final String sourceVal = (source == null || source.isEmpty() ||
                        (source.equals(COLOR_SOURCE_HOME) && both)) ? "both" : source;
                updateListByValue(mColorSourcePref, sourceVal);
                final boolean enabled = updateAccentEnablement(sourceVal);
                if (enabled && color != null && !color.isEmpty()) {
                    mAccentColorPref.setNewPreviewColor(
                            ColorPickerPreference.convertToColorInt(color));
                }
                // etc
                int luminV = 0;
                if (lumin > 1d) luminV = Math.round((lumin - 1f) * 100f);
                else if (lumin < 1d) luminV = -1 * Math.round((1f - lumin) * 100f);
                mLuminancePref.setValue(luminV);
                int chromaV = 0;
                if (chroma > 1d) chromaV = Math.round((chroma - 1f) * 100f);
                else if (chroma < 1d) chromaV = -1 * Math.round((1f - chroma) * 100f);
                mChromaPref.setValue(chromaV);
                mTintBackgroundPref.setChecked(tintBG);     
            } catch (JSONException | IllegalArgumentException ignored) {}
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mThemeStylePref) {
            String value = (String) newValue;
         setStyleValue(value);   
            updateListByValue(mThemeStylePref, value, false);
            return true;
        } else if (preference == mColorSourcePref) {
            String value = (String) newValue;
            setStyleValue(value);
            updateListByValue(mColorSourcePref, value, false);
            updateAccentEnablement(value);
            return true;
        } else if (preference == mAccentColorPref) {
            int value = (Integer) newValue;
        setColorValue(value);
            return true;
        } else if (preference == mLuminancePref) {
            int value = (Integer) newValue;
            setLuminanceValue(value);   
            return true;
        } else if (preference == mChromaPref) {
            int value = (Integer) newValue;
            setChromaValue(value);
            return true;
        } else if (preference == mTintBackgroundPref) {
            boolean value = (Boolean) newValue;
            setTintBackgroundValue(value);
            return true;
        }
        return false;
    }

    private void updateListByValue(ListPreference pref, String value) {
        updateListByValue(pref, value, true);
    }

    private void updateListByValue(ListPreference pref, String value, boolean set) {
        if (set) pref.setValue(value);
        final int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
    }

    private boolean updateAccentEnablement(String source) {
        final boolean shouldEnable = source != null && source.equals(COLOR_SOURCE_PRESET);
        mAccentColorPref.setEnabled(shouldEnable);
        return shouldEnable;
    }

     private JSONObject getSettingsJson() throws JSONException {
        final String overlayPackageJson = Settings.Secure.getStringForUser(
         getActivity().getContentResolver(),
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,    
               UserHandle.USER_CURRENT);              
                JSONObject object;
        if (overlayPackageJson == null || overlayPackageJson.isEmpty())
            return new JSONObject();
        return new JSONObject(overlayPackageJson);
    }

        private void putSettingsJson(JSONObject object) {
           Settings.Secure.putStringForUser(
                    getActivity().getContentResolver(),
                   Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                  object.toString(), UserHandle.USER_CURRENT);
        }

    private void setStyleValue(String style) {     
        try {
            JSONObject object;
            JSONObject object = getSettingsJson();
            object.putOpt(OVERLAY_CATEGORY_THEME_STYLE, style);
            putSettingsJson(object);
        } catch (JSONException | IllegalArgumentExceptiion ignored) {}
    } 
           private void setSourceValue(String source) {
        try {
            JSONObject object = getSettingsJson();
            if (source.equals("both")) {
                object.putOpt(OVERLAY_COLOR_BOTH, 1);
                object.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME);
            } else {
                object.remove(OVERLAY_COLOR_BOTH);
                object.putOpt(OVERLAY_COLOR_SOURCE, source);
           
            }
            if (!source.equals(COLOR_SOURCE_PRESET)) {
                object.remove(OVERLAY_CATEGORY_ACCENT_COLOR);
                object.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE);
            }
                        putSettingsJson(object);
        } catch (JSONException | IllegalArgumentException ignored) {}
    }

    private void setColorValue(int color) {
        try {
            JSONObject object = getSettingsJson();
            final String rgbColor = ColorPickerPreference.convertToRGB(color).replace("#", "");
            object.putOpt(OVERLAY_CATEGORY_ACCENT_COLOR, rgbColor);
            object.putOpt(OVERLAY_CATEGORY_SYSTEM_PALETTE, rgbColor);
            object.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_PRESET);
            putSettingsJson(object);
        } catch (JSONException | IllegalArgumentException ignored) {}
    }
    private void setLuminanceValue(int lumin) {
        try {
            JSONObject object = getSettingsJson();
            if (lumin == 0)
                object.remove(OVERLAY_LUMINANCE_FACTOR);
            else
                object.putOpt(OVERLAY_LUMINANCE_FACTOR, 1d + ((double) lumin / 100d));
            putSettingsJson(object);
        } catch (JSONException | IllegalArgumentException ignored) {}
    }

    private void setChromaValue(int chroma) {
        try {
            JSONObject object = getSettingsJson();
            if (chroma == 0)
                object.remove(OVERLAY_CHROMA_FACTOR);
            else
                object.putOpt(OVERLAY_CHROMA_FACTOR, 1d + ((double) chroma / 100d));
            putSettingsJson(object);
        } catch (JSONException | IllegalArgumentException ignored) {}
    }

    private void setTintBackgroundValue(boolean tint) {
        try {
            JSONObject object = getSettingsJson();
            if (!tint) object.remove(OVERLAY_TINT_BACKGROUND);
            else object.putOpt(OVERLAY_TINT_BACKGROUND, 1);
            putSettingsJson(object);       
        } catch (JSONException | IllegalArgumentException ignored) {}
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.HAVOC_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.custom_settings_monet);
}