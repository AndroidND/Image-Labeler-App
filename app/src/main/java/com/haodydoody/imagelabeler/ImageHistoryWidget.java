package com.haodydoody.imagelabeler;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import static com.haodydoody.imagelabeler.DirectoryEntryAdapter.PREFERENCES_WIDGET_CONTENT;
import static com.haodydoody.imagelabeler.DirectoryEntryAdapter.PREFERENCES_WIDGET_TITLE;

/**
 * Implementation of App Widget functionality.
 */
public class ImageHistoryWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.image_history_widget);
        SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        views.setTextViewText(R.id.appwidget_title, sharedPreferences.getString(PREFERENCES_WIDGET_TITLE, ""));
        views.setTextViewText(R.id.appwidget_content, sharedPreferences.getString(PREFERENCES_WIDGET_CONTENT, ""));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

}

