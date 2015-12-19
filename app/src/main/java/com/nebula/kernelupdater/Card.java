package com.nebula.kernelupdater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nebula.kernelupdater.R.id;
import com.nebula.kernelupdater.R.layout;

/**
 * Created by Mike on 9/19/2014.
 */
public class Card {
    private final Context CONTEXT;
    private final String TITLE;
    private final View PARENT;

    public Card(Context c, String title, boolean placeSeparators, View... views) {
        this(c, title, null, placeSeparators, views);
    }

    public Card(Context c, String title, View addition, boolean placeSeparators, View... views) {
        this.TITLE = title;
        this.PARENT = ((LayoutInflater) (this.CONTEXT = c).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout.card_layout, null);
        ((TextView) this.PARENT.findViewById(id.card_title)).setText(this.TITLE);
        LinearLayout container = (LinearLayout) this.PARENT.findViewById(id.card_content);
        for (View view : views) {
            container.addView(view);
            if (placeSeparators && !(view == views[views.length - 1]))
                container.addView(this.PARENT.findViewById(id.card_separator));
        }

        if (addition != null) {
            ((ViewGroup) this.PARENT.findViewById(id.additional)).addView(addition);
        }
    }

    public String getTITLE() {
        return this.TITLE;
    }

    public View getPARENT() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 10);
        this.PARENT.setLayoutParams(params);
        return this.PARENT;
    }

    @Override
    public String toString() {
        return "Card{" +
                "CONTEXT=" + this.CONTEXT +
                ", TITLE='" + this.TITLE + '\'' +
                ", PARENT=" + this.PARENT +
                '}';
    }
}
