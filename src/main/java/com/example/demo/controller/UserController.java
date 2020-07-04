package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.io.IOException;
import java.util.List;

@Controller
public class UserController {
    @Autowired
    private UserServiceImpl userService;

    @GetMapping(value = {"/hello"})
    public String hello(){
        return "hello";
    }

    @GetMapping(value = {"/to_login"})
    public String to_login(){
        return "pages/login";
    }

    @GetMapping(value = {"/to_register"})
    public String to_register(){
        return "pages/register";
    }

    @PostMapping(value = {"/login"})
    public String login(@RequestBody User user){
        userService.login(user);
        return "redirect:/index";
    }

    @GetMapping(value = {"/logout"})
    public String logout(){
        userService.logout();
        return "redirect:/index";
    }

    @PostMapping(value = {"/register"})
    public String register(@RequestBody User user){
        userService.regist(user);
        return "pages/login";
    }

    @GetMapping(value = {"/","index"})
    public String index(Model model){
        List<User> users=userService.getAll();
        userService.getUserBySession();
        model.addAttribute("users",users);
        return "index";
    }

    @GetMapping(value = {"/followUser"})
    public String followUser( String followId){
        User user=userService.getUserBySession();
        userService.followUser(user,followId);
        return "redirect:/index";
    }
    @GetMapping(value = {"/unFollowUser"})
    public String unFollowUser(String unFollowId) throws IOException {
        User user=userService.getUserBySession();
        userService.unFollowUser(user,unFollowId);
        return "redirect:/index";
    }

    @GetMapping(value = {"/searchUser"})
    public String searchUser(Model model,String userId){
        User user = userService.searchUser(userId);
        model.addAttribute("user",user);
        return "pages/user_detail";
    }
}
