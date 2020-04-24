package me.luliru.gateway.core.web.domain.resp;

import lombok.Data;

/**
 * GatewayResponse
 * Created by luliru on 2019-07-03.
 */
@Data
public class GatewayResponse {

    private String code = "success";

    private String message = "成功";

    private String sub_code;

    private String sub_message;

    private Object data;
}
