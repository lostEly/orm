package OrmArchivarius.DBManager;

import OrmArchivarius.Annotations.*;
import OrmArchivarius.DBManager.utils.ClassUtils;
import OrmArchivarius.DBManager.utils.DBUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class TableManager {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS \"%s\" (";
    private static final String ALTER_FOREIGN_KEY = "FOREIGN KEY (%s) REFERENCES \"%s\"(id)";
    private static final String INSERT_INTO_TABLE = "INSERT INTO \"%s\" (";
    private static final String UPDATE_TABLE = "UPDATE \"%s\" SET ";
    private static final String GET_BY_ID = "SELECT * FROM \"%s\" WHERE id = %d";
    private static final String GET_ALL = "SELECT * FROM \"%s\"";
    private static final String DELETE_RECORD = "DELETE FROM \"%s\" WHERE id = %d;";
    private static final String GET_BY_FOREIGN_KEY = "SELECT * FROM \"%s\" WHERE %s = %d";
    private Connection connection;

    public TableManager(String dbName) {
        try {
            connection = ConnectionFactory.getConnection(dbName);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public <T> List<T> getAllBy(Class<T> targetClazz, Object obj) {
        String tableName = DBUtil.getTableNameFromAnnotation(targetClazz);
        String fieldName = "";
        Long id = ClassUtils.getObjectId(obj);
        Field[] fields1 = targetClazz.getDeclaredFields();
        for (Field field : fields1) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class)) {
                fieldName = field.getName();
            }
        }
        String sql = String.format(GET_BY_FOREIGN_KEY, tableName, fieldName, id);
        return fillListByResultSetValues(targetClazz, sql);
    }

    public <T> List<T> getAll(Class<T> clazz) {
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        String sql = String.format(GET_ALL, tableName);
        return fillListByResultSetValues(clazz, sql);
    }

    private <T> List<T> fillListByResultSetValues(Class<T> clazz, String sql) {
        ArrayList<T> objects = null;
        try (Statement statement = connection.createStatement()) {
            Field[] fields = clazz.getDeclaredFields();
            ResultSet rs = statement.executeQuery(sql);
            objects = new ArrayList<>(rs.getFetchSize());
            while (rs.next()) {
                Optional<T> optionalObject = createInstanceOfType(clazz);
                T o = optionalObject.orElseThrow(() -> new RuntimeException("failed to create object of clazz" + clazz));
                setObjectByInfoFromDB(rs, fields, o);
                objects.add(o);
            }
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
        return objects;
    }

    public <T> Optional<T> getById(Class<?> clazz, Long id) {
        Optional<T> optionalObject = createInstanceOfType(clazz);
        T o = optionalObject.orElseThrow(() -> new RuntimeException("failed to create object of clazz " + clazz));
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        String sql = String.format(GET_BY_ID, tableName, id);

        try (Statement statement = connection.createStatement()) {
            Field[] fields = clazz.getDeclaredFields();
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                setObjectByInfoFromDB(rs, fields, o);
            } else {
                return Optional.empty();
            }
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
        return Optional.of(o);
    }

    private <T> Optional<T> createInstanceOfType(Class<?> clazz) {
        T o;
        try {
            o = (T) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
        return Optional.of(o);
    }

    private void setObjectByInfoFromDB(ResultSet rs, Field[] fields, Object o) throws SQLException, IllegalAccessException {
        for (Field field : fields) {
            if (field != null && field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                String fieldName = DBUtil.getFieldName(field);
                Object fieldValue;
                Class<?> fieldType = field.getType();
                if (fieldType == Long.class || fieldType == long.class)
                    fieldValue = rs.getLong(fieldName);
                else if (fieldType == Integer.class || fieldType == int.class)
                    fieldValue = rs.getInt(fieldName);
                else
                    fieldValue = rs.getObject(fieldName, field.getType());
                field.set(o, fieldValue);
            } else if (field != null && field.isAnnotationPresent(ManyToOne.class)) {
                field.setAccessible(true);
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                Long id = rs.getLong(manyToOne.name());
                Object relatedObject = getById(manyToOne.targetClazz(), id).orElse(null);
                field.set(o, relatedObject);
            }
        }
    }

    public boolean deleteRecord(Object o) {
        Class<?> clazz = o.getClass();
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        Long id = ClassUtils.getObjectId(o);
        String sql = String.format(DELETE_RECORD, tableName, id);
        return executeUpdateByStatement(sql);
    }

    public void updateRecord(Object o) {
        Map<String, Object> fieldsMap = ClassUtils.getMapOfFieldsToValues(o);
        Class<?> clazz = o.getClass();
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        String sql = String.format(UPDATE_TABLE, tableName);
        sql += createSqlUpdateQuery(o);
        sql += " WHERE id = %d;";
        Long objectId = ClassUtils.getObjectId(o);
        if (ClassUtils.getObjectId(o) == null)
            objectId = 0L;
        sql = String.format(sql, objectId);
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            setInsertStatementParameters(fieldsMap, preparedStatement);
            preparedStatement.executeUpdate();
            connection.commit();
            preparedStatement.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String createSqlUpdateQuery(Object o) {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Object> fieldsMap = ClassUtils.getMapOfFieldsToValues(o);
        for (Map.Entry<String, Object> entry : fieldsMap.entrySet()) {
            if (entry.getKey().equals("id"))
                continue;
            stringBuilder.append(entry.getKey())
                    .append(" = ")
                    .append("?")
                    .append(", ");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 2);
    }

    public void insertIntoTable(Object o) {
        Class<?> clazz = o.getClass();
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        String sql = String.format(INSERT_INTO_TABLE, tableName);
        Map<String, Object> fieldsMap = ClassUtils.getMapOfFieldsToValues(o);
        sql += createSqlInsertQuery(fieldsMap);
        String tempString = sql;
        prepareStatementForInsert(o, tempString, fieldsMap);
    }

    private void prepareStatementForInsert(Object o, String sql, Map<String, Object> fieldsMap) {
        PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setInsertStatementParameters(fieldsMap, preparedStatement);
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            long id = 0;
            if (rs != null) {
                rs.next();
                id = rs.getLong(1);
            }
            ClassUtils.setIdForEntity(o, id);
            connection.commit();
            preparedStatement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String createSqlInsertQuery(Map<String, Object> fieldsMap) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : fieldsMap.entrySet()) {
            if (entry.getKey().equals("id"))
                continue;
            stringBuilder.append(entry.getKey()).append(",");
        }
        stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
        stringBuilder.append(") VALUES (");
        stringBuilder.append("?, ".repeat(Math.max(0, fieldsMap.size() - 1)));
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        stringBuilder.append(");");
        return stringBuilder.toString();
    }

    private void setInsertStatementParameters(Map<String, Object> fieldsMap, PreparedStatement preparedStatement) {
        int i = 1;
        for (Map.Entry<String, Object> entry : fieldsMap.entrySet()) {
            if (entry.getKey().equals("id"))
                continue;
            Object value = entry.getValue();
            try {
                if (value != null) {
                    Class<?> valueClazz = value.getClass();

                    if (valueClazz == Integer.class) {
                        preparedStatement.setInt(i++, (Integer) value);
                    } else if (valueClazz == Long.class) {
                        preparedStatement.setLong(i++, (Long) value);
                    } else if (valueClazz == String.class) {
                        preparedStatement.setString(i++, (String) value);
                    } else if (valueClazz == Boolean.class) {
                        preparedStatement.setBoolean(i++, (Boolean) value);
                    }
                } else
                    preparedStatement.setObject(i++, null);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void createTableIfNotExists(Class<?> clazz, Field... fields) {
        String tableName = DBUtil.getTableNameFromAnnotation(clazz);
        StringBuilder sql = new StringBuilder(String.format(CREATE_TABLE, tableName));
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ManyToOne.class)) {
                Class<?> targetClazz = field.getAnnotation(ManyToOne.class).targetClazz();
                createTableIfNotExists(targetClazz, targetClazz.getDeclaredFields());
                break;
            }
        }
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(OneToMany.class) && !field.isAnnotationPresent(ManyToOne.class))
                sql.append(convertFieldToSql(field));
        }
        addForeignKey(clazz, sql);
        sql = new StringBuilder(sql.substring(0, sql.length() - 1));
        sql.append(");");
        executeUpdateByStatement(sql.toString());
    }

    private boolean executeUpdateByStatement(String query) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
            connection.commit();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
        return true;
    }

    private String convertFieldToSql(Field field) {
        String sql = "";
        if (!field.getAnnotation(Column.class).value().isEmpty()) {
            sql += field.getAnnotation(Column.class).value() + " ";
        } else {
            sql += field.getName() + " ";
        }
        if (field.isAnnotationPresent(Id.class)) {
            sql += "SERIAL PRIMARY KEY";
        } else {
            sql += DBUtil.javaTypeToSql(field.getType());
        }
        sql += ",";
        return sql;
    }

    private void addForeignKey(Class<?> clazz, StringBuilder sql) {
        Field[] field = clazz.getDeclaredFields();
        for (Field field1 : field) {
            field1.setAccessible(true);
            if (field1.isAnnotationPresent(ManyToOne.class)) {
                String keyName = field1.getAnnotation(ManyToOne.class).name();
                Class<?> referencedClazz = field1.getAnnotation(ManyToOne.class).targetClazz();
                sql.append(keyName).append(" INTEGER").append(",");
                String referencedTableName = DBUtil.getTableNameFromAnnotation(referencedClazz);
                sql.append(String.format(ALTER_FOREIGN_KEY, keyName, referencedTableName));
                sql.append(",");
                return;
            }
        }
    }
}
