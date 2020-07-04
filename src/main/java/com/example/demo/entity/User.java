package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private String id;
    private String username;
    private String pwd;
    //用户id
    private List<String> follows;
    private List<String> fans;


}
