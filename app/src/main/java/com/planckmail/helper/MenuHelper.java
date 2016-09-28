package com.planckmail.helper;

import android.content.Context;

import com.planckmail.R;
import com.planckmail.activities.MenuActivity;
import com.planckmail.enums.Folders;
import com.planckmail.web.response.nylas.wrapper.Folder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 01.10.15.
 */
public class MenuHelper {
    public static String setTileToolbar(Context context, int childPosition) {
        String title = null;
        switch (childPosition) {
            case MenuActivity.INBOX:
                title = context.getString(R.string.item_inbox);
                break;
            case MenuActivity.DRAFTS:
                title = context.getString(R.string.item_draft);
                break;
            case MenuActivity.TRASH:
                title = context.getString(R.string.item_trash);
                break;
            case MenuActivity.SENT_MAIL:
                title = context.getString(R.string.item_sent);
                break;
            case MenuActivity.ALL_MAIL:
                title = context.getString(R.string.item_archive);
                break;
            case MenuActivity.STARRED:
                title = context.getString(R.string.item_starred);
                break;
            case MenuActivity.SPAM:
                title = context.getString(R.string.item_spam);
                break;

        }
        return title;
    }

    public static List<Folder> getFilteredFolders(List<Folder> folders) {
        List<Folder> filteredFolders = new ArrayList<>();

        for (Folder folder : folders) {
            boolean inbox = !folder.display_name.equalsIgnoreCase(Folders.INBOX.toString());
            boolean drafts = !folder.display_name.equalsIgnoreCase(Folders.DRAFTS.toString());
            boolean trash = !folder.display_name.equalsIgnoreCase(Folders.TRASH.toString());
            boolean starred = !folder.display_name.equalsIgnoreCase(Folders.STARRED.toString());
            boolean sent = !folder.display_name.equalsIgnoreCase(Folders.SENT.toString());
            boolean spam = !folder.display_name.equalsIgnoreCase(Folders.SPAM.toString());
            boolean followUp = !folder.display_name.equalsIgnoreCase(Folders.FOLLOW_UP.toString());
            boolean readNow = !folder.display_name.equalsIgnoreCase(Folders.READ_NOW.toString());
            boolean readLater = !folder.display_name.equalsIgnoreCase(Folders.READ_LATER.toString());

            if (inbox && drafts && trash && starred && sent && spam && followUp && readNow && readLater)
                filteredFolders.add(folder);
        }

        return filteredFolders;
    }

}
