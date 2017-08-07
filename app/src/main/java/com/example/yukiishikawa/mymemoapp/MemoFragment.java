package com.example.yukiishikawa.mymemoapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by yukiishikawa on 2017/08/06.
 */

public class MemoFragment extends Fragment {
    private MemoEditText mMemoEditText;

    private Uri mMemoUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // レイアウトXMLからViewを生成
        View view = inflater.inflate(R.layout.fragment_memo, container, false);

        mMemoEditText = (MemoEditText)view.findViewById(R.id.Memo);

        return view;
    }

    // 設定を反映する
    public void reflectSettings() {
        Context context = getActivity();

        if (context != null) {
            // SharedPreferencesから値を取得して設定を反映
            setFontSize(SettingPrefUtil.getFontSize(context));
            setTypeface(SettingPrefUtil.getTypeface(context));
            setMemoColor(SettingPrefUtil.isScreenReverse(context));
        }
    }

    // 文字サイズの設定を反映する
    public void setFontSize(float fontSizePx) {
        mMemoEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx);
    }

    // 文字装飾の設定を反映する
    public void setTypeface(int typeface) {
        mMemoEditText.setTypeface(Typeface.DEFAULT, typeface);
    }

    // 色の反転の設定を反映する
    public void setMemoColor(boolean reverse) {
        int backgroundColor = reverse ? Color.BLACK : Color.WHITE;
        int textColor = reverse ? Color.WHITE : Color.BLACK;

        mMemoEditText.setBackgroundColor(backgroundColor);
        mMemoEditText.setTextColor(textColor);
    }

    // 保存する
    public void save() {
        if (mMemoUri != null) {
            MemoRepository.update(getActivity(), mMemoUri, mMemoEditText.getText().toString());
        } else {
            // 新規作成
            MemoRepository.create(getActivity(), mMemoEditText.getText().toString());
        }

        // 保存しました、と表示
        Toast.makeText(getActivity(), "保存しました", Toast.LENGTH_SHORT).show();
    }

    // 読み込む
    public void load(Uri uri) {
        // 現在のURIを変更する
        mMemoUri = uri;

        if (uri != null) {
            // メモを読み込む
            String memo = MemoRepository.findMemoByUri(getActivity(), uri);

            // EditTextに反映
            mMemoEditText.setText(memo);
        } else {
            // URIがnullの場合には、メモをクリアするだけ
            mMemoEditText.setText(null);
        }
    }
}
