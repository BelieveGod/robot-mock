package cn.nannar.robotmock.fx.dao;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.sql.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE;

/**
 * @author LTJ
 * @date 2023/8/7
 */
public class DaoTest {
    private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
    public static void main(String[] args) {

        logger.setLevel(Level.FINE);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
        logger.fine("test");
        Handler[] handlers = logger.getHandlers();
        try {
            Class<?> aClass = Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = "jdbc:sqlserver://127.0.0.1:1433;DatabaseName=monitor_test;trustServerCertificate=true;encrypt=true";
//            String url = "jdbc:sqlserver://192.168.10.25:1433;DatabaseName=monitor_test;trustServerCertificate=true;encrypt=true";
            String username = "sa";
            String password = "command";
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
//            ResultSet resultSet = statement.executeQuery("SELECT TOP 100 * FROM train_log Order by id desc ;");
            ResultSet resultSet = statement.executeQuery("SELECT * from train_info ;");
            int type = resultSet.getType();
            boolean scrollInsensitive = TYPE_SCROLL_INSENSITIVE == type;
            System.out.println("type = " + type);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for(int i=1;i<=columnCount;i++){
                String columnName = metaData.getColumnName(i);
                System.out.print(columnName);
                if(i==columnCount){
                    System.out.print("\n");
                }else{
                    System.out.print("\t");
                }
            }
            while (resultSet.next()) {
                for(int i=1;i<=columnCount;i++){
                    Object object = resultSet.getObject(i);
                    System.out.print(object);
                    if(i==columnCount){
                        System.out.print("\n");
                    }else{
                        System.out.print("\t");
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            String sqlState = throwables.getSQLState();
            System.out.println("sqlState = " + sqlState);
            int errorCode = throwables.getErrorCode();
            System.out.println("errorCode = " + errorCode);
            String message = throwables.getMessage();
            System.out.println("message = " + message);
            SQLServerException sqlServerException = (SQLServerException) throwables;
            throwables.printStackTrace();
        }

    }
}
