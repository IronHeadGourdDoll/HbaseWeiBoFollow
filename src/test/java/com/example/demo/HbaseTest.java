package com.example.demo;

import com.example.demo.util.HBaseUtil;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class HbaseTest {
    /**
     * 创建命名空间final
     * user，列族info,follows,fans
     */
    @Test
    public void createTable() {
        HBaseUtil.createTable("final:user", new String[]{"info", "follows","fans"});
    }

    /**
     * 按照规则插入1000条数据
     */
    @Test
    public void addRows() {
        //添加1000个用户
        int N=1000;
        for (int i = 0; i < N; i++) {
            //添加用户i
            String id= String.valueOf(i+10000000);
            HBaseUtil.putRow("final:user", id, "info", "username", "张三"+i);
            HBaseUtil.putRow("final:user", id, "info", "pwd", "123456");

            String t1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String t2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String t3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //每个用户随机关注用户,最少1个最多3个，随机数可能重复
            String id1= String.valueOf(new Random().nextInt(N)+10000000);
            String id2= String.valueOf(new Random().nextInt(N)+10000000);
            String id3= String.valueOf(new Random().nextInt(N)+10000000);
            //关注,列值id–timestamp根据字典排序，方便新写入数据查询时快速命中
            HBaseUtil.putRow("final:user", id, "follows", id1+"–"+t1, id1);
            HBaseUtil.putRow("final:user", id, "follows", id2+"–"+t2, id2);
            HBaseUtil.putRow("final:user", id, "follows", id3+"–"+t3, id3);
            //粉丝
            HBaseUtil.putRow("final:user", id1, "fans", id1+"–"+t1, id);
            HBaseUtil.putRow("final:user", id2, "fans", id2+"–"+t2, id);
            HBaseUtil.putRow("final:user", id3, "fans", id3+"–"+t3, id);
        }
    }

    /**
     * 测试完毕，删除表，方便下次直接使用
     */
    @Test
    public void deleteTable() {
        HBaseUtil.deleteTable("final:user");
    }


}
