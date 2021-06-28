package OrmArchivarius.DBManager.utils;

import OrmArchivarius.Annotations.Column;
import OrmArchivarius.Annotations.Entity;

import java.lang.reflect.Field;

public class DBUtil {
    public static String getTableNameFromAnnotation(Class<?> clazz) {
        if (clazz.getAnnotation(Entity.class) == null)
            throw new RuntimeException("Class of object is not marked as Entity");
        String tableName = clazz.getAnnotation(Entity.class).value();
        if (!tableName.isEmpty()) {
            return tableName;
        } else {
            return clazz.getSimpleName();
        }
    }

    public static String javaTypeToSql(Class<?> clazz) {
        if (int.class.equals(clazz) || Integer.class.equals(clazz) || long.class.equals(clazz) || Long.class.equals(clazz)) {
            return "INTEGER";
        } else if (String.class.equals(clazz)) {
            return "VARCHAR";
        } else if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return "BIT";
        }
        return "";
    }

    public static String getFieldName(Field field) {
        String fieldName = field.getAnnotation(Column.class).value();
        if (!fieldName.isEmpty())
            return fieldName;
        return field.getName();
    }
}
