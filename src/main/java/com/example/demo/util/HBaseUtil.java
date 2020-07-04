package com.example.demo.util;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HBaseUtil {
    /**
     * 创建Hbase表
     *
     * @param tableName 表名
     * @param cfs       列族数组
     * @return 是否创建成功
     */
    public static boolean createTable(String tableName, String[] cfs) {
        try (HBaseAdmin admin = (HBaseAdmin) HBaseConn.getHBaseConn().getAdmin()) {
            if (admin.tableExists(tableName)) {//表已经存在
                return false;
            }
            HTableDescriptor tableDescriptor =
                    new HTableDescriptor(TableName.valueOf(tableName));
            Arrays.stream(cfs).forEach(cf -> {//遍历列族数组
                HColumnDescriptor columnDescriptor = new HColumnDescriptor(cf);
                columnDescriptor.setMaxVersions(1);//设置版本数量
                tableDescriptor.addFamily(columnDescriptor);
            });
            admin.createTable(tableDescriptor);//创建表
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    public static Table getTable(String tableName) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            return table;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
    * @Description: 删除表
    * @Param: [tableName]
    * @return: boolean
    * @Author: wuliang
    * @Date: 2020/6/24 15:37
    */
    public static boolean deleteTable(String tableName){
        try (HBaseAdmin admin = (HBaseAdmin) HBaseConn.getHBaseConn().getAdmin()) {
            admin.disableTable(tableName);//disable表
            admin.deleteTable(tableName);//删除表
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * hbase中插入一条数据
     *
     * @param tableName 表名
     * @param rowKey    唯一标识
     * @param cfName    列族名
     * @param qualifier 列名
     * @param data      数据
     * @return 是否插入成功
     */
    public static boolean putRow(String tableName, String rowKey,
                                 String cfName, String qualifier, String data) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Put put = new Put(Bytes.toBytes(rowKey));//创建put对象
            put.addColumn(Bytes.toBytes(cfName),
                    Bytes.toBytes(qualifier),
                    Bytes.toBytes(data));//封装put对象
            table.put(put);//put数据
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 插入多条数据
     *
     * @param tableName 表名
     * @param puts      封装好的put集合
     * @return 是否成功
     */
    public static boolean putRows(String tableName, List<Put> puts) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            table.put(puts);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 获取单条数据
     *
     * @param tableName 表名
     * @param rowKey    唯一标识
     * @return 查询结果
     */
    public static Result getRow(String tableName, String rowKey) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Get get = new Get(Bytes.toBytes(rowKey));
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取单条数据
     *
     * @param tableName  表名
     * @param rowKey     唯一标识
     * @param filterList 过滤器
     * @return 查询结果
     */
    public static Result getRow(String tableName, String rowKey, FilterList filterList) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Get get = new Get(Bytes.toBytes(rowKey));
            get.setFilter(filterList);
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 全表扫描
     *
     * @param tableName 表名
     * @return ResultScanner
     */
    public static ResultScanner getScanner(String tableName) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Scan scan = new Scan();
            scan.setCaching(1000);//缓存条数
            return table.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 带过滤器的扫描
     * @param tableName 表名
     * @param filterList 过滤器
     * @return ResultScanner
     */
    public static ResultScanner getScanner(String tableName,FilterList filterList) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Scan scan = new Scan();
            scan.setCaching(1000);//缓存条数
            scan.setFilter(filterList);
            return table.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 按区间扫描
     * @param tableName 表名
     * @param startKey 起始rowkey
     * @param endKey 终止rowKey
     * @return ResultScanner
     */
    public static ResultScanner getScanner(String tableName, String startKey, String endKey) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Scan scan = new Scan();
            scan.setCaching(1000);//缓存条数
            scan.setStartRow(Bytes.toBytes(startKey));
            scan.setStopRow(Bytes.toBytes(endKey));
            return table.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  带过滤器的按区间扫描
     * @param tableName 表名
     * @param startKey 起始rowkey
     * @param endKey 终止rowKey
     * @param filterList 过滤器
     * @return ResultScanner
     */
    public static ResultScanner getScanner(String tableName, String startKey, String endKey,
                                           FilterList filterList) {
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Scan scan = new Scan();
            scan.setCaching(1000);//缓存条数
            scan.setStartRow(Bytes.toBytes(startKey));
            scan.setStopRow(Bytes.toBytes(endKey));
            scan.setFilter(filterList);
            return table.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * hbase删除一行记录
     * @param tableName 表名
     * @param rowKey 唯一标识
     * @return 是否成功
     */
    public static boolean deleteRow(String tableName,String rowKey){
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 删除列族
     * @param tableName 表名
     * @param cName 列族名
     * @return 是否成功
     */
    public static boolean deleteColumnFamily(String tableName,String cName){
        try (HBaseAdmin admin = (HBaseAdmin) HBaseConn.getHBaseConn().getAdmin()) {
            admin.deleteColumn(tableName,cName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 删除列
     * @param tableName 表名
     * @param rowKey 唯一标识
     * @param cfName 列族名
     * @param qualifier 列名
     * @return
     */
    public static boolean deleteQualifier(String tableName,String rowKey,
                                          String cfName,String qualifier){
        try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addColumn(Bytes.toBytes(cfName),Bytes.toBytes(qualifier));
            table.delete(delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
