package me.luliru.gateway.core.web.exception;

import me.luliru.gateway.core.web.enums.SubCodeEnum;

/**
 * ApiBizException
 * Created by luliru on 2019-07-03.
 */
public class ApiBizException extends RuntimeException {

    private String code;

    public ApiBizException(SubCodeEnum subCodeEnum){
        this(subCodeEnum.getCode(),subCodeEnum.getMessage());
    }

    public ApiBizException(SubCodeEnum subCodeEnum,String message){
        this(subCodeEnum.getCode(),message);
    }

    public ApiBizException(String code,String message){
        super(message);
        this.code = code;
    }

    public Throwable fillInStackTrace() {
        return this;
    }

    public String getCode() {
        return code;
    }
}
