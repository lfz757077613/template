package cn.laifuzhi.template.model;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException{
    public MyException(String message) {
        super(message);
    }

    public MyException(String message, Throwable cause) {
        super(String.format("%s <- %s", message, cause.toString()), cause);
    }
}
