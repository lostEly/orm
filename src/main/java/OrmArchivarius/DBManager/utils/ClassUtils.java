package OrmArchivarius.DBManager.utils;

import OrmArchivarius.Annotations.Column;
import OrmArchivarius.Annotations.Id;
import OrmArchivarius.Annotations.ManyToOne;
import OrmArchivarius.Annotations.OneToMany;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClassUtils {

    public static Long getObjectId(Object o) {
        Class<?> clazz = o.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Id.class) != null) {
                field.setAccessible(true);
                try {
                    if (field.get(o) != null)
                        return (long) field.get(o);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void setIdForEntity(Object o, long id) {
        Class<?> clazz = o.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Id.class) != null) {
                field.setAccessible(true);
                try {
                    field.set(o, id);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    public static Map<String, Object> getMapOfFieldsToValues(Object o) {
        LinkedHashMap<String, Object> fieldsMap = new LinkedHashMap<>();
        Class<?> clazz = o.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(OneToMany.class)) {
                    String fieldName = DBUtil.getFieldName(field);
                    Object objectFromField = field.get(o);
                    fieldsMap.put(fieldName, objectFromField);

                } else if (field.isAnnotationPresent(ManyToOne.class)) {
                    String fieldName = field.getAnnotation(ManyToOne.class).name();
                    Object objectFromField = field.get(o);
                    Field f = objectFromField.getClass().getDeclaredField("id");
                    f.setAccessible(true);
                    Long id = (Long) f.get(objectFromField);
                    fieldsMap.put(fieldName, id);
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return fieldsMap;
    }
}
