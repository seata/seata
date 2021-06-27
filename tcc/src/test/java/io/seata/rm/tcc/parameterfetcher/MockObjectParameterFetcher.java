package io.seata.rm.tcc.parameterfetcher;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import io.seata.rm.tcc.TccParam;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.ParamType;
import io.seata.rm.tcc.api.ParameterFetcher;
import io.seata.rm.tcc.interceptor.ActionContextUtil;

/**
 * @author wang.liang
 */
public class MockObjectParameterFetcher implements ParameterFetcher<TccParam> {

    @Override
    public void fetchContext(@Nonnull ParamType paramType, @Nonnull String paramName, TccParam param,
            @Nonnull BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> actionContext) {
        // fetch context
        Map<String, Object> paramContext = new HashMap<>();
        paramContext.put("num", param.getNum());
        paramContext.put("name", param.getName());

        // put into the context
        ActionContextUtil.putContextByParamName(paramContext, annotation, actionContext);
    }
}
