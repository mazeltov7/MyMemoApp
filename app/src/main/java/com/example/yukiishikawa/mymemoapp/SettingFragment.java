package com.example.yukiishikawa.mymemoapp;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by yukiishikawa on 2017/08/20.
 */

public class SettingFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ファイル名を指定する
        getPreferenceManager().setSharedPreferencesName(
                SettingRepository.PREF_FILE_NAME;
        );

        // Preferencesの設定ファイルを指定
        addPreferencesFromResource(R.xml.preferences);
    }

}
