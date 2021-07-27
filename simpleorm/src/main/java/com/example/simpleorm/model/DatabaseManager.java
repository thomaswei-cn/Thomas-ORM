package com.example.simpleorm.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.simpleorm.annotation.Column;
import com.example.simpleorm.annotation.NotNull;
import com.example.simpleorm.annotation.Primary;
import com.example.simpleorm.annotation.Table;
import com.example.simpleorm.exception.ORMException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager<T> {
    private T target;
    private SQLiteDatabase mDatabase;
    private List<MyColumn> myColumnList;
    private String TableName;


    public DatabaseManager(T target, SQLiteDatabase database) {
        this.target = target;
        mDatabase = database;
        myColumnList = new ArrayList<>();
        init();
    }

    private void init() {
        if (target != null && mDatabase != null) {
            Class targetClass = target.getClass();

            if (targetClass.isAnnotationPresent(Table.class)) {
                Table table = (Table) targetClass.getAnnotation(Table.class);
                if (table.name().equals("")) {
                    TableName = targetClass.getName();
                } else {
                    TableName = table.name();
                }
            } else {
                try {
                    throw new ORMException("must have a Table Annotation");
                } catch (ORMException e) {
                    e.printStackTrace();
                }
            }
            //------------------------设置表名------------------------
            Field[] fields = targetClass.getDeclaredFields();
            if (fields.length != 0) {
                for (Field field : fields) {
                    MyColumn singleColumn = new MyColumn();
                    singleColumn.setColumnName(getColumnName(field));
                    singleColumn.setPrimary(getPrimary(field));
                    singleColumn.setAutoIncrement(getAutoIncrement(field));
                    singleColumn.setTYPE(getTYPE(field));
                    singleColumn.setNotnull(getNotNull(field));
                    myColumnList.add(singleColumn);
                }
            } else {
                try {
                    throw new ORMException("no fields");
                } catch (ORMException e) {
                    e.printStackTrace();
                }
            }
            //---------------------------设置字段-----------------------

        } else {
            try {
                throw new ORMException("target or database does not exist");
            } catch (ORMException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean getNotNull(Field field){
        if (field.isAnnotationPresent(NotNull.class)){
            NotNull notNull = field.getAnnotation(NotNull.class);
            if (notNull.Notnull()){
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    private String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column column = (Column) field.getAnnotation(Column.class);
            if (column.ColumnName().equals("")) {
                return field.getName();
            } else {
                return column.ColumnName();
            }

        } else {
            return field.getName();
        }
    }

    private boolean getPrimary(Field field) {
        if (field.isAnnotationPresent(Primary.class)) {
            Primary primary = field.getAnnotation(Primary.class);
            if (primary.isPrimary()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean getAutoIncrement(Field field) {
        if (field.isAnnotationPresent(Primary.class)) {
            Primary primary = field.getAnnotation(Primary.class);
            if (primary.isAutoIncrement()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private String getTYPE(Field field) {
        Class fieldType = field.getType();
        String res = null;
        if (fieldType == String.class) {
            res = "text";
        } else if (fieldType == short.class || fieldType == int.class || fieldType == long.class ||
                fieldType == Short.class || fieldType == Integer.class || fieldType == Long.class || fieldType == boolean.class || fieldType == Boolean.class) {
            res = "integer";
        } else if (fieldType == float.class || fieldType == double.class || fieldType == Float.class || fieldType == Double.class) {
            res = "real";
        } else {
            try {
                throw new ORMException("no TYPE matched");
            } catch (ORMException e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    public void createTable() {
        StringBuilder builder = new StringBuilder();
        builder.append("create table if not exists ").append(TableName + " ").append("(");
        for (MyColumn myColumn : myColumnList) {
            builder.append(myColumn.getColumnName() + " ").append(myColumn.getTYPE() + " ");
            if (myColumn.isNotnull()){
                builder.append("not null ");
            }
            if (myColumn.isPrimary()) {
                builder.append("primary key ");
                if (myColumn.isAutoIncrement()) {
                    builder.append("autoincrement ");
                }
            }
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(");");

        Log.i("mysql", builder.toString());
        mDatabase.execSQL(builder.toString());
    }

    public SQLiteDatabase getDatabase() {
        if (mDatabase == null) {
            try {
                throw new ORMException("database is null");
            } catch (ORMException e) {
                e.printStackTrace();
            }
        }
        return mDatabase;
    }

    public boolean insert(T t) {
        Class tempClass = t.getClass();
        int i = 0;
        ContentValues contentValues = new ContentValues();
        Field[] fields = tempClass.getDeclaredFields();
        try {
            for (MyColumn myColumn : myColumnList) {
                if (myColumn.isAutoIncrement()) {
                    i++;
                    continue;
                }
                fields[i].setAccessible(true);
                Object obj = fields[i].get(t);

                if (myColumn.isNotnull()&&obj == null){
                    try {
                        throw new ORMException(myColumn.getColumnName()+"is not null");
                    }catch (ORMException e){
                        e.printStackTrace();
                    }
                }

                if (obj instanceof String) {
                    contentValues.put(myColumn.getColumnName(), (String) obj);
                } else if (obj instanceof Boolean) {
                    if ((Boolean) obj) {
                        contentValues.put(myColumn.getColumnName(), (short) 1);
                    } else {
                        contentValues.put(myColumn.getColumnName(), (short) 0);
                    }
                } else if (obj instanceof Short) {
                    contentValues.put(myColumn.getColumnName(), (Short) obj);
                } else if (obj instanceof Integer) {
                    contentValues.put(myColumn.getColumnName(), (Integer) obj);
                } else if (obj instanceof Long) {
                    contentValues.put(myColumn.getColumnName(), (Long) obj);
                } else if (obj instanceof Float) {
                    contentValues.put(myColumn.getColumnName(), (Float) obj);
                } else if (obj instanceof Double) {
                    contentValues.put(myColumn.getColumnName(), (Double) obj);
                } else {
                    try {
                        throw new ORMException("invalid type");
                    } catch (ORMException e) {
                        e.printStackTrace();
                    }

                }
                i++;

            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        long res = mDatabase.insert(TableName,null,contentValues);
        if (res == 1){
            return false;
        }else {
            return true;
        }
    }
}
