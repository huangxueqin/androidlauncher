package com.hxq.test;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class LauncherProvider extends ContentProvider{

    public static final String AUTHORITY = "com.hxq.test.launcherprovider";
    public static final String PARAMETER_NOTIFY = "notify";

    private DatabaseHelper mHelper;

    private static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int DIR = 1;
    private static final int ITEM = 2;
    private static final String PATH = DatabaseHelper.TABLE_NAME_FAVORITES;
    static {
        matcher.addURI(AUTHORITY, PATH, DIR);
        matcher.addURI(AUTHORITY, PATH+"/#", ITEM);
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		int flag = matcher.match(uri);
		switch(flag) {
		case DIR:
		case ITEM:
			SQLiteDatabase db = mHelper.getWritableDatabase();
			count = db.delete(PATH, selection, selectionArgs);
			if(count > 0) {
				sendNotify(uri);
			}
			break;
			
		default:
			break;
		}
		return count;
	}

	@Override
	public String getType(Uri uri) {
        switch(matcher.match(uri)) {
            case DIR:
                return "vnd.android.cursor.dir/" + PATH;
            case ITEM:
                return "vnd.android.cursor.item/" + PATH;
            default:
                return null;
        }
	}

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if ("true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    private long dbInsertAndCheck(DatabaseHelper helper,
                                         SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (!values.containsKey(LauncherSettings._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        return db.insert(table, nullColumnHack, values);
    }

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int flag = matcher.match(uri);
		switch(flag) {
		case DIR:
			SQLiteDatabase db = mHelper.getWritableDatabase();
			final long rowId = dbInsertAndCheck(mHelper, db, PATH, null, values);
			if(rowId <= 0) {
				return null;
			}
			uri = ContentUris.withAppendedId(uri, rowId);
			sendNotify(uri);
			break;
		default:
			break;
		}
        return uri;
	}

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
    	
    	int flag = matcher.match(uri);
    	switch(flag) {
    	case DIR:
    		SQLiteDatabase db = mHelper.getWritableDatabase();
    		db.beginTransaction();
            try {
                int numValues = values.length;
                for (int i = 0; i < numValues; i++) {
                    if (dbInsertAndCheck(mHelper, db, PATH, null, values[i]) < 0) {
                        return 0;
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            sendNotify(uri);
            break;
        default:
            break;
    	}
    	return values.length;
    }

    @Override
	public boolean onCreate() {
		mHelper = new DatabaseHelper(getContext());
		((LauncherApplication)getContext()).setLauncherProvider(this);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		int flag = matcher.match(uri);
    	switch(flag) {
    	case DIR:
    	case ITEM:
    		SQLiteDatabase db = mHelper.getWritableDatabase();
    		count = db.update(PATH, values, selection, selectionArgs);
            if(count > 0)
            	sendNotify(uri);
    		break;
        default:
            break;
    	}
    	return count;
	}
	
	public long generatorNewId() {
		return mHelper.generateNewId();
	}
	
	static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);                
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

}
