package me.luliru.gateway.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.web.GatewayVerticle;

/**
 * GatewayBootstrap
 * Created by luliru on 2019-07-03.
 */
@Slf4j
public class GatewayBootstrap {

    public static void main(String[] args){
        Vertx vertx = io.vertx.reactivex.core.Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions().setWorkerPoolSize(Runtime.getRuntime().availableProcessors()+1);
        vertx.rxDeployVerticle(GatewayVerticle.class.getName(),options)
                .toObservable()
                .subscribe(
                        id->{
                            log.info("启动成功，id:{}",id);
                        },
                        e->{
                            throw new RuntimeException("启动失败",e);
                        });
    }
}
