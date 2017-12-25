package com.manmeet.to_dolist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import static com.manmeet.to_dolist.data.TaskContract.TaskEntry.TABLE_NAME;

public class TaskContentProvider extends ContentProvider {
    public final static int TASKS = 100;
    public final static int TASK_WITH_ID = 101;

    private final static UriMatcher sUriMatcher = buildUriMatcher();
    private TaskDbHelper mTaskDbHelper;

    public static UriMatcher buildUriMatcher() {
        UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.PATH_TASKS, TASKS);
        mUriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.PATH_TASKS + "/#", TASK_WITH_ID);
        return mUriMatcher;
    }

    /* onCreate() is where you should initialize anything you’ll need to setup
    your underlying data source.
    In this case, you’re working with a SQLite database, so you’ll need to
    initialize a DbHelper to gain access to it.
     */
    @Override
    public boolean onCreate() {
        Context context = getContext();
        mTaskDbHelper = new TaskDbHelper(context);
        return true;
    }


    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = mTaskDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri returnUri;
        switch (match) {
            case TASKS:
                long id = db.insert(TABLE_NAME, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(TaskContract.TaskEntry.CONTENT_URI, id);
                } else {
                    throw new SQLException("Insert failed into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mTaskDbHelper.getReadableDatabase();
        int match = sUriMatcher.match(uri);
        Cursor returnCursor;
        switch (match) {
            case TASKS:
                returnCursor = db.query(TABLE_NAME, projection,
                        selection, selectionArgs, null,
                        null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }
        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return returnCursor;
    }


    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mTaskDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        int returnNumberOfTasks;
        switch (match) {
            case TASK_WITH_ID:
                String id = uri.getPathSegments().get(1);
                String mSelection = "_id=?";
                String[] mSelectionArgs = new String[]{id};
                returnNumberOfTasks = db.delete(TABLE_NAME, mSelection, mSelectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }
        if (returnNumberOfTasks != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return returnNumberOfTasks;
    }


    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int tasksUpdated;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case TASK_WITH_ID:
                String id = uri.getPathSegments().get(1);
                tasksUpdated = mTaskDbHelper.getWritableDatabase().update(TABLE_NAME, values, "_id=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (tasksUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return tasksUpdated;
    }

    /* getType() handles requests for the MIME type of data
    We are working with two types of data:
    1) a directory and 2) a single row of data.
    This method will not be used in our app, but gives a way to standardize the data formats
    that your provider accesses, and this can be useful for data organization.
    For now, this method will not be used but will be provided for completeness.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        int match = sUriMatcher.match(uri);

        switch (match) {
            case TASKS:
                // directory
                return "vnd.android.cursor.dir" + "/" + TaskContract.AUTHORITY + "/" + TaskContract.PATH_TASKS;
            case TASK_WITH_ID:
                // single item type
                return "vnd.android.cursor.item" + "/" + TaskContract.AUTHORITY + "/" + TaskContract.PATH_TASKS;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }
}
