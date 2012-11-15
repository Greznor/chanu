package com.chanapps.four.data;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A loader that queries the ChanDatabaseHelper and returns a {@link Cursor}.
 * This class implements the {@link Loader} protocol in a standard way for
 * querying cursors, building on {@link AsyncTaskLoader} to perform the cursor
 * query on a background thread so that it does not block the application's UI.
 * 
 */
public class ChanThreadCursorLoader extends AsyncTaskLoader<Cursor> {
    protected final ForceLoadContentObserver mObserver;

    protected SQLiteDatabase db;
    protected Cursor mCursor;

    private String boardName;
    
    protected ChanThreadCursorLoader(Context context, SQLiteDatabase db) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        this.db = db;
    }

    public ChanThreadCursorLoader(Context context, SQLiteDatabase db, String boardName) {
        this(context, db);
        this.boardName = boardName;
    }

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {
    	Log.i(TAG(), "loadInBackground");
    	String query = "SELECT " + ChanDatabaseHelper.POST_ID + ", "
				+ "'http://0.thumbs.4chan.org/' || " + ChanDatabaseHelper.POST_BOARD_NAME
					+ " || '/thumb/' || " + ChanDatabaseHelper.POST_TIM + " || 's.jpg' 'image_url', "
				+ ChanDatabaseHelper.POST_NOW + " || ' ' || " + ChanDatabaseHelper.POST_COM + " 'text', "
				+ ChanDatabaseHelper.POST_TN_W + " 'tn_w', " + ChanDatabaseHelper.POST_TN_H + " 'tn_h'"
				+ " FROM " + ChanDatabaseHelper.POST_TABLE
				+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
					+ ChanDatabaseHelper.POST_RESTO + "=0 ORDER BY " + ChanDatabaseHelper.POST_TIM + " DESC";
    	if (db != null && db.isOpen()) {
    		Log.i(TAG(), "loadInBackground database is ok");
    		Cursor cursor = db.rawQuery(query, null);
    		if (cursor != null) {
    			// Ensure the cursor window is filled
    			int count = cursor.getCount();
    			Log.i(TAG(), "loadInBackground cursor is ok, count: " + count);
    			registerContentObserver(cursor, mObserver);
    		}
    		return cursor;
    	}
        return null;
    }

    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
		Log.i(TAG(), "deliverResult isReset(): " + isReset());
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
    	Log.i(TAG(), "onStartLoading mCursor: " + mCursor);
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
    	Log.i(TAG(), "onStopLoading");
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
    	Log.i(TAG(), "onCanceled cursor: " + cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        Log.i(TAG(), "onReset cursor: " + mCursor);
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

	protected String TAG() {
		return "ChanThreadCursorLoader";
	}

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("boardName="); writer.println(boardName);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
