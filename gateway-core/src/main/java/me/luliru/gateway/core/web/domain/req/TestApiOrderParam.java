package me.luliru.gateway.core.web.domain.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * TestApiOrderParam
 * Created by luliru on 2019-07-04.
 */
@Data
public class TestApiOrderParam {

    @NotBlank
    private String order_original_id;
}
