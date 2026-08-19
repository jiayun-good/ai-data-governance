package com.mp.executor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleExecutorFactory {

    private final Map<String,RuleExecutor> executorMap;

    public RuleExecutorFactory(
            List<RuleExecutor> executors
    ){

        executorMap =
                executors.stream()
                        .collect(
                                Collectors.toMap(
                                        RuleExecutor::getRuleType,
                                        executor -> executor
                                )
                        );

    }

    public RuleExecutor getExecutor(String type){

        RuleExecutor executor = executorMap.get(type);

        if(executor == null){
            throw new RuntimeException(
                    "不支持的规则类型:"
                            + type
            );

        }
        return executor;
    }
}