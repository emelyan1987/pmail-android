package com.planckmail.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.planckmail.R;
import com.planckmail.activities.SelectFileAccountActivity;
import com.planckmail.activities.SelectFileAccountActivity.FileAccountType;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.utils.MimeUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.planckmail.enums.AccountType.BOX;
import static com.planckmail.enums.AccountType.DROP_BOX;
import static com.planckmail.enums.AccountType.GMAIL;
import static com.planckmail.enums.AccountType.MICROSOFT_EXCHANGE;
import static com.planckmail.enums.AccountType.OUTLOOK;
import static com.planckmail.enums.AccountType.YAHOO;

/**
 * Created by Taras Matolinets on 01.06.15.
 */
public class UserHelper {
    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    public static File copyInputStreamToFile(InputStream in, String fileName) {
        File newFile = new File(Environment.getExternalStorageDirectory() + PlanckMailApplication.PLANK_MAIL_FILES, fileName);
        if (newFile.exists())
            Log.i(PlanckMailApplication.TAG, "absolute patch of fileType " + newFile.getAbsolutePath());

        OutputStream out = null;
        try {
            out = new FileOutputStream(newFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        } finally {
            try {
                if (out != null)
                    out.close();

                in.close();
            } catch (Exception e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return newFile;
    }

    public static String getCurrentTimeZoneOffset() {
        DateTimeZone tz = DateTimeZone.getDefault();
        Long instant = DateTime.now().getMillis();

        long offsetInMilliseconds = tz.getOffset(instant);
        long hours = TimeUnit.MILLISECONDS.toHours(offsetInMilliseconds);

        return " + " + new DateTime(hours).toString("hh:mm ");
    }

    public static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append("\r\n");
        }
        return out.toString();
    }

    public static void hideKeyboard(Context context, View view) {
        // Check if no view has focus:
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static Drawable getAccountDrawable(Context context, AccountType accountType) {
        Drawable drawable;
        switch (accountType) {
            case GMAIL:
                drawable = ContextCompat.getDrawable(context, R.drawable.gmail_icon);
                break;
            case OUTLOOK:
                drawable = ContextCompat.getDrawable(context, R.drawable.outlook_icon);
                break;
            case YAHOO:
                drawable = ContextCompat.getDrawable(context, R.drawable.yahoo_icon);
                break;
            case DROP_BOX:
                drawable = ContextCompat.getDrawable(context, R.drawable.dropbox);
                break;
            case ONE_DRIVE:
                drawable = ContextCompat.getDrawable(context, R.drawable.one_drive);
                break;
            case GOOGLE_DRIVE:
                drawable = ContextCompat.getDrawable(context, R.drawable.google_drive);
                break;
            case BOX:
                drawable = ContextCompat.getDrawable(context, R.drawable.box);
                break;
            default:
                drawable = ContextCompat.getDrawable(context, R.drawable.exchange_icon);
                break;
        }
        return drawable;
    }

    public static AccountType getEmailAccountType(String email) {
        if (email.contains(GMAIL.toString()))
            return GMAIL;
        else if (email.contains(OUTLOOK.toString()))
            return OUTLOOK;
        else if (email.contains(YAHOO.toString()))
            return YAHOO;
        else
            return MICROSOFT_EXCHANGE;
    }

    public static Drawable getImageFile(Context context, String mimeType) {
        Drawable drawable;

        String fileFormat = MimeUtils.guessExtensionFromMimeType(mimeType);
        if (mimeType == null || fileFormat == null)
            return ContextCompat.getDrawable(context, R.drawable.ic_general_file_type);

        if (fileFormat.equalsIgnoreCase(FILE_FROMATS.DOC.toString()) || fileFormat.equalsIgnoreCase(FILE_FROMATS.DOCX.toString()))
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_doc_file_type);
        else if (fileFormat.equalsIgnoreCase(FILE_FROMATS.JPG.toString()) || fileFormat.equalsIgnoreCase(FILE_FROMATS.PNG.toString())) {
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_image_file_type);
        } else if (fileFormat.equalsIgnoreCase(FILE_FROMATS.PDF.toString())) {
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_pdf_file_type);
        } else if (fileFormat.equalsIgnoreCase(FILE_FROMATS.PPT.toString())) {
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_ppt_file_type);
        } else if (fileFormat.equalsIgnoreCase(FILE_FROMATS.ZIP.toString())) {
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_zip_file_type);
        } else
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_general_file_type);

        return drawable;
    }

    public static Drawable resize(Context context, Drawable image) {
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 120, 100, false);
        return new BitmapDrawable(context.getResources(), bitmapResized);
    }

    public static AccountType getFileAccountType(FileAccountType fileAccountType) {
        AccountType accountType = null;
        switch (fileAccountType) {
            case DROP_BOX:
                accountType = AccountType.DROP_BOX;
                break;
            case GOOGLE_DRIVE:
                accountType = AccountType.GOOGLE_DRIVE;
                break;
            case BOX:
                accountType = AccountType.BOX;
                break;
            case ONE_DRIVE:
                accountType = AccountType.ONE_DRIVE;
                break;
        }
        return accountType;
    }

    public static AccountInfo getAccountInfo(List<AccountInfo> listAccountInfo, String accountId) {
        AccountInfo accountInfo = null;
        for (AccountInfo a : listAccountInfo) {
            if (accountId.equals(a.accountId))
                accountInfo = a;
        }
        return accountInfo;
    }

    /**
     * @param state isEmail account
     */
    public static List<AccountInfo> getEmailAccountList(boolean state) {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        return manager.getEmailAccountInfoList(state);
    }

    /**
     * generate light color
     */
    public static int getLightColor() {
        int color;
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        float[] colors = {r, g, b};
        color = Color.HSVToColor(colors);

        return color;
    }

    public enum FILE_FROMATS {
        DOC, DOCX, ZIP, JPG, PNG, PPT, GTF, PDF
    }

}
