package com.planckmail.web.response.boxDrive;

/**
 * Created by Terry on 1/28/2016.
 */
public class BoxDriveFile
{
    public String type;
    public long id;
    public int sequence_id;
    public int etag;
    public String sha1;
    public String name;
    public String description;
    public long size;
    public BoxDriveUser created_by;
    public String modified_at;
    public BoxDriveParent parent;
    public BoxDriveSharedLink shared_link;
}
