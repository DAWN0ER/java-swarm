package priv.dawn.swarm.domain;

import com.google.gson.Gson;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import priv.dawn.swarm.api.CallableFunction;
import priv.dawn.swarm.common.ToolFunction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/10:01
 */
public class FunctionRepository {

    private final Map<String, ToolFunction> functionMap;

    public FunctionRepository(){
        functionMap = new HashMap<>();
    }

    public void register(ToolFunction tool) {
        functionMap.put(tool.getName(), tool);
    }

    public ToolFunction getTool(String name) {
        return functionMap.get(name);
    }

    public FunctionFactory factory() {
        return new FunctionFactory(this);
    }

    public FunctionRepository subRepository(String... names){
        HashSet<String> subNameSet = new HashSet<>(Arrays.asList(names));
        Map<String, ToolFunction> collect = this.functionMap.entrySet().stream()
                .filter(entry -> subNameSet.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new FunctionRepository(collect);
    }

    private FunctionRepository(Map<String, ToolFunction> functionMap){
        this.functionMap = functionMap;
    }

    public static class FunctionFactory {

        private final HashMap<String, String> paramDesc = new HashMap<>();
        private String name;
        private String desc;
        private CallableFunction function;
        private final FunctionRepository repository;

        private FunctionFactory(FunctionRepository repository) {
            this.repository = repository;
        }

        public FunctionFactory name(String name) {
            this.name = name;
            return this;
        }

        public FunctionFactory description(String description) {
            this.desc = description;
            return this;
        }

        public FunctionFactory addParamDescription(String name, String description) {
            this.paramDesc.put(name, description);
            return this;
        }

        public FunctionFactory functionCall(CallableFunction function) {
            this.function = function;
            return this;
        }

        /**
         * TODO 如果构造失败，就抛出一个异常
         *
         * @return 构造好的 Function 实例
         */
        public void register() {
            if (StringUtils.isBlank(name) || StringUtils.isBlank(desc)) {
                return;
            }
            if (Objects.isNull(function)) {
                return;
            }
            String paramJson = null;
            if (MapUtils.isNotEmpty(paramDesc)) {
                paramJson = new Gson().toJson(paramDesc);
            }
            ToolFunction tool = new ToolFunction(name, paramJson, desc, function);
            this.repository.register(tool);
        }

    }

}
