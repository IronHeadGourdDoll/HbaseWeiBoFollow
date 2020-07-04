package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.util.HBaseUtil;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserServiceImpl {
    private String tableName="final:user";

    public List<User> getAll() {
        ResultScanner scanner = HBaseUtil.getScanner(tableName);
        List<User> users=new ArrayList<>();
        if (scanner != null) {
            scanner.forEach(result -> {
                //获得单行元素
                Result row = HBaseUtil.getRow(tableName, Bytes.toString(result.getRow()));
                assert row != null;
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
                users.add(user);
            });
        }
        scanner.close();
        return users;
    }

    public User getUserBySession() {
        String visitorName="anonymousUser";//初始默认不登陆，参观者为匿名用户
        User visitor=new User();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session= request.getSession();
        //没登录为匿名用户
        if (session.getAttribute("visitor")==null){
            visitor.setId("0");
            visitor.setUsername(visitorName);
            session.setAttribute("visitor", visitor);
            return visitor;
        }else {
            visitor= (User) session.getAttribute("visitor");
        }
        return visitor;
    }

    public void regist(User user) {
        String t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        HBaseUtil.putRow(tableName,user.getId(),"info","username",user.getUsername());
        HBaseUtil.putRow(tableName,user.getId(),"info","pwd",user.getPwd());
    }

    public void login(User user) {
        //将登录信息添加到session
        Result row=HBaseUtil.getRow(tableName,user.getId());
        assert row != null;
        String id=Bytes.toString(row.getRow());
        String username=Bytes.toString(row.getValue(Bytes.toBytes("info"), Bytes.toBytes("username")));
        String pwd=Bytes.toString(row.getValue(Bytes.toBytes("info"), Bytes.toBytes("pwd")));
        if (user.getPwd().equals(pwd)){
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
            User realUser=new User(id,username,pwd,follows,fans);
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            HttpSession session= request.getSession();
            session.setAttribute("visitor",realUser);
        }
    }

    public void logout() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session= request.getSession();
        //将访问者改为匿名用户
        User user=new User();
        user.setId("0");
        user.setUsername("anonymousUser");
        session.setAttribute("visitor",user);
    }

    public void followUser(User user, String followId) {
        String t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        HBaseUtil.putRow(tableName,user.getId(),"follows",user.getId()+"–"+t,followId);
        HBaseUtil.putRow(tableName,followId,"fans",user.getId()+"–"+t,user.getId());
    }
    /**
    * @Description: 删除关注的id，再删除被关注的粉丝id
    * @Param: [user, unFollowId]
    * @return: void
    * @Author: wuliang
    * @Date: 2020/7/4 14:41
    */
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
}
