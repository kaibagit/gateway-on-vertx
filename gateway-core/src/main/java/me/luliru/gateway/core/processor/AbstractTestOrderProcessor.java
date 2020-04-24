package me.luliru.gateway.core.processor;

import com.alibaba.fastjson.JSON;
import com.dianwoba.order.domain.dto.param.BaseParam;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.en.OrderStatusEn;
import com.dianwoda.open.order.mapping.dto.response.OrderMappingResponse;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.web.domain.req.TestApiOrderParam;
import me.luliru.gateway.core.web.enums.CodeEnum;
import me.luliru.gateway.core.web.enums.SubCodeEnum;
import me.luliru.gateway.core.web.exception.ApiBizException;
import me.luliru.gateway.core.web.exception.OpenGatewayException;
import org.apache.commons.lang3.StringUtils;

/**
 * AbstractTestOrderProcessor
 * Created by luliru on 2019-07-04.
 */
@Slf4j
public abstract class AbstractTestOrderProcessor<T> implements TestApiProcessor<T> {

    protected Vertx vertx;

    public AbstractTestOrderProcessor(Vertx vertx){
        this.vertx = vertx;
    }

    @Override
    public Single<T> process(TestApiContext context, String biz_params) {
        if(StringUtils.isBlank(biz_params)){
            return Single.error(new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空"));
        }
        TestApiOrderParam param = null;
        try{
            param = JSON.parseObject(biz_params, TestApiOrderParam.class);
        }catch (Exception e){
            log.error("解析业务参数失败",e);
            return Single.error(new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"解析业务参数失败"));
        }
        String order_original_id = param.getOrder_original_id();
        if(StringUtils.isBlank(order_original_id)){
            return Single.error(new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空"));
        }

//        Single<OrderMappingResponse> orderMappingResponseSingle = Single.create(subscriber ->{
//            vertx.eventBus().send("dubbo.OrderMappingProvider.fetchOrderMapping",order_original_id,ar -> {
//                if(ar.succeeded()){
//                    String data = (String) ar.result().body();
//                    OrderMappingResponse orderMappingResponse = JSON.parseObject(data,OrderMappingResponse.class);
//                    if("ORDER_MAPPING_NOT_EXIST".equals(orderMappingResponse.getRespName())){
//                        subscriber.onError(new ApiBizException(SubCodeEnum.ORDER_NOT_EXISTED));
//                        return;
//                    }
//                    if(!orderMappingResponse.isSuccess()){
//                        subscriber.onError(new OpenGatewayException(CodeEnum.API_UNKNOWN_ERROR));
//                        return;
//                    }
//                    subscriber.onSuccess(JSON.parseObject(data,OrderMappingResponse.class));
//                }else{
//                    ReplyException ex = (ReplyException) ar.cause();
//                    if(ex.failureCode() < 0){
//                        subscriber.onError(ex);
//                    }else{
//                        subscriber.onError(JSON.parseObject(ex.getMessage(),Exception.class));
//                    }
//                }
//            });
//        });

        Single<OrderMappingResponse> orderMappingResponseSingle = Single.create(subscriber ->{
            vertx.eventBus().send("dubbo.OrderMappingProvider.fetchOrderMapping2",order_original_id,new DeliveryOptions().setCodecName("json_default"), ar -> {
                if(ar.succeeded()){
                    OrderMappingResponse orderMappingResponse = (OrderMappingResponse) ar.result().body();
                    if("ORDER_MAPPING_NOT_EXIST".equals(orderMappingResponse.getRespName())){
                        subscriber.onError(new ApiBizException(SubCodeEnum.ORDER_NOT_EXISTED));
                        return;
                    }
                    if(!orderMappingResponse.isSuccess()){
                        subscriber.onError(new OpenGatewayException(CodeEnum.API_UNKNOWN_ERROR));
                        return;
                    }
                    subscriber.onSuccess(orderMappingResponse);
                }else{
                    ReplyException ex = (ReplyException) ar.cause();
                    if(ex.failureCode() < 0){
                        subscriber.onError(ex);
                    }else{
                        subscriber.onError(JSON.parseObject(ex.getMessage(),Exception.class));
                    }
                }
            });
        });

        return orderMappingResponseSingle.flatMap(orderMappingResponse -> {
            BaseParam queryParam = new BaseParam();
            queryParam.setCityId(orderMappingResponse.getOrderMapping().getCityId());
            queryParam.setOrderId(orderMappingResponse.getOrderMapping().getDwdOrderId());
            return Single.create(subscriber ->{
                vertx.eventBus().send("dubbo.QueryOrderProvider.findOrderByIdDeeply",queryParam,ar -> {
                    if(ar.succeeded()){
                        String data = (String) ar.result().body();
                        OrderDTO order = JSON.parseObject(data,OrderDTO.class);
                        processOrder(orderMappingResponse.getOrderMapping().getCityId(),order).subscribe(s ->{
                            subscriber.onSuccess(s);
                        });
                    }else{
                        ReplyException ex = (ReplyException) ar.cause();
                        if(ex.failureCode() < 0){
                            subscriber.onError(ex);
                        }else{
                            subscriber.onError(JSON.parseObject(ex.getMessage(),Exception.class));
                        }
                    }
                });
            });
        });
    }

    public abstract Single<T> processOrder(int cityId,OrderDTO order);

    protected String getOrderStatusDesc(int orderStatus){
        if(orderStatus == OrderStatusEn.PLACED.getCode()){
            return "已下单";
        }else if(orderStatus == OrderStatusEn.DISPATCHED.getCode()){
            return "已接单";
        }else if(orderStatus == OrderStatusEn.ARRIVED.getCode()){
            return "已到店";
        }else if(orderStatus == OrderStatusEn.OBTAINED.getCode()){
            return "已离店";
        }else if(orderStatus == OrderStatusEn.FINISHED.getCode()){
            return "已完成";
        }else if(orderStatus == OrderStatusEn.ABNORMAL.getCode()){
            return "异常";
        }else if(orderStatus == OrderStatusEn.CANCEL.getCode()){
            return "已取消";
        }else{
            return "未知";
        }
    }
}
