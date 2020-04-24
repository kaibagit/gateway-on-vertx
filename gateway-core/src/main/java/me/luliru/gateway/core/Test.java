package me.luliru.gateway.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;

/**
 * Test
 * Created by luliru on 2019-07-05.
 */
@Slf4j
public class Test {

    public static void main(String[] args){
        Vertx vertx = io.vertx.reactivex.core.Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions().setWorkerPoolSize(Runtime.getRuntime().availableProcessors()+1);
        vertx.rxDeployVerticle(MyVerticle.class.getName(),options)
                .toObservable()
                .subscribe(
                        id->{
                            log.info("启动成功，id:{}",id);
                        },
                        e->{
                            throw new RuntimeException("启动失败",e);
                        });
    }

    public static class MyVerticle extends AbstractVerticle{
        public void start() throws Exception {
            createHttpServer();
        }

        private void createHttpServer(){
            HttpServer server = vertx.createHttpServer();
            server.requestHandler(request -> {
                System.out.println(request.query());
                request.response().end("hello");
            });
            server.listen(15295);
        }
    }
}
