package entity;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Administrator on 2015/11/24 0024.
 */
public interface IBeanResultSetCreate<T> {
    T init(ResultSet rs) throws SQLException;
}
