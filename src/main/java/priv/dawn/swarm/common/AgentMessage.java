package priv.dawn.swarm.common;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/17:08
 */

@Data
public class AgentMessage {

    private String role;
    private String content;
    private List<ToolFunctionCall> toolCalls;
}
