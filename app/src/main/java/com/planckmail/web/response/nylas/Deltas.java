package com.planckmail.web.response.nylas;

import com.planckmail.web.response.nylas.wrapper.Attributes;

import java.util.List;

/**
 * Created by Taras Matolinets on 09.07.15.
 */
public class Deltas {
    public String cursor_end;
    public String cursor_start;
    public List<Attributes> deltas;
}
