package me.luliru.gateway.core.processor;

import io.reactivex.Single;

/**
 * TestApiProcessor
 * Created by luliru on 2019-07-04.
 */
public interface TestApiProcessor<T> {

    Single<T> process(TestApiContext context, String biz_params);
}
