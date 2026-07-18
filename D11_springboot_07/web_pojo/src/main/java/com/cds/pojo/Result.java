package com.cds.pojo;

import lombok.Data;

@Data
public class Result {
    private Integer code;//1为成功,0为失败
    private String msg;
    private Object data;

    public static Result success() {
        Result result = new Result();
        result.code = 1;
        result.msg = "操作成功";
        return result;
    }

    public static Result success(Object object) {
        Result result = new Result();
        result.code = 1;
        result.msg = "操作成功";
        result.data = object;
        return result;
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
