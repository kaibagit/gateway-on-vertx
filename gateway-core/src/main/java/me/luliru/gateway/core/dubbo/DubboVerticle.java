package me.luliru.gateway.core.dubbo;

import com.alibaba.dubbo.rpc.cluster.support.UnitUtil;
import com.alibaba.fastjson.JSON;
import com.dianwoba.dispatch.weather.monitor.domain.dto.param.TestDispatchParam;
import com.dianwoba.dispatch.weather.monitor.provider.TestDispatchProvider;
import com.dianwoba.order.domain.dto.param.BaseParam;
import com.dianwoba.order.domain.dto.result.OrderDTO;
import com.dianwoba.order.query.provider.QueryOrderProvider;
import com.dianwoda.open.order.mapping.dto.request.QueryRequest;
import com.dianwoda.open.order.mapping.dto.response.OrderMappingResponse;
import com.dianwoda.open.order.mapping.provider.OrderMappingProvider;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DubboVerticle
 * Created by luliru on 2019-07-04.
 */
@Slf4j
@Component
public class DubboVerticle extends AbstractVerticle {

    private static AtomicInteger id = new AtomicInteger(0);

    private Integer qaapiPlatformId = 5;

    private static OrderMappingProvider orderMappingProvider;

    private static QueryOrderProvider queryOrderProvider;

    private static TestDispatchProvider testDispatchProvider;

//    public void DubboVerticle(){
//        log.info("DubboVerticle-{} create.",id.getAndIncrement());
//    }

    public void start() {
        vertx.eventBus().consumer("dubbo.OrderMappingProvider.fetchOrderMapping",message -> {
//            log.info("DubboVerticle并发：{}",id.incrementAndGet());
            try{
                String order_original_id = (String) message.body();
                QueryRequest request = QueryRequest
                        .create()
                        .setOuterOrderId(order_original_id)
                        .setPlatformId(qaapiPlatformId);
                OrderMappingResponse orderMappingResponse = orderMappingProvider.find(request);
                message.reply(JSON.toJSONString(orderMappingResponse));
            }catch (Exception e){
                message.fail(0, JSON.toJSONString(e));
            }finally {
//                log.info("DubboVerticle并发：{}",id.decrementAndGet());
            }
        });

        vertx.eventBus().consumer("dubbo.OrderMappingProvider.fetchOrderMapping2",message -> {
//            log.info("DubboVerticle并发：{}",id.incrementAndGet());
            try{
                String order_original_id = (String) message.body();
                QueryRequest request = QueryRequest
                        .create()
                        .setOuterOrderId(order_original_id)
                        .setPlatformId(qaapiPlatformId);
                OrderMappingResponse orderMappingResponse = orderMappingProvider.find(request);
                message.reply(orderMappingResponse,new DeliveryOptions().setCodecName("json_default"));
            }catch (Exception e){
                message.fail(0, JSON.toJSONString(e));
            }finally {
//                log.info("DubboVerticle并发：{}",id.decrementAndGet());
            }
        });

        vertx.eventBus().consumer("dubbo.QueryOrderProvider.findOrderByIdDeeply",message ->{
//            log.info("DubboVerticle并发：{}",id.incrementAndGet());
            try{
                BaseParam queryParam = (BaseParam) message.body();
                UnitUtil.setCityId(queryParam.getCityId());
                OrderDTO order = queryOrderProvider.findOrderByIdDeeply(queryParam);
                message.reply(JSON.toJSONString(order));
            }catch (Exception e){
                message.fail(0, JSON.toJSONString(e));
            }finally {
//                log.info("DubboVerticle并发：{}",id.decrementAndGet());
            }
        });

        vertx.eventBus().consumer("dubbo.TestDispatchProvider.dispatch",message ->{
//            log.info("DubboVerticle并发：{}",id.incrementAndGet());
            try{
                TestDispatchParam dispatchParam = (TestDispatchParam) message.body();
                UnitUtil.setCityId(dispatchParam.getCityId());
                boolean isDispatchSuccess = testDispatchProvider.dispatch(dispatchParam);
                message.reply(JSON.toJSONString(isDispatchSuccess));
            }catch (Exception e){
                message.fail(0, JSON.toJSONString(e));
            }finally {
//                log.info("DubboVerticle并发：{}",id.decrementAndGet());
            }
        });
    }

    @Resource
    public void setOrderMappingProvider(OrderMappingProvider orderMappingProvider) {
        this.orderMappingProvider = orderMappingProvider;
    }

    @Resource
    public void setQueryOrderProvider(QueryOrderProvider queryOrderProvider) {
        this.queryOrderProvider = queryOrderProvider;
    }

    @Resource
    public void setTestDispatchProvider(TestDispatchProvider testDispatchProvider) {
        this.testDispatchProvider = testDispatchProvider;
    }
}
