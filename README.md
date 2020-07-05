

### 一、 项目概述

​	本项目是我的Hbase期末作品，主要实现微博系统用户的关注功能，还附带用户注册登录等。

![image-20200704182726502](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200704182726502.png)

#### 1. 技术选型

​	SpringBoot、Thymeleaf、Hbase、jQuery、Bootstrap

### 二、运行配置

​	1. 修改HBaseConn的ip与端口

​	2. run HbaseTest的addRows初始化数据

​	3. run SpringBoot

​	4. 浏览器输入：localhost:8888

### 三、 特别说明

#### 		1.Hbase数据模型（依次包含关系）

- 表（Table）

- 行（Row）

- 列族（Column Family）

- 列标识（Column Qualifier）

- 单元（Cell）

- 时间戳（Timestamp）

  单元数据的版本号就是时间戳，保留版本数量默认是3，查询不指定版本返回最新数据）

#### 	2.Hbase数据库设计

​		设计考虑属性

- 1.索引仅仅依赖于Key
- 2.表数据根据行键排序，表中的每个区域都代表了一部分行键的空间，这个区域通过开始和结束行键来指定
- 3.hbase表中的数据都是字节数组，没有类型之分。
- 4.原子性仅保证在行级。跨行操作不保证原子性，也就是说不存在多行事务。
- 5.列族必须在表创建的时候就定义。
- 6.列标识是动态的，可以在写入数据是定义。

##### 	2.1 单行多列（我使用的）

​	Row Key：用户标识ID

​	Column Family1：’info’

​	Column Family2：’follows’

​	Column Family3：’fans’

​	Column Qualifier2x cell：关注ID-timespace

​	Column Qualifier3x cell：粉丝ID-timespace

![image-20200704173530056](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200704173530056.png)

###### 查询

```java
public User searchUser(String userId) {
        Result row = HBaseUtil.getRow(tableName, userId);
        String id=Bytes.toString(row.getRow());
        String username=Bytes.toString(row.getValue(Bytes.toBytes("info"), Bytes.toBytes("username")));
        String pwd=Bytes.toString(row.getValue(Bytes.toBytes("info"), Bytes.toBytes("pwd")));

        //将列族所有数据转化为字节map，封装为关注、粉丝list
        Map<byte[], byte[]> followsMap = row.getFamilyMap(Bytes.toBytes("follows"));
        Map<byte[], byte[]> fansMap = row.getFamilyMap(Bytes.toBytes("fans"));
        List<String> follows=new ArrayList<>();
        List<String> fans=new ArrayList<>();
        for(Map.Entry<byte[], byte[]> entry:followsMap.entrySet()){
            //将列标识的值加入
            follows.add(Bytes.toString(entry.getValue()));
        }
        for(Map.Entry<byte[], byte[]> entry:fansMap.entrySet()){
            //将列标识的值加入
            fans.add(Bytes.toString(entry.getValue()));
        }

        User user=new User(id,username,pwd,follows,fans);
        return user;
    }
```

HbaseUtil

```java
public static Result getRow(String tableName, String rowKey) {
    try (Table table = HBaseConn.getTable(tableName)) {//创建table对象
        Get get = new Get(Bytes.toBytes(rowKey));
        return table.get(get);
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null;
}
```

###### 添加

```java
public void regist(User user) {
    String t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    HBaseUtil.putRow(tableName,user.getId(),"info","username",user.getUsername());
    HBaseUtil.putRow(tableName,user.getId(),"info","pwd",user.getPwd());
}
```

HbaseUtil

```java
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
```

######  删除列前缀是id的列

```java
public void unFollowUser(User user, String unFollowId) throws IOException {

    Table table = HBaseUtil.getTable(tableName);
    //列前缀过滤器，过滤包含id的列
    ColumnPrefixFilter f = new ColumnPrefixFilter(Bytes.toBytes(unFollowId));
    //只匹配行键user.userid的行，[start,end)，返回行
    Scan followsScan = new Scan(Bytes.toBytes(user.getId()),Bytes.toBytes(String.valueOf(Integer.parseInt(user.getId())+1)));
    Scan fansScan = new Scan(Bytes.toBytes(unFollowId),Bytes.toBytes(String.valueOf(Integer.parseInt(unFollowId)+1)));
    // 通过QualifierFilter的 newBinaryPrefixComparator也可以实现
    followsScan.setFilter(f);
    fansScan.setFilter(f);

    followsScan.setBatch(1);
    fansScan.setBatch(1);

    ResultScanner rs1 = table.getScanner(followsScan);
    ResultScanner rs2 = table.getScanner(fansScan);
    System.out.println(user.getId().equals(unFollowId));
    //删除关注数据
    for (Result r1 = rs1.next(); r1 != null; r1 = rs1.next()) {
        for (Cell cell : r1.listCells()) {
            // 获得列标识
            String qf= new String(CellUtil.cloneQualifier(cell));
            System.out.println("follows的列标识==============================="+ qf);
            HBaseUtil.deleteQualifier(tableName,user.getId(),"follows",qf);
        }
    }
    //删除粉丝数据
    for (Result r2 = rs2.next(); r2 != null; r2 = rs2.next()) {
        for (Cell cell : r2.listCells()) {
            // 获得列标识
            String qf= new String(CellUtil.cloneQualifier(cell));
            System.out.println("fans的列标识==============================="+ qf);
            HBaseUtil.deleteQualifier(tableName,unFollowId,"fans",qf);
        }
    }
    rs1.close();
}
```

​	设计分析：	

​	单column说明一行数据一个 keyvelue对象 或者说cell单元格

​	多column即为一行数据包含多个cell单元格

​	那么同样一行数据的put 多c要比单c多处更多的rpc请求次数。

​	以及每个cell单元格 都有自己的元数据（ key（rowkey ， 列族 ， 列名） ： value） 在key部分是重复的，数据数据量方面也会多出一些。因此数据量较大（即关注粉丝太多）效率变低。

##### 2.2 多行单列（推荐使用）

​		Row Key：用户标识ID + (Long.MAX_VALUE - timestamp)

​		Column Family：’f’

​		Column Qualifier：’userid’

​		Column cell：username

![这里写图片描述](https://img-blog.csdn.net/20171128111109402?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbWFvc2lqdW56aQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

​		行键现在包括关注着和被关注者；列族的名称已经缩短为f。短的列族名称没有任何影响，它仅仅是为了提高IO操作。这里获得一个被关注的列表变成了一个部分浏览，而不是之前的全表浏览。取消关注和用户A是否关注用户B变成了简单的删除和get操作，而不是之前的遍历整行。这在关注者和别关注者列表很大的时候将变得很有用。

​		需要注意的是行键的长度是变化的。变化的长度使得监控性能变得困难，因为来自每个请求的长度不一致。一个解决方法就是行键使用哈希值，为了得到长度一致的行键，你可以先哈希用户的id然后连接他们，而不是简单的连接在一起。在查询的时候，因为你知道你要查询的用户ID，所以你可以重新计算哈希后，在进行查询。进行哈希后的表像下面这样：

![这里写图片描述](https://img-blog.csdn.net/20171128111158281?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbWFvc2lqdW56aQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

###### 	查找

- ​	查找粉丝：行键过滤器followedID
- ​	查找关注：行键过滤器followID

### 总结

这篇文章包含了Hbase的基础架构设计，Hbase Java API的基本使用。设计Hbase数据库的关键点如下：

- 1.行键在表设计中非常重要，决定着应用中的交互以及提取数据的性能。
- 2.hbase表示非常灵活的，你可以使用字节数组存储任何数据。
- 3.存错任何数据到列族中，都可以使用相同的访问模式来访问数据。
- 4.索引仅仅是行键，好好利用，将成为你的优势。
- 5.深度高的表结构，可以使得你快速且简单的访问数据，但是却丢掉了原子性。宽度广的表结构，可以保证行级别的原子操作，但每行会有很多的列。
- 6.你需要好好的思考你的表设计，使得可以使用单条API就可以操作，而不是使用多条。hbase不支持跨行的事务，也尽量避免在客户端代码中使用这样的逻辑。
- 7.行键的哈希可以使得行键有固定的长度和更好的分布。但是却丢弃了使用字符串时的默认排序功能。
- 8.列标识可以用来存储数据，就像单元数据一样。
- 9.列标识的长度影响数据存储的足迹。也影响硬盘和网络IO的花销，所以应该尽量简洁。
- 10.列族名字的长度影响到发送到客户端的数据长度。所以尽量简洁。

本项目[github](https://blog.csdn.net/qq_41170102/article/details/105999612)地址

References

https://blog.csdn.net/maosijunzi/article/details/78653018

https://blog.csdn.net/qq_41170102/article/details/105999612
