package com.lms.gateway;

import com.lms.shared.AppException;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof AppException app) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.DataFetchingException)
                    .message(app.getMessage())
                    .extensions(Map.of("code", app.getCode()))
                    .build();
        }
        return null;
    }
}
