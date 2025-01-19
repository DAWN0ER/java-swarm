package priv.dawn.swarm.common;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/17:01
 */

@Data
public class ToolFunctionCall {

    private String callId;
    private String name;
    private String jsonParam;
}
