package me.luliru.gateway.core.spring.config;

import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.dubbo.DubboVerticle;
import me.luliru.gateway.core.util.EventBusMessageCodec;
import me.luliru.gateway.core.web.GatewayVerticle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * BootstrapConfig
 * Created by luliru on 2019-07-04.
 */
@Slf4j
@Configuration
public class BootstrapConfig {

    @Resource
    private DubboVerticle dubboVerticle;

    @Bean
    public Vertx vertx(){
        Vertx vertx = io.vertx.reactivex.core.Vertx.vertx();
        vertx.eventBus().registerCodec(new EventBusMessageCodec());
        // vertx通过workerPoolName来共享线程池
        vertx.rxDeployVerticle(DubboVerticle.class.getName(),new DeploymentOptions().setWorker(true).setInstances(20).setWorkerPoolName("vertx-dubbo"))
                .toObservable()
                .subscribe(id->{
                    log.info("DubboVerticle 启动成功，id:{}",id);
                }, e->{
                    throw new RuntimeException("启动失败",e);
                });
        vertx.rxDeployVerticle(
                GatewayVerticle.class.getName(),
                new DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors()*3/2)
//                new DeploymentOptions().setInstances(1)
        ).toObservable()
                .subscribe(id->{
                        log.info("GatewayVerticle 启动成功，id:{}",id);
                    }, e->{
                        throw new RuntimeException("启动失败",e);
                    });
        return vertx;
    }
}
