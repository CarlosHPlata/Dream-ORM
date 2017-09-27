package com.legion.silver.code.dreamorm.mappers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import com.legion.silver.code.dreamorm.entities.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Legion on 8/23/2017.
 */

public class DBManager<T extends Entity> {

    private SQLiteOpenHelper helper;
    private SQLiteDatabase db;
    private boolean writableDb;

    public DBManager(SQLiteOpenHelper helper) {
        this.helper = helper;
        this.db = helper.getWritableDatabase();
        this.writableDb = true;
    }



    public long insert ( T entity ){
        getWritableDB();
        long NO_INSERTED_ENTITY = -1;
        long id = NO_INSERTED_ENTITY;

        if (entity.getPrimaryID() == NO_INSERTED_ENTITY) {
            id = db.insert(entity.getTableName(), null, entity.getContentValuesNoId());
        } else {
            id = db.replace(entity.getTableName(), null, entity.getContentValues());
        }

        return id;
    }

    public long insertOrUpdate (T entity){
        getWritableDB();
        long id = db.replace(entity.getTableName(), null, entity.getContentValues());

        return id;
    }

    public List<T> multipleInsert (List<T> entities) {
        long tempId;

        getWritableDB();

        for (T entity : entities) {
            tempId = this.insert(entity);
            entity.setPrimaryIDValue(tempId);
        }

        return entities;
    }

    public void update (T entity) {
        getWritableDB();

        db.update(entity.getTableName(), entity.getContentValues(), entity.getIdColumn()+" = ?", new String[]{ String.valueOf(entity.getPrimaryID()) });
    }

    public T getById(T emptyContainer){
        getReadebleDB();

        Cursor cursor = db.query(emptyContainer.getTableName(), emptyContainer.getColumnNames(), emptyContainer.getIdColumn() +" = ?", new String[]{ String.valueOf(emptyContainer.getPrimaryID()) }, null, null, null, null);

        if (cursor !=  null)
            cursor.moveToFirst();

        if (cursor.getColumnCount() > 0)
            emptyContainer.setValues(cursor);

        return emptyContainer;
    }

    public List<T> getWhere(String whereQuery, String[] values, T dummyEntity){
        Class clazz = dummyEntity.getClass();
        ArrayList<T> entities = new ArrayList<T>();

        Cursor cursor = db.query(dummyEntity.getTableName(), dummyEntity.getColumnNames(), whereQuery, values, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    dummyEntity = (T) clazz.newInstance();
                    dummyEntity.setValues(cursor);
                    entities.add(dummyEntity);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }

        return entities;
    }


    public void delete(T entitie){
        getWritableDB();

        db.delete(entitie.getTableName(), entitie.getIdColumn()+" = ?", new String[]{ String.valueOf(entitie.getPrimaryID()) });
    }


    public List<T> getAll (T dummyEntity) {
        Class clazz = dummyEntity.getClass();
        ArrayList<T> entities = new ArrayList<T>();
        String selectQuery = "SELECT * FROM " +  dummyEntity.getTableName();

        getWritableDB();

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    dummyEntity = (T) clazz.newInstance();
                    dummyEntity.setValues(cursor);
                    entities.add(dummyEntity);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            } while ( cursor.moveToNext() );
        }

        return entities;
    }




    private SQLiteDatabase getWritableDB(){
        if ( !this.writableDb )
            this.db = helper.getWritableDatabase();

        return this.db;
    }

    private SQLiteDatabase getReadebleDB(){
        if ( !this.writableDb )
            this.db = helper.getReadableDatabase();

        return this.db;
    }

}
