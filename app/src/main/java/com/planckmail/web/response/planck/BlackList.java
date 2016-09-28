package com.planckmail.web.response.planck;

import com.planckmail.fragments.ArchiveSpammerFragment;

import java.util.List;

/**
 * Created by Terry on 3/13/2016.
 */
public class BlackList {
    public List<BlackSpammer> blacklist;
    public boolean success;

    public static class BlackSpammer{
        public String name;
        public String email;
    }
}
