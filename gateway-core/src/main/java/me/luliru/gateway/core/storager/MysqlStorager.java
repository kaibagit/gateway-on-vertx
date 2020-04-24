package me.luliru.gateway.core.storager;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import me.luliru.gateway.core.storager.entity.ApplicationQa;
import me.luliru.gateway.core.web.enums.CodeEnum;
import me.luliru.gateway.core.web.exception.OpenGatewayException;

import java.util.List;

/**
 * MysqlStorager
 * Created by luliru on 2019-07-03.
 */
public class MysqlStorager {

    private JDBCClient jdbcClient;

    public MysqlStorager(io.vertx.reactivex.core.Vertx vertx){
        JsonObject config = new JsonObject()
                .put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
                .put("jdbcUrl", "jdbc:mysql://rm-bp188fb70hknd9gi2o.mysql.rds.aliyuncs.com:3306/openqa_coordinator")
                .put("driverClassName", "com.mysql.jdbc.Driver")
                .put("maximumPoolSize", 10)
                .put("username","devuser")
                .put("password","Devuser123");

        jdbcClient = JDBCClient.createShared(vertx, config);
    }

    public Single<ApplicationQa> findByAppKey(String appKey){
        return jdbcClient.rxGetConnection().flatMap(conn ->{
            JsonArray params = new JsonArray();
            params.add(appKey);
            return conn.rxQueryWithParams("select * from application_qa where app_key = ?",params)
                    .doFinally(conn::close);
        }).flatMap(rs ->{
            List<JsonObject> rsList = rs.getRows();
            if(rsList.isEmpty()){
                return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"无效的appkey"));
            }
            JsonObject record = rsList.get(0);
            ApplicationQa applicationQa = new ApplicationQa();
            applicationQa.setId(record.getLong("id"));
            applicationQa.setDevepId(record.getLong("devep_id"));
            applicationQa.setAppName(record.getString("app_name"));
            applicationQa.setAppKey(record.getString("app_key"));
            applicationQa.setAppSecret(record.getString("app_secret"));
            applicationQa.setCallbackUrl(record.getString("callback_url"));
            applicationQa.setCallbackFormat(record.getInteger("callback_format").byteValue());
//            applicationQa.setInsTm(new Date(record.getLong("ins_tm")));
//            applicationQa.setUpdTm(new Date(record.getLong("upd_tm")));
            return Single.just(applicationQa);
        });
    }
}
