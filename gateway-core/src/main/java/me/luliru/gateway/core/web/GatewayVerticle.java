package me.luliru.gateway.core.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.Single;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.processor.TestApiContext;
import me.luliru.gateway.core.processor.TestOrderAcceptProcessor;
import me.luliru.gateway.core.storager.MysqlStorager;
import me.luliru.gateway.core.web.domain.resp.GatewayResponse;
import me.luliru.gateway.core.web.enums.CodeEnum;
import me.luliru.gateway.core.web.exception.ApiBizException;
import me.luliru.gateway.core.web.exception.OpenGatewayException;
import me.luliru.gateway.core.web.util.Signer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GatewayVerticle
 * Created by luliru on 2019-07-03.
 */
@Slf4j
public class GatewayVerticle extends AbstractVerticle {

    private static AtomicInteger id = new AtomicInteger(1);

    private WebClient httpClient;

    private MysqlStorager mysqlStorager;

    private String qaAccessToken = "TEST2018-a444-4e50-b785-f48ba984bd9c";

    private String remoteAppkey = "1000024";

    private String remoteSecret = "1b29ef6f5826878c7f3243d0a0495a99";

    private String remoteAccessToken = "11d28381-41e7-4983-8392-f7a40cb87245";

    private String remoteGateway = "http://open-api-gw-gw-dev-gw.dwbops.com/gateway";

    private TestOrderAcceptProcessor testOrderAcceptProcessor;

    public GatewayVerticle(){
        log.info("GatewayVerticle-{} created",id.getAndIncrement());
    }

    public void start() throws Exception {
        testOrderAcceptProcessor = new TestOrderAcceptProcessor(vertx);
        httpClient = WebClient.create(vertx);
        initDatasource();
        createHttpServer();
    }

    private void initDatasource(){
        mysqlStorager = new MysqlStorager(vertx);
    }

    private void createHttpServer(){
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(request -> {
            log.info("GatewayVerticle 处理中:{}",this);
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            log.info("GatewayVerticle 处理后:{}",this);
            String queryString = request.query();
            request.bodyHandler(buffer ->{
                String biz_params = buffer.toString();
                invokeQaOpenApi(queryString,biz_params)
                        .subscribe(json -> {
                            request.response().end(json);
                        });
            });
        });
        server.listen(15295);
    }

    public Single<String> invokeQaOpenApi(String queryString,String biz_params){
        return invokeQaOpenApi0(queryString,biz_params)
                .onErrorReturn(ex -> {
                    GatewayResponse gatewayResponse = new GatewayResponse();
                    if(ex instanceof OpenGatewayException){
                        OpenGatewayException e = (OpenGatewayException) ex;
                        gatewayResponse.setCode(e.getCode());
                        gatewayResponse.setMessage(e.getMessage());
                    }else if(ex instanceof ApiBizException){
                        ApiBizException e = (ApiBizException) ex;
                        gatewayResponse.setCode(CodeEnum.API_BUSINESS_ERROR.getCode());
                        gatewayResponse.setMessage(CodeEnum.API_BUSINESS_ERROR.getMessage());
                        gatewayResponse.setSub_code(e.getCode());
                        gatewayResponse.setSub_message(e.getMessage());
                    }else{
                        log.error("系统未知异常",ex);
                        gatewayResponse.setCode(CodeEnum.SYS_UNKNOWN_ERROR.getCode());
                        gatewayResponse.setMessage(CodeEnum.SYS_UNKNOWN_ERROR.getMessage());
                    }
                    return JSON.toJSONString(gatewayResponse);
                });
    }

    public Single<String> invokeQaOpenApi0(String queryString,String biz_params){
        // 平台级参数，null值表示未传参，""表示传空值
        String appkey = null;
        String timestampStr = null;
        Long timestamp = null;
        String nonce = null;
        String sign = null;
        String access_token = null;
        String api = null;
        Map<String,String> queryParameterMap = new HashMap<>();
        if(StringUtils.isNotBlank(queryString)){
            String[] queryParameters = queryString.split("&");
            for(String param : queryParameters){
                String[] paramNameAndValue = param.split("=");
                if(paramNameAndValue.length==1){
                    queryParameterMap.put(paramNameAndValue[0],"");
                }else{
                    queryParameterMap.put(paramNameAndValue[0],paramNameAndValue[1]);
                }
            }
            appkey = queryParameterMap.get("appkey");
            timestampStr = queryParameterMap.get("timestamp");
            nonce = queryParameterMap.get("nonce");
            sign = queryParameterMap.get("sign");
            access_token = queryParameterMap.get("access_token");
            api = queryParameterMap.get("api");
        }

        // 校验必传参数
        if(appkey == null){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"appkey不能为空"));
        }
        if(timestampStr == null){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"timestamp不能为空"));
        }
        if(nonce == null){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"nonce不能为空"));
        }
        if(api == null){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"api不能为空"));
        }
        if(sign == null){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"sign不能为空"));
        }

        // 校验数据格式
        if(StringUtils.isBlank(appkey)){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"appkey不能为空"));
        }
        if(appkey.indexOf("t") != 0){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"appkey不正确，请使用测试appkey"));
        }
        if(StringUtils.isBlank(timestampStr)){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"timestamp不能为空"));
        }
        try{
            timestamp = Long.valueOf(timestampStr);
        }catch (NumberFormatException e){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"timestamp格式不正确"));
        }
        if(StringUtils.isBlank(nonce)){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"nonce不能为空"));
        }
        if(StringUtils.isBlank(api)){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"api不能为空"));
        }
        if(StringUtils.isBlank(sign)){
            return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"sign不能为空"));
        }

        String finalAppkey = appkey;
        String finalTimestampStr = timestampStr;
        String finalNonce = nonce;
        String finalAccess_token = access_token;
        String finalApi = api;
        String finalSign = sign;
        return mysqlStorager.findByAppKey(appkey).flatMap(applicationQa -> {
            // 验签
            String generatedSign = Signer.sign(finalAppkey, finalTimestampStr, finalNonce, finalAccess_token, finalApi,applicationQa.getAppSecret(),biz_params);
            if(!generatedSign.equals(finalSign)){
                Pair<String,String> unsignStringAndFinalSign = Signer.getUnsignStringAndFinalSign(finalAppkey, finalTimestampStr, finalNonce, finalAccess_token, finalApi,applicationQa.getAppSecret(),biz_params);
                log.info("签名失败， 签名前字符串：{} 最后签名：{}",unsignStringAndFinalSign.getLeft(),unsignStringAndFinalSign.getRight());
                return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_SIGNATURE));
            }

            // 验证access_token
            boolean needAccessToken = needAccessToken(finalApi);
            if(needAccessToken){
                if(finalAccess_token == null){
                    return Single.error(new OpenGatewayException(CodeEnum.SYS_MISSING_PARAMETER,"access_token不能为空"));
                }
                if(StringUtils.isBlank(finalAccess_token)){
                    return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"access_token不能为空"));
                }
                if(!qaAccessToken.equals(finalAccess_token)){
                    return Single.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"无效的access_token"));
                }
            }

            // qa环境特殊业务参数校验
            validateBizParams(finalAppkey, finalApi,biz_params);

            if(isSelfTestingApi(finalApi)){
                TestApiContext context = new TestApiContext();
                context.setApi(finalApi);
                context.setAppkey(finalAppkey);
                return processTestApi(context,biz_params).map(t -> JSON.toJSONString(new GatewayResponse()));
            }else{
                // 准备调用QA环境开放平台网关
                // 需要将appkey和secret切换成qa环境准备好的appkey和secret
                Pair<String,String> unsignStringAndFinalSign = Signer.getUnsignStringAndFinalSign(remoteAppkey, finalTimestampStr, finalNonce,remoteAccessToken, finalApi,remoteSecret,biz_params);
                log.info("remote gateway unsigned string : {}",unsignStringAndFinalSign.getLeft());
                String remoteSign = unsignStringAndFinalSign.getRight();
                // 构建url
                Map<String,String> remoteGatewayQueryParams = mapClone(queryParameterMap);
                // 需要替换appkey和sign
                remoteGatewayQueryParams.put("appkey", remoteAppkey);
                remoteGatewayQueryParams.put("access_token",remoteAccessToken);
                remoteGatewayQueryParams.put("sign",remoteSign);

                // 拼接url
                StringBuilder requestUrl = new StringBuilder(remoteGateway);
                if(!remoteGateway.contains("?")){
                    requestUrl.append("?");
                }
                boolean firstQaGatewayQueryParam = true;
                for(Map.Entry<String,String> entry : remoteGatewayQueryParams.entrySet()){
                    if(!firstQaGatewayQueryParam){
                        requestUrl.append("&");
                    }
                    requestUrl.append(entry.getKey()).append("=").append(entry.getValue());
                    firstQaGatewayQueryParam = false;
                }

                return Single.create(subscriber -> {
                    httpClient.postAbs(requestUrl.toString())
                            .putHeader("Content-Type","application/json; charset=utf-8")
                            .sendBuffer(Buffer.buffer(biz_params), ar ->{
                                if(ar.succeeded()){
                                    HttpResponse<Buffer> response = ar.result();
                                    if(response.statusCode() != 200){
                                        log.info("remote gateway reresponse http code : {}",response.statusCode());
                                        throw new OpenGatewayException(CodeEnum.SYS_UNKNOWN_ERROR);
                                    }
                                    String result = response.bodyAsString();
                                    log.info("remote gateway response : {}",result);
                                    subscriber.onSuccess(result);
                                }else{
                                    log.error("invoke remote server failure",ar.cause());
                                    subscriber.onError(new OpenGatewayException(CodeEnum.SYS_UNKNOWN_ERROR));
                                }
                            });
                });
            }
        });
    }

    private boolean needAccessToken(String api){
        return "dianwoda.data.city.code".equals(api)? false:true;
    }

    private void validateBizParams(String appkey,String api,String biz_params){
        try{
            switch (api){
                case "dianwoda.seller.transportation.confirm":
                    if(StringUtils.isNotBlank(biz_params)){
                        JSONObject data = JSON.parseObject(biz_params);
                        String seller_id = data.getString("seller_id");
                        if(StringUtils.isBlank(seller_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"seller_id不能为空");
                        }
                        if(seller_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，seller_id请加上'${appkey}_'前缀");
                        }
                    }
                    break;
                case "dianwoda.order.create":
                    if(StringUtils.isNotBlank(biz_params)){
                        JSONObject data = JSON.parseObject(biz_params);
                        String order_original_id = data.getString("order_original_id");
                        if(StringUtils.isBlank(order_original_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"order_original_id不能为空");
                        }
                        if(order_original_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，order_original_id请加上'${appkey}_'前缀");
                        }
                        String seller_id = data.getString("seller_id");
                        if(StringUtils.isBlank(seller_id)){
                            throw new OpenGatewayException(CodeEnum.API_MISSING_PARAMETER,"seller_id不能为空");
                        }
                        if(seller_id.indexOf(appkey) != 0){
                            throw new OpenGatewayException(CodeEnum.API_INVALID_PARAMETER,"qa联调时，seller_id请加上'${appkey}_'前缀");
                        }
                    }
                    break;
            }
        }catch (Exception e){
            if(e instanceof OpenGatewayException) throw e;
            log.error("validateBizParams failure",e);
            throw new OpenGatewayException(CodeEnum.API_BUSINESS_ERROR,"业务参数解析失败");
        }
    }

    private boolean isSelfTestingApi(String api){
        if(api.indexOf("test.") == 0){
            return true;
        }
        return false;
    }

    private Single processTestApi(TestApiContext context,String biz_params){
        Single single = null;
        String api = context.getApi();
        try{
            switch (api){
//                case "test.seller.transportation.confirm-result":
//                    testSellerTransportationConfirmResultProcessor.process(context,biz_params);
//                    break;
                case "test.order.accept":
                    single = testOrderAcceptProcessor.process(context,biz_params);
                    break;
//                case "test.order.arrive":
//                    testOrderArriveProcessor.process(context,biz_params);
//                    break;
//                case "test.order.leave":
//                    testOrderLeaveProcessor.process(context,biz_params);
//                    break;
//                case "test.order.complete":
//                    testOrderCompleteProcessor.process(context,biz_params);
//                    break;
//                case "test.order.abnormal":
//                    testOrderAbnormalProcessor.process(context,biz_params);
//                    break;
//                case "test.order.cancel":
//                    testOrderCancelProcessor.process(context,biz_params);
//                    break;
//                case "test.order.transfer":
//                    testOrderTransferProcessor.process(context,biz_params);
//                    break;
                default:
                    single = Single.error(new OpenGatewayException(CodeEnum.SYS_API_NOT_EXISTED));
            }
        }catch (Exception e){
            if(e instanceof OpenGatewayException) throw e;
            if(e instanceof ApiBizException) throw e;
            log.error("自测api处理异常",e);
            single = Single.error(new OpenGatewayException(CodeEnum.API_BUSINESS_ERROR,"api业务系统异常，请稍后再试"));
        }
        return single;
    }

    private Map<String,String> mapClone(Map<String,String> origin){
        Map<String,String> clone = new HashMap<>(origin.size());
        clone.putAll(origin);
        return clone;
    }
}
