package me.luliru.gateway.core.processor;

import com.alibaba.fastjson.JSON;
import com.dianwoba.dispatch.weather.monitor.domain.dto.param.TestDispatchParam;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.en.OrderStatusEn;
import io.reactivex.Single;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.reactivex.core.Vertx;
import me.luliru.gateway.core.web.enums.SubCodeEnum;
import me.luliru.gateway.core.web.exception.ApiBizException;

/**
 * TestOrderAcceptProcessor
 * Created by luliru on 2019-07-04.
 */
public class TestOrderAcceptProcessor extends AbstractTestOrderProcessor {

    public TestOrderAcceptProcessor(Vertx vertx) {
        super(vertx);
    }

    @Override
    public Single processOrder(int cityId, OrderDTO order) {
        // 查出当前订单状态，并判断能否状态转化
        if (!(order.getStatus() == OrderStatusEn.PLACED.getCode())) {
            return Single.error(new ApiBizException(SubCodeEnum.ORDER_NOT_SUPPORT_MODIFICATION,"不能从当前订单状态'"+getOrderStatusDesc(order.getStatus())+"'转换为'已接单'"));
        }

        TestDispatchParam dispatchParam = new TestDispatchParam();
        dispatchParam.setCityId(order.getCityId());
        dispatchParam.setOrderId(order.getId());
        dispatchParam.setRiderId(1);
        dispatchParam.setRiderLat(order.getFromLat());
        dispatchParam.setRiderLng(order.getFromLng());

        return Single.create(subscriber ->{
            vertx.eventBus().send("dubbo.TestDispatchProvider.dispatch",dispatchParam,ar ->{
                if(ar.succeeded()){
                    boolean isDispatchSuccess = Boolean.valueOf((String) ar.result().body());
                    if (!isDispatchSuccess) {
                        subscriber.onError(new ApiBizException(SubCodeEnum.ORDER_NOT_SUPPORT_MODIFICATION,"调度系统模拟派单失败！"));
                    }else{
                        ReplyException ex = (ReplyException) ar.cause();
                        if(ex.failureCode() < 0){
                            subscriber.onError(ex);
                        }else{
                            subscriber.onError(JSON.parseObject(ex.getMessage(),Exception.class));
                        }
                    }
                }else{
                    subscriber.onError(ar.cause());
                }
            });
        });
    }
}
