package com.digitalhigh.digitalhacks;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import eu.chainfire.libsuperuser.Shell;


public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener, OnPreferenceChangeListener {


    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    private void setupSimplePreferencesScreen() {
        addPreferencesFromResource(R.xml.pref_display);

        addPreferencesFromResource(R.xml.pref_status_bar);

        addPreferencesFromResource(R.xml.pref_navigation_bar);

        addPreferencesFromResource(R.xml.pref_inputs);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceScreen) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); j++) {
                    Preference subPreference = preferenceGroup.getPreference(j);
                    if (subPreference instanceof PreferenceCategory) {
                        PreferenceGroup subPreferenceGroup = (PreferenceGroup) subPreference;
                        for (int k = 0; k < subPreferenceGroup.getPreferenceCount(); k++) {
                            Preference anotherSubPreference = subPreferenceGroup.getPreference(k);
                            if (anotherSubPreference instanceof ListPreference) {
                                bindPreferenceSummaryToValue(anotherSubPreference);
                            } else {
                                anotherSubPreference.setOnPreferenceClickListener(this);
                            }
                        }
                    }
                    if (subPreference instanceof ListPreference) {
                        bindPreferenceSummaryToValue(subPreference);
                    } else if (subPreference instanceof ColorPickerPreference) {
                        subPreference.setOnPreferenceChangeListener(this);
                        initColorPicker((ColorPickerPreference) subPreference);
                    } else {
                        subPreference.setOnPreferenceClickListener(this);
                    }
                }
            }
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (findPreference(key) instanceof SwitchPreference) {
                Settings.System.putInt(getContentResolver(), key, sharedPreferences.getBoolean(key, false) ? 1 : 0);
            } else if (findPreference(key) instanceof ListPreference) {
                Settings.System.putString(getContentResolver(), key, sharedPreferences.getString(key, ""));
            }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey() != null) {

        }
        return false;
    }

    private int mId = 0;

    private void showNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                                //Should move these to strings.xml but I doubt I'll need translations so I'll keep it this way for now.
                        .setContentTitle("TEST")
                        .setContentText("Testing heads up.")
                        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private void setLcdDensity(String newDensity) {
        Shell.SU.run("mount -o remount,rw /system /system");
        Shell.SU.run("busybox sed -i 's/ro.sf.lcd_density=[0-9][0-9][0-9]/ro.sf.lcd_density="
                + newDensity + "/g' /system/build.prop");
        Shell.SU.run("mount -o remount,ro /system /system");
        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("You must reboot in order for these changes to take effect.")
                .setPositiveButton("Later", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Now", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PowerManager pm = (PowerManager) SettingsActivity.this
                                .getSystemService(Context.POWER_SERVICE);
                        pm.reboot("Resetting density");
                    }
                });
        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }

    private void initColorPicker(ColorPickerPreference preference) {
        int intColor = Settings.System.getInt(getContentResolver(),
                getString(R.string.pref_key_default_color), -2);
        preference.setNewPreviewColor(intColor);

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().matches(getString(R.string.pref_key_traffic_color))) {
                        return true;
        }
        if (preference.getKey().matches(getString(R.string.pref_key_miui_battery_color))) {
            return true;
        }
                return false;
        }
}
