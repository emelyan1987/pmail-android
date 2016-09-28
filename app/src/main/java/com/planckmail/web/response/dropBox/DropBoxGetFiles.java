package com.planckmail.web.response.dropBox;

import java.util.List;

/**
 * Created by Terry on 1/3/2016.
 */
public class DropBoxGetFiles {
    public String hash;
    public boolean thumb_exists;
    public String path;
    public boolean is_dir;
    public String folder;
    public String dropbox;
    public List<DropBoxFile> contents;
    public String size;
}
