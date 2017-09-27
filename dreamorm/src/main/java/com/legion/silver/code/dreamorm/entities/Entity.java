package com.legion.silver.code.dreamorm.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;


import com.legion.silver.code.dreamorm.annotations.Column;
import com.legion.silver.code.dreamorm.annotations.IdColumn;
import com.legion.silver.code.dreamorm.annotations.TableName;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Legion on 8/16/2017.
 */

public abstract class Entity {

    private String tableName;
    private HashMap<String, Field> fieldsNames;
    private HashMap<String, FieldType> fieldTypes;
    private List<String> columns;
    private String idColumn;

    private enum FieldType  {
        STRING, LONG, FLOAT, BOOLEAN, DATE, BYTE, ID
    }

    protected Entity(){
        this.fieldsNames = new HashMap<String, Field>();
        this.fieldTypes = new HashMap<String, FieldType>();
        this.columns = new ArrayList<String>();
        fillTableInformation();
    }


    public String getIdColumn() {
        return idColumn;
    }

    public ContentValues getContentValues(){
        ContentValues contentValues = new ContentValues();

        for (String column : this.columns ) {
            FieldType columnType =  this.fieldTypes.get(column);
            Field field = this.fieldsNames.get(column);

            if (columnType == FieldType.ID){
                long longValue = (long) invokeGetMethod(field);
                if (longValue > 0)
                    contentValues.put(column, longValue);
            } else detectTypes(columnType, field, column, contentValues);
        }

        return  contentValues;
    }

    public ContentValues getContentValuesNoId(){
        ContentValues contentValues = new ContentValues();

        for (String column : this.columns){
            FieldType columnType = this.fieldTypes.get(column);
            Field field = this.fieldsNames.get(column);

            detectTypes(columnType, field, column, contentValues);
        }

        return contentValues;
    }

    public void setValues(Cursor cursor){

        for (String column : this.columns){
            FieldType fieldType = this.fieldTypes.get(column);
            Field field = this.fieldsNames.get(column);

            switch (fieldType) {
                case BOOLEAN:
                    long longBolean = cursor.getLong( cursor.getColumnIndex(column) );

                    boolean boolValue = false;
                    if (longBolean == 1) boolValue = true;

                    invokeSetMethod(field, boolValue, fieldType);
                    break;

                case BYTE:
                    byte[] bytesValue = cursor.getBlob( cursor.getColumnIndex(column) );
                    invokeSetMethod(field, bytesValue, fieldType);
                    break;

                case DATE:
                    String stringDate = cursor.getString( cursor.getColumnIndex(column) );
                    SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS.SSS");

                    Date dateValue = null;

                    try {
                        dateValue = formatter.parse(stringDate);
                    } catch (ParseException e) {
                        Log.e("NO VALID DATE FORMAT", e.getMessage());
                    }

                    invokeSetMethod(field, dateValue, fieldType);

                    break;

                case FLOAT:
                    float floatValue = cursor.getFloat( cursor.getColumnIndex(column) );
                    invokeSetMethod(field, floatValue, fieldType);
                    break;

                case ID:
                case LONG:
                    long longValue = cursor.getLong( cursor.getColumnIndex(column) );
                    invokeSetMethod(field, longValue, fieldType);
                    break;

                case STRING:
                    String stringValue = cursor.getString( cursor.getColumnIndex(column) );
                    invokeSetMethod(field, stringValue, fieldType);
                    break;
            }
        }

    }

    public long getPrimaryID(){
        String idFieldName = this.idColumn;

        if (idFieldName != null) {

            Field field = this.fieldsNames.get(idFieldName);
            long result = (long) invokeGetMethod(field);

            if (result > 0)
                return result;
            else
                 return -1;

        } else
            return -1;
    }

    public void setPrimaryIDValue(long id){
        if (this.idColumn != null) {
            Field field = this.fieldsNames.get(this.idColumn);

            invokeSetMethod(field, id, FieldType.ID);

        } else {
            //throw exception
        }
    }

    public String getTableName(){
        return this.tableName;
    }

    public String[] getColumnNames(){
        String[] columnNamesArr = new String[this.columns.size()];
        columnNamesArr = this.columns.toArray(columnNamesArr);

        return  columnNamesArr;
    }

    public boolean isColumnInClass(String columnName) {
        boolean isColumn = false;

        for (String column : this.columns){
            if (column == columnName){
                isColumn = true;
                break;
            }
        }

        return isColumn;
    }

    private void fillTableInformation(){
        Class<?> obj = this.getClass();

        fillTableName(obj);
        findIdColumn(obj);
    }

    private void fillTableName (Class<?> obj) {
        if  (obj.isAnnotationPresent(TableName.class) ) {
            TableName annotation = (TableName) obj.getAnnotation(TableName.class);
            this.tableName = annotation.value();
        } else {
            //throw exception
        }
    }

    private void findIdColumn(Class<?> obj){
        boolean foundId = false;
        String columnName;
        String fieldName;

        for ( Field field :  obj.getDeclaredFields() ) {
            if ( field.isAnnotationPresent(IdColumn.class) ) {
                foundId = true;

                IdColumn idAnnotation = (IdColumn) field.getAnnotation(IdColumn.class);
                columnName = idAnnotation.value();
                validateId(field, columnName);
                this.idColumn = columnName;

            } else if (field.isAnnotationPresent(Column.class)) {

                Column columnAnnotation = (Column) field.getAnnotation(Column.class);
                columnName = columnAnnotation.value();
                validateField(field, columnName);
            }
        }

        if (!foundId) {
            //Throw exception
        }


    }

    private void validateId(Field field, String columnName){
        if (field.getType() ==  long.class){
            this.columns.add(columnName);
            this.fieldsNames.put(columnName, field);
            this.fieldTypes.put(columnName, FieldType.ID);
        } else {
            //Throw exception
        }
    }

    private void validateField(Field field, String columnName){
        Class<?> type = field.getType();

        if (field.getType().isArray()){
            Class<?> arrayType = field.getType();
            if (arrayType.getComponentType() == byte.class){
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.BYTE);
            } else {
                //Throw exception
            }
        } else {
            if (type == String.class) {
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.STRING);
            } else if (type == Date.class){
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.DATE);
            }else if (type == long.class) {
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.LONG);
            }else if (type == boolean.class) {
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.BOOLEAN);
            }else if (type == float.class) {
                this.columns.add(columnName);
                this.fieldsNames.put(columnName, field);
                this.fieldTypes.put(columnName, FieldType.FLOAT);
            }else{
                //Throw exception
            }
        }

        Log.d("DATABASE 2.0 TYPES", " " + columnName + " -> " + type + "/" + field.getGenericType());
    }

    private void detectTypes(FieldType columnType, Field field, String column, ContentValues contentValues){
        switch (columnType) {
            case BOOLEAN:
                boolean boolValue = (boolean) invokeGetMethod(field);
                long boltolong;

                if (boolValue) boltolong = 1;
                else boltolong = 0;

                contentValues.put(column, boltolong);
                break;
            case BYTE:
                byte[] byteVale = (byte[]) invokeGetMethod(field);
                if (byteVale != null)
                    contentValues.put(column, byteVale);
                break;
            case DATE:
                Date datevalue = (Date) invokeGetMethod(field);

                if(datevalue != null) {
                    DateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS.SSS");
                    String datetoString = df.format(datevalue);
                    contentValues.put(column, datetoString);
                }

                break;
            case FLOAT:
                float floatValue = (float) invokeGetMethod(field);
                contentValues.put(column, floatValue);
                break;
            //case ID:
            case LONG:
                long longValue = (long) invokeGetMethod(field);
                contentValues.put(column, longValue);
                break;
            case STRING:
                String stringValue = (String) invokeGetMethod(field);
                if (stringValue != null)
                    contentValues.put(column, stringValue);
                break;
        }
    }

    private void invokeSetMethod(Field field, Object value, FieldType type){
        Class<?> Obj = this.getClass();

        for (Method method : Obj.getMethods()) {
            if ( (method.getName().startsWith("set")) && (method.getName().length() == (field.getName().length()+3)) ){
                if (method.getName().toLowerCase().endsWith(field.getName().toLowerCase())){
                    try {
                        method.invoke(this, new Object[]{value});
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Object invokeGetMethod(Field field) {
        Class<?> Obj = this.getClass();
        for (Method method : Obj.getMethods()){
            String methodName = method.getName();
            if ( ( (method.getName().startsWith("get")) && (method.getName().length() == (field.getName().length()+3)) )
                    || ( (method.getName().startsWith("is")) && (method.getName().length() == (field.getName().length()+2)) )){
                if (method.getName().toLowerCase().endsWith(field.getName().toLowerCase())){
                    try {
                        return method.invoke(this, new Object[]{});
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }
}
