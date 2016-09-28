package com.planckmail.web.response.boxDrive;

import java.util.List;

/**
 * Created by Terry on 1/27/2016.
 */
public class BoxDriveFolder {
    public String id;
    public String sequence_id;
    public String etag;
    public String name;
    public String created_at;
    public String description;
    public int size;
    public BoxDriveUser created_by;
    public BoxDriveModifyBy modifyed_by;
    public BoxDriveParent parent;
    public BoxDriveSharedLink shared_link;
    public int total_count;
    public List<BoxDriveFile> entries;

}
