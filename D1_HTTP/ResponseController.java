package com.cds.javaweb;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ResponseController {


    //设置响应数据
    //方式一：通过HttpServletResponse设置响应数据
    @RequestMapping("/response")
    public void response(HttpServletResponse response) throws IOException {
        //响应状态码
        response.setStatus(200);

        //设置响应头
        response.setHeader("name", "cds");

        //设置响应体
        response.getWriter().write("<h1>hello world<h1>");

    }

    //方式二：通过ResponseEntity设置响应数据
    @RequestMapping("/response2")
    public ResponseEntity<String> response2(){
        return ResponseEntity
                .status(200)
                .header("name", "cds")
                .body("<h1>hello world<h1>");
    }
}
