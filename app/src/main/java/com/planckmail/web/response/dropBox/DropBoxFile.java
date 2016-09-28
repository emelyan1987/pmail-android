package com.planckmail.web.response.dropBox;

/**
 * Created by Terry on 12/29/2015.
 */
public class DropBoxFile implements Comparable<DropBoxFile> {
    public String rev;
    public boolean thumb_exist;
    public String path;
    public boolean is_dir;
    public String client_mtime;
    public String icon;
    public boolean read_only;
    public int bytes;
    public String modified;
    public String size;
    public String root;
    public String mime_type;
    public int revision;

    public String getSize() {
        return size;
    }

    public DropBoxFile setSize(String size) {
        this.size = size;
        return this;
    }

    public String getRev() {
        return rev;
    }

    public DropBoxFile setRev(String rev) {
        this.rev = rev;
        return this;
    }

    public int getBytes() {
        return bytes;
    }

    public DropBoxFile setBytes(int bytes) {
        this.bytes = bytes;
        return this;
    }

    public String getModified() {
        return modified;
    }

    public DropBoxFile setModified(String modified) {
        this.modified = modified;
        return this;
    }

    public String getPath() {
        return path;
    }

    public DropBoxFile setPath(String path) {
        this.path = path;
        return this;
    }

    public boolean is_dir() {
        return is_dir;
    }

    public DropBoxFile setIs_dir(boolean is_dir) {
        this.is_dir = is_dir;
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public DropBoxFile setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getRoot() {
        return root;
    }

    public DropBoxFile setRoot(String root) {
        this.root = root;
        return this;
    }

    public int getRevision() {
        return revision;
    }

    public DropBoxFile setRevision(int revision) {
        this.revision = revision;
        return this;
    }

    public boolean isRead_only() {
        return read_only;
    }

    public DropBoxFile setRead_only(boolean read_only) {
        this.read_only = read_only;
        return this;
    }

    @Override
    public int compareTo(DropBoxFile another) {
        int b1 = another.is_dir ? 1 : 0;
        int b2 = is_dir ? 1 : 0;
        return b1 - b2;
    }
}
