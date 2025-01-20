package priv.dawn.swarm.api;

import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import priv.dawn.swarm.common.*;
import priv.dawn.swarm.domain.FunctionRepository;
import priv.dawn.swarm.enums.Roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * Description: AgentClient 模板
 *
 * @author Dawn Yang
 * @since 2025/01/19/17:15
 */

@Getter
public abstract class BaseAgentClient implements AgentClient {

    protected String apiKey;
    protected String baseUrl;
    // TODO 临时使用的单线程池，后续肯定提供其他配置
    protected ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();

    protected abstract ModelResponse modelCall(Agent agent, List<AgentMessage> messages);

    protected abstract Flowable<ModelResponse> modelStreamCall(Agent agent, List<AgentMessage> messages);

    public BaseAgentClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<AgentMessage> run(Agent agent, List<AgentMessage> messages, int maxTurn) {
        // TODO 前置校验抛异常

        List<AgentMessage> appendMsg = new ArrayList<>(maxTurn);
        AgentMessage prompt = new AgentMessage();
        prompt.setRole(Roles.SYSTEM.value);
        prompt.setContent(agent.getInstructions());

        while (appendMsg.size() < maxTurn) {
            List<AgentMessage> allMsg = new ArrayList<>(messages.size() + appendMsg.size() + 1);
            allMsg.add(prompt);
            allMsg.addAll(messages);
            allMsg.addAll(appendMsg);

            // 获取模型响应
            ModelResponse response = modelCall(agent, allMsg);
            AgentMessage message = castFromModelRsp(response);
            appendMsg.add(message);

            // 如果响应是对话结束
            if (ModelResponse.Type.FINISH.code == response.getType()) {
                return appendMsg;
            }

            // 处理函数调用
            if (ModelResponse.Type.TOOL_CALL.code == response.getType()) {
                List<ToolFunctionCall> calls = response.getCalls();
                List<AgentMessage> toolResults = handleFunctionCall(calls, agent);
                appendMsg.addAll(toolResults);
            }
        }
        return appendMsg;
    }

    @Override
    public Flowable<AgentStreamMessage> streamRun(Agent agent, List<AgentMessage> messages, int maxTurn) {
        FlowableProcessor<AgentStreamMessage> processor = PublishProcessor.create();
        singleThreadPool.execute(() -> asyncRun(processor, agent, messages, maxTurn));
        return Flowable.fromPublisher(processor).onBackpressureBuffer();
    }

    private void asyncRun(
            FlowableProcessor<AgentStreamMessage> processor,
            Agent agent,
            List<AgentMessage> messages,
            int maxTurn
    ) {

        AgentMessage prompt = new AgentMessage();
        prompt.setRole(Roles.SYSTEM.value);
        prompt.setContent(agent.getInstructions());
        List<AgentMessage> appendMsg = new ArrayList<>(maxTurn);

        while (appendMsg.size() < maxTurn) {
            List<AgentMessage> allMsg = new ArrayList<>(messages.size() + appendMsg.size() + 1);
            allMsg.add(prompt);
            allMsg.addAll(messages);
            allMsg.addAll(appendMsg);

            // 后续使用的 rsp，所有字段覆盖只允许的深拷贝，并且非 null 字段不允许被 null 覆盖
            ModelResponse thisTurnRsp = new ModelResponse();
            int thisTurn = appendMsg.size(); // idx in appendMsg
            // 获取模型响应
            Flowable<ModelResponse> responseFlowable = modelStreamCall(agent, allMsg);
            // 等待响应的 stream 完成，Response 也全部填充完成，继续走流程就行
            responseFlowable.blockingForEach(chunk -> {
                if (chunk.getType()==ModelResponse.Type.DUPLICATED.code){
                    return;
                }
                // 覆盖 ModelResponse 中需要的字段
                thisTurnRsp.coverFiledWith(chunk);
                // 封装 flowable 信息
                AgentStreamMessage streamMsg = new AgentStreamMessage();
                streamMsg.setMsgIndex(thisTurn);
                // 封装残缺的 msg, 里面的所有引用都来自 model rsp
                AgentMessage message = castFromModelRsp(chunk);
                streamMsg.setMessages(message);
                // 发送 flowable
                processor.onNext(streamMsg);
            });

            // 此时 stream 的所有字段都存储完成。
            AgentMessage message = castFromModelRsp(thisTurnRsp);
            appendMsg.add(message);
            // 如果响应是结束，则返回
            if (ModelResponse.Type.FINISH.code == thisTurnRsp.getType()) {
                break;
            }

            // 处理函数调用
            if (ModelResponse.Type.TOOL_CALL.code == thisTurnRsp.getType()) {
                List<ToolFunctionCall> calls = thisTurnRsp.getCalls();
                List<AgentMessage> toolResultsMsg = handleFunctionCall(calls, agent);
                // 需要把每个 tool Result msg 都 flowable 发送一次。
                toolResultsMsg.forEach(msg -> {
                    AgentStreamMessage streamMessage = new AgentStreamMessage();
                    streamMessage.setMsgIndex(appendMsg.size());
                    streamMessage.setMessages(msg);
                    processor.onNext(streamMessage);
                    appendMsg.add(msg);
                });
            }
        }
        processor.onComplete();
    }

    protected List<AgentMessage> handleFunctionCall(List<ToolFunctionCall> calls, Agent agent) {
        FunctionRepository functionRepository = agent.getFunctions();
        List<AgentMessage> toolMsgList = new ArrayList<>();
        for (ToolFunctionCall call : calls) {
            ToolFunction tool = functionRepository.getTool(call.getName());
            if (Objects.isNull(tool)) {
                // 未找到 tool 的处理逻辑
                ToolFunctionResult noneResult = new ToolFunctionResult();
                noneResult.setResult("Error: Tool \"" + call.getName() + "\" not found.");
                noneResult.setCallId(call.getCallId());
                noneResult.setName(call.getName());
                AgentMessage message = new AgentMessage();
                message.setRole(Roles.TOOL.value);
                message.setToolResult(noneResult);
                toolMsgList.add(message);
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

    // 把 model 接口的 response 的需要字段全部塞进去
    protected AgentMessage castFromModelRsp(ModelResponse rsp) {
        AgentMessage msg = new AgentMessage();
        msg.setRole(Roles.ASSISTANT.value);
        msg.setContent(rsp.getContent());
        msg.setToolCalls(rsp.getCalls());
        return msg;
    }

}
