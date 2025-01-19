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

    /**
     * 下面两个分别是 tool call 的调用和响应
     * openai 可以在一个 message 中平行调用多个 tool_call，但是 DashScope 一次智能调用一个 tool_call
     * 但是每一个 tool 的 result 都需要一个 message
     */
    private List<ToolFunctionCall> toolCalls;
    private ToolFunctionResult toolResult;
}
