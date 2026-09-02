package com.merware.dayssincepro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FontSizeListPreference extends ListPreference {

    public FontSizeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FontSizeListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        final CharSequence[] entries = getEntries();
        final CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException("FontSizeListPreference requires matching entries and entryValues");
        }

        int selectedIndex = findIndexOfValue(getValue());
        if (selectedIndex < 0) {
            selectedIndex = findIndexOfValue("16");
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                getContext(),
                android.R.layout.select_dialog_singlechoice,
                android.R.id.text1,
                entries) {
            @Override
            public TextView getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                try {
                    int sizeSp = Integer.parseInt(entryValues[position].toString());
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
                } catch (NumberFormatException ignored) {
                    // Keep the platform default text size if an entry value cannot be parsed.
                }
                return view;
            }
        };

        builder.setSingleChoiceItems(adapter, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && callChangeListener(entryValues[which].toString())) {
                    setValue(entryValues[which].toString());
                }
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(null, null);
        builder.setNegativeButton(android.R.string.cancel, null);
    }
}
