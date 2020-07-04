package com.example.demo.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;

/**
 * 〈获取连接的单例的类〉
 * 因为连接是贯穿整个操作的，所以采用饿汉式直接就创建了单例
 * 不常用的可以用静态内部类，枚举类或者双重锁创建懒汉式单例
 *
 * @author wuliang
 * @create 2020/5/5
 * @since 1.0.0
 */
public class HBaseConn {
    private static final HBaseConn INSTANCE = new HBaseConn();
    private static Configuration configuration;
    private static Connection connection;

	//构造方法中初始化连接配置
    private HBaseConn() {
        try {
            if (configuration == null) {
                configuration = HBaseConfiguration.create();
                configuration.set("hbase.zookeeper.quorum", "39.99.195.107");
                configuration.set("hbase.zookeeper.property.clientPort", "2181");
                configuration.set("zookeeper.znode.parent","/hbase-unsecure");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	//通过工厂方法创建连接
    private Connection getConnection() {
        if (connection == null || connection.isClosed()) {
            try {
                connection = ConnectionFactory.createConnection(configuration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
	//获取连接
    public static Connection getHBaseConn() {
        return INSTANCE.getConnection();
    }
	//获取表对象
    public static Table getTable(String tableName) throws IOException {
        return INSTANCE.getConnection().getTable(TableName.valueOf(tableName));
    }
    //关闭连接
    public static void closeConn(){
        if (connection!=null){
            try {
                connection.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
