package io.seata.rm.tcc.parameterfetcher;

import java.util.Map;
import javax.annotation.Nonnull;

import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.ParamType;
import io.seata.rm.tcc.api.ParameterFetcher;
import io.seata.rm.tcc.interceptor.ActionContextUtil;

/**
 * @author wang.liang
 */
public class MockBooleanParameterFetcher implements ParameterFetcher<Boolean> {

    @Override
    public void fetchContext(@Nonnull ParamType paramType, @Nonnull String paramName, @Nonnull Boolean paramValue,
            @Nonnull BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> actionContext) {
        Object paramObject;

        // fetch context
        if (paramValue) {
            paramObject = "yes";
        } else {
            paramObject = "no";
        }

        // put into the context
        ActionContextUtil.putObjectByParamName(paramName, paramObject, annotation, actionContext);
    }
}
