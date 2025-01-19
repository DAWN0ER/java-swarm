package priv.dawn.swarm.api;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import priv.dawn.swarm.common.*;
import priv.dawn.swarm.domain.FunctionRepository;
import priv.dawn.swarm.enums.Roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * Description: AgentClient 模板
 *
 * @author Dawn Yang
 * @since 2025/01/19/17:15
 */

@Getter
@AllArgsConstructor
public abstract class BaseAgentClient implements AgentClient {

    protected String apiKey;
    protected String baseUrl;

    @Override
    public List<AgentMessage> run(Agent agent, List<AgentMessage> messages, int maxTurn) {
        // TODO 前置校验
        List<AgentMessage> appendMsg = new ArrayList<>(maxTurn);
        AgentMessage prompt = new AgentMessage();
        prompt.setRole(Roles.SYSTEM.value);
        prompt.setContent(agent.getInstructions());
        while (appendMsg.size() < maxTurn) {
            List<AgentMessage> allMsg = new ArrayList(messages.size() + appendMsg.size() + 1);
            allMsg.add(prompt);
            allMsg.addAll(messages);
            allMsg.addAll(appendMsg);

            // DEBUG
            System.out.println("Msgs = " + new Gson().toJson(allMsg));
            // 获取模型响应
            ModelResponse response = modelCall(agent, allMsg);
            // 如果响应是结束
            if (ModelResponse.Type.FINISH.code == response.getType()) {
                AgentMessage message = new AgentMessage();
                message.setRole(Roles.ASSISTANT.value);
                message.setContent(response.getContent());
                appendMsg.add(message);
                return appendMsg;
            }

            // 处理函数调用
            if (ModelResponse.Type.TOOL_CALL.code == response.getType()){
                AgentMessage message = new AgentMessage();
                message.setRole(Roles.ASSISTANT.value);
                message.setContent(response.getContent());
                message.setToolCalls(response.getCalls());
                appendMsg.add(message);

                List<ToolFunctionCall> calls = response.getCalls();
                List<AgentMessage> toolResults = handleFunctionCall(calls, agent);
                appendMsg.addAll(toolResults);
            }
        }
        return appendMsg;
    }

    protected abstract ModelResponse modelCall(Agent agent, List<AgentMessage> messages);

    protected List<AgentMessage> handleFunctionCall(List<ToolFunctionCall> calls, Agent agent) {
        FunctionRepository functionRepository = agent.getFunctions();
        List<AgentMessage> toolMsgList = new ArrayList<>();
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
            AgentMessage message = new AgentMessage();
            message.setRole(Roles.TOOL.value);
            message.setToolResult(result);
            toolMsgList.add(message);
        }
        return toolMsgList;
    }


}
