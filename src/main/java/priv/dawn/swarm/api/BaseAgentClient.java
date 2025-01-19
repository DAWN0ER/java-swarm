package priv.dawn.swarm.api;

import org.apache.commons.collections4.ListUtils;
import priv.dawn.swarm.common.*;
import priv.dawn.swarm.domain.FunctionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/17:15
 */
public abstract class BaseAgentClient implements AgentClient {

    @Override
    public AgentResponse run(Agent agent, List<AgentMessage> messages, int maxTurn) {
        // TODO 前置校验
        List<AgentMessage> appendMsg = new ArrayList<>(maxTurn);
        ModelResponse rsp = modelCall(agent, messages);
        while (appendMsg.size() < maxTurn) {
            ModelResponse response = modelCall(agent, ListUtils.union(messages, appendMsg));
            if (ModelResponse.Type.FINISH.code == response.getType()) {
                String content = response.getContent();
                AgentMessage message = new AgentMessage();
//                message.setRole();
            }

        }

//        if (ModelResponse.Type.TOOL.code == rsp.getType())
        return null;
    }

    protected abstract ModelResponse modelCall(Agent agent, List<AgentMessage> messages);

    protected List<AgentMessage> handleFunctionCall(List<ToolFunctionCall> calls, Agent agent) {
        FunctionRepository functionRepository = agent.getFunctions();
        for (ToolFunctionCall call : calls) {
            ToolFunction tool = functionRepository.getTool(call.getName());
            if (Objects.isNull(tool)) {
                // TODO 未找到 tool 的处理逻辑
                continue;
            }
            String stringResult = tool.getCallableFunction().call(call.getJsonParam());
            ToolFunctionResult result = new ToolFunctionResult();
            result.setCallId(call.getCallId());
            result.setName(call.getName());
            result.setResult(stringResult);
        }

        return null;
    }


}
