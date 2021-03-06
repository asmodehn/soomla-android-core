/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soomla.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.soomla.SoomlaConfig;

import java.util.HashMap;

/**
 * The KeyValDatabase provides a basic key-value store above SQLite.
 */
public class KeyValDatabase {

    /**
     * Constructor.
     *
     * @param context global information about the application environment
     */
    public KeyValDatabase(Context context) {

        if (SoomlaConfig.DB_DELETE){
            context.deleteDatabase(DATABASE_NAME);
        }

        mDatabaseHelper = new DatabaseHelper(context);
        mStoreDB = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Closes the database.
     */
    public synchronized void close() {
        mDatabaseHelper.close();
    }

    /**
     * Deletes the database completely!
     *
     * @param context global information about the application environment
     */
    public void purgeDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Sets the given value to the given key.
     *
     * @param key the key of the key-val pair
     * @param val the val of the key-val pair
     */
    public synchronized void setKeyVal(String key, String val) {
        ContentValues values = new ContentValues();
        values.put(KEYVAL_COLUMN_VAL, val);

        int affected = mStoreDB.update(KEYVAL_TABLE_NAME, values, KEYVAL_COLUMN_KEY + "='"
                + key + "'", null);
        if (affected == 0){
            values.put(KEYVAL_COLUMN_KEY, key);
            mStoreDB.replace(KEYVAL_TABLE_NAME, null, values);
        }
    }

    /**
     * Retrieves the value for the given key.
     *
     * @param key the key of the key-val pair
     * @return a value for the given key
     */
    public synchronized String getKeyVal(String key) {
        Cursor cursor = mStoreDB.query(KEYVAL_TABLE_NAME, KEYVAL_COLUMNS, KEYVAL_COLUMN_KEY
                + "='" + key + "'",
                null, null, null, null);
 
        if (cursor != null && cursor.moveToNext()) {
            int valColIdx = cursor.getColumnIndexOrThrow(KEYVAL_COLUMN_VAL);
            String ret = cursor.getString(valColIdx);
            cursor.close();
            return ret;
        }
        
        if(cursor != null) {
        	cursor.close();
        }
        
        return null;
    }

    /**
     * Deletes the key-val pair.
     *
     * @param key the key of the key-val pair
     */
    public synchronized void deleteKeyVal(String key) {
        mStoreDB.delete(KEYVAL_TABLE_NAME, KEYVAL_COLUMN_KEY + "=?", new String[] { key });
    }

    public synchronized HashMap<String, String> getQueryVals(String query) {
        query = query.replace('*', '%');
        Cursor cursor = mStoreDB.query(KEYVAL_TABLE_NAME, KEYVAL_COLUMNS, KEYVAL_COLUMN_KEY
                + " LIKE '" + query + "'",
                null, null, null, null);

        HashMap<String, String> ret = new HashMap<String, String>();
        while (cursor != null && cursor.moveToNext()) {
            try {
                int valColIdx = cursor.getColumnIndexOrThrow(KEYVAL_COLUMN_VAL);
                int keyColIdx = cursor.getColumnIndexOrThrow(KEYVAL_COLUMN_KEY);
                ret.put(cursor.getString(keyColIdx), cursor.getString(valColIdx));
            } catch (IllegalArgumentException exx) {
            }
        }

        if(cursor != null) {
            cursor.close();
        }

        return ret;
    }

    /**
     * DataBase Helper class
     */
    private class DatabaseHelper extends SQLiteOpenHelper{

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            if (!sqLiteDatabase.isReadOnly()){
                sqLiteDatabase.execSQL("PRAGMA foreign_key=ON");
            }

            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + KEYVAL_TABLE_NAME + "(" +
                    KEYVAL_COLUMN_KEY + " TEXT PRIMARY KEY, " +
                    KEYVAL_COLUMN_VAL + " TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            // Nothing to do here ...
        }
    }


    /** General key-value storage */

    private static final String KEYVAL_TABLE_NAME = "kv_store";
    public static final String KEYVAL_COLUMN_KEY = "key";
    public static final String KEYVAL_COLUMN_VAL = "val";
    private static final String[] KEYVAL_COLUMNS = {
            KEYVAL_COLUMN_KEY, KEYVAL_COLUMN_VAL
    };


    /** Private Members **/

    private static final String TAG = "KeyValDatabase"; //used for Log messages

    private static final String DATABASE_NAME  = "store.kv.db";

    private SQLiteDatabase mStoreDB;

    private DatabaseHelper mDatabaseHelper;
}
