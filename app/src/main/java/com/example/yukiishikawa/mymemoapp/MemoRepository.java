package com.example.yukiishikawa.mymemoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

/**
 * Created by yukiishikawa on 2017/07/29.
 */

public class MemoRepository {
    // ファイル名フォーマット prefix-yyyy-mm-dd-HH-MM-SS.txt
    private static final String MEMO_FILE_FORMAT = "%1$s-%2$tF-%2$tH-%2$tM-%2$tS.txt";

    // インスタンスを作らせない
    private MemoRepository() {}

    // メモを新規に保存する
    public static Uri create(Context context, String memo) {
        // 出力先ディレクトリを取得
        File outputDir = getOutputDir(context);

        if (outputDir == null) {
            // 何らかの原因でディレクトリが見つからなかった
            return null;
        }

        // 出力先ファイル名を決定する
        File outputFile = getFileName(context, outputDir);
        if (outputFile == null || writeToFile(outputFile, memo)) {
            // ファイルの書き込みに失敗した場合
            return null;
        }

        // メモのタイトルは、文章内容から決定
        String title = memo.length() > 10 ? memo.substring(0, 10) : memo;

        // データベースに登録するその他の情報
        ContentValues values = new ContentValues();
        values.put(MemoDBHelper.TITLE, title);
        values.put(MemoDBHelper.DATA, outputFile.getAbsolutePath());
        values.put(MemoDBHelper.DATE_ADDED, System.currentTimeMillis());

        // コンテントプロバイダに挿入する
        return context.getContentResolver().insert(MemoProvider.CONTENT_URI, values);
    }

    // 既存のメモを更新する
    public static int update(Context context, Uri uri, String memo) {
        // 引数のURIをもとに、まずはデータベースから検索する
        // ID部分
        String id = uri.getLastPathSegment();
        // 検索
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MemoDBHelper.DATA}, MemoDBHelper._ID + " = ?", new String[]{id}, null);
        if (cursor == null) {
            return 0;
        }

        String filePath = null;
        while (cursor.moveToNext()) {
            filePath = cursor.getString(cursor.getColumnIndex(MemoDBHelper.DATA));
        }

        cursor.close();

        if (TextUtils.isEmpty(filePath)) {
            return 0;
        }

        File outputFile = new File(filePath);
        if (writeToFile(outputFile, memo)) {
            return 0;
        }

        return 1;
    }

    // メモを読み込む
    public static String findMemoByUri(Context context, Uri uri) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                }
            }
        } catch (FileNotFoundException fnfe) {
            // ファイルが削除されるなどして見つからなかった場合
            fnfe.printStackTrace();
            return context.getString(R.string.error_memo_file_not_found);
        } catch (IOException ioe) {
            // ファイル読み込みに失敗した場合
            ioe.printStackTrace();
            return context.getString(R.string.error_memo_file_load_failed);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return builder.toString();
    }

    // メモの一覧を取得
    public static Cursor query(Context context) {
        return context.getContentResolver().query((MemoProvider.CONTENT_URI, null, null, null, MemoDBHelper.DATE_MODIFIED + " DESC"));
    }

    // メモの出力先ディレクトリを取得する
    private static File getOutputDir(Context context) {
        File outputDir;
        if (Build.VERSION.SDK_INT >= 19) {
            outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        } else {
            outputDir = new File(context.getExternalFilesDir(null), "Documents");
        }

        if (outputDir == null) {
            return null;
        }

        boolean isExist = true;

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            isExist = outputDir.mkdirs();
        }

        if (isExist) {
            return outputDir;
        } else {
            // ディレクトリの作成に失敗した場合
            return null;
        }
    }

    // 出力先ファイルを取得する
    public static File getFileName(Context context, File outputDir) {
        String fileNamePrefix = SettingPrefUtil.getKeyFileNamePrefix(context);

        Calendar now = Calendar.getInstance();

        String fileName = String.format(MEMO_FILE_FORMAT, fileNamePrefix, now);

        return new File(outputDir, fileName);
    }

    // ファイルにメモを書き込む
    private static boolean writeToFile(File outputFile, String memo) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
            writer.write(memo);
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    // ファイルとして保存し、データベースに保存する
    public static Uri store(Context context, String memo) {
        File outputDir;

        if (Build.VERSION.SDK_INT >= 19) {
            // API level 19以上の場合には、ドキュメントファイル用共有ディレクトリ
            outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        } else {
            // API level 19未満の場合には、”Documents"ディレクトリを作成して保存する
            outputDir = new File(context.getExternalFilesDir(null), "Documents");
        }

        if (outputDir == null) {
            // 外部ストレージがマウントされていない等の場合はなにもしない
            return null;
        }

        boolean hasDirectory = true;

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            // 保存ディスクが存在しない場合は作成する
            hasDirectory = outputDir.mkdirs();
        }

        if (!hasDirectory) {
            // ディレクトリの作成に失敗した場合は何もしない
            return null;
        }

        // ファイルに保存する
        File outputFile = saveAsFile(context, outputDir, memo);

        if (outputFile == null) {
            // ファイルの書き込みに失敗した場合
            return null;
        }

        // タイトルは本文から決定する
        String title = memo.length() > 10 ? memo.substring(0, 10) : memo;
        // DBに保存するため、ContentValuesに詰める
        ContentValues values = new ContentValues();
        values.put(MemoDBHelper.TITLE, title);
        values.put(MemoDBHelper.DATA, outputFile.getAbsolutePath());
        values.put(MemoDBHelper.DATE_ADDED, System.currentTimeMillis());

        // ContentProviderに保存する
        return context.getContentResolver().insert(MemoProvider.CONTENT_URI, values);
    }

    private static File saveAsFile(Context context, File outputDir, String memo) {
        // ファイル名のプレフィックスの設定を取得する
        String fileNamePrefix = SettingPrefUtil.getKeyFileNamePrefix(context);
        // 現在の日時を取得する
        Calendar now = Calendar.getInstance();
        // ファイル名をフォーマットに従って決定する
        String fileName = String.format(MEMO_FILE_FORMAT, fileNamePrefix, now);
        // Fileオブジェクトを取得
        File outputFile = new File(outputDir, fileName);

        FileWriter writer = null;
        try {
            // ファイルに書き込みを行う
            writer = new FileWriter(outputFile);
            writer.write(memo);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return outputFile;

    }
}
