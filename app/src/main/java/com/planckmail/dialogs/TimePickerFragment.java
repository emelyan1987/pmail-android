package com.planckmail.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.planckmail.utils.BundleKeys;

import java.util.Calendar;

/**
 * Created by Terry on 11/13/2015.
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private IPickedTime mListener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default value for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        int year = getArguments().getInt(BundleKeys.YEAR);
        int month = getArguments().getInt(BundleKeys.MONTH);
        int day = getArguments().getInt(BundleKeys.DAY);

        if (mListener != null) {
            mListener.pickedTime(year, month, day, hourOfDay, minute);
            dismiss();
        }
    }

    public void setListener(IPickedTime listener) {
        mListener = listener;
    }

    public interface IPickedTime {
        void pickedTime(int year, int month, int day, int hourOfDay, int minute);
    }
}
