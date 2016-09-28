package com.planckmail.web.response.googleDrive;

import java.util.List;

/**
 * Created by Terry on 1/24/2016.
 */
public class GoogleDriveListFiles {
    public String kind;
    public String etag;
    public String selfLink;
    public String nextPageToken;
    public String nextLink;
    public List<GoogleDriveFile> files;
}
