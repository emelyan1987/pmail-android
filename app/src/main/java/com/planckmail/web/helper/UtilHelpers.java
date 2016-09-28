package com.planckmail.web.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.View;

import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.util.List;

/**
 * Created by Taras Matolinets on 19.05.15.
 */
public class UtilHelpers {


    public static String buildTitle(String title) {
        String customTitle = "";
        String[] arrayTitle = title.split(" ");
        String one = arrayTitle[0].replaceAll("[(+@*&'%$#%^*!=]", "");

        if (arrayTitle.length >= 2 && !TextUtils.isEmpty(one)) {
            String two = arrayTitle[1].replaceAll("[(+@*&'%$#%^*!=]", "");

            if (!TextUtils.isEmpty(two))
                customTitle = one.charAt(0) + " " + two.charAt(0);
            else
                customTitle = String.valueOf(one.charAt(0));

        } else if (!TextUtils.isEmpty(one))
            customTitle = String.valueOf(one.charAt(0));

        return customTitle;
    }

    public static String getParticipants(List<Participant> list) {
        StringBuilder builder = new StringBuilder();

        if (!list.isEmpty()) {
            //participant who sent email
            Participant participants = list.get(0);

            if (!TextUtils.isEmpty(participants.name))
                builder.append(participants.name);
            else
                builder.append(participants.email);
        }

        return builder.toString();
    }


    public static Object extractBitmapFromTextView(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        view.measure(spec, spec);

        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());

        view.draw(c);

        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();

        return new BitmapDrawable(viewBmp);
    }
}
