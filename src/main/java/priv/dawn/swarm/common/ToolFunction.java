package priv.dawn.swarm.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import priv.dawn.swarm.api.CallableFunction;

/**
 * Created with IntelliJ IDEA.
 * Description: 工具函数
 *
 * @author Dawn Yang
 * @since 2025/01/19/9:56
 */

@Getter
@AllArgsConstructor
public class ToolFunction {

    private final String name;
    private final String parameters;
    private final String description;

    private final CallableFunction callableFunction;

}
