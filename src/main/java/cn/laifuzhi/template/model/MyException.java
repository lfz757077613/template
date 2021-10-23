package cn.laifuzhi.template.model;

import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import lombok.Getter;

@Getter
public class MyException extends RuntimeException{
    private final BizCodeEnum bizCodeEnum;

    public MyException(BizCodeEnum bizCodeEnum, Throwable cause) {
        super(cause);
        this.bizCodeEnum = bizCodeEnum;
    }

    public MyException(BizCodeEnum bizCodeEnum) {
        this.bizCodeEnum = bizCodeEnum;
    }

    // 不需要堆栈，避免性能开销
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
