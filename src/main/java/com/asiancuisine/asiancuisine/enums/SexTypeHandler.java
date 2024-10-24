package com.asiancuisine.asiancuisine.enums;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SexTypeHandler extends BaseTypeHandler<Sex> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Sex sex, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, sex.getCode()); // Convert enum to int when inserting
    }

    @Override
    public Sex getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName); // Convert int to enum when querying
        return SexFromCode(code);
    }

    @Override
    public Sex getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex); // Convert int to enum when querying
        return SexFromCode(code);
    }

    @Override
    public Sex getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return SexFromCode(code);
    }

    private Sex SexFromCode(int code) {
        for (Sex sex : Sex.values()) {
            if (sex.getCode() == code) {
                return sex;
            }
        }
        return null; // Handle invalid values
    }
}
