package com.alexcmak.dayssincepro;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

// http://stackoverflow.com/questions/5335306/how-can-i-get-an-event-in-android-spinner-when-the-current-selected-item-is-sele

// based on NDSpinner

public class SelectAgainSpinner extends Spinner {

    OnItemSelectedListener listener;

    public SelectAgainSpinner(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position)
    {
        super.setSelection(position);

        if (position == getSelectedItemPosition())
        {
            listener.onItemSelected(null, null, position, 0);
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener)
    {
        this.listener = listener;
    }


}