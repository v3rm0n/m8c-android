package io.maido.m8client.log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

import io.maido.m8client.R;

public class CachedFileProvider extends ContentProvider {

    private static final String TAG = CachedFileProvider.class.getSimpleName();
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        final Context ctx = getContext();
        final String authority = ctx.getString(R.string.uri_authority_cache);
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(authority, "*", 1);
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        switch (uriMatcher.match(uri)) {
            case 1:
                String fileName = uri.getLastPathSegment();
                File file = new File(getContext().getCacheDir(), fileName);
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);
                return pfd;
            default:
                throw new FileNotFoundException("Unsupported uri: "
                        + uri.toString());
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
                      String[] as) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String s, String[] as1,
                        String s1) {
        return null;
    }
}