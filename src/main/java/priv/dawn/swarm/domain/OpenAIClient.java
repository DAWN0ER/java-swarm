package priv.dawn.swarm.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.run.ToolChoice;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.function.FunctionDefinition;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import lombok.SneakyThrows;
import priv.dawn.swarm.api.BaseAgentClient;
import priv.dawn.swarm.common.*;
import priv.dawn.swarm.enums.Roles;
import priv.dawn.swarm.enums.ToolChoices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/19:53
 */

public class OpenAIClient extends BaseAgentClient {

    private final OpenAiService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAIClient(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
        service = new OpenAiService(apiKey, baseUrl);
    }

    @Override
    protected ModelResponse modelCall(Agent agent, List<AgentMessage> messages) {

        ChatCompletionRequest chatCompletionRequest = buildParam(agent, messages);
        ChatCompletionResult chatCompletion = service.createChatCompletion(chatCompletionRequest);
        ChatCompletionChoice originalRsp = chatCompletion.getChoices().get(0);
        String finishReason = originalRsp.getFinishReason();
        AssistantMessage originalRspMsg = originalRsp.getMessage();
        ModelResponse response = new ModelResponse();
        switch (finishReason) {
            case "stop":
                response.setType(ModelResponse.Type.FINISH.code);
                response.setContent(originalRspMsg.getContent());
                break;
            case "length":
                response.setType(ModelResponse.Type.OVER_LENGTH.code);
                response.setContent(originalRspMsg.getContent());
                break;
            case "tool_calls":
                response.setType(ModelResponse.Type.TOOL_CALL.code);
                List<ToolFunctionCall> calls = originalRspMsg.getToolCalls().stream()
                        .map(this::cast2ToolCall)
                        .collect(Collectors.toList());
                response.setCalls(calls);
        }
        // DEGUG
//        System.out.println("OpenAIClient.modelCall");
//        System.out.println("response = " + new Gson().toJson(response));
        return response;
    }

    @Override
    protected Flowable<ModelResponse> modelStreamCall(Agent agent, List<AgentMessage> messages) {
        ChatCompletionRequest chatCompletionRequest = buildParam(agent, messages);
        Flowable<ChatCompletionChunk> orgFlowable = service.streamChatCompletion(chatCompletionRequest);
        return orgFlowable.map(this::chatChunk2Rsp);
    }

    private ChatCompletionRequest buildParam(Agent agent, List<AgentMessage> messages) {
        // 前期准备
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage(agent.getInstructions()));
        List<String> toolNameList = agent.getFunctions().getNameList();

        // 将 AgentMessage 转换为 ChatMessage
        for (AgentMessage msg : messages) {
            if (Roles.SYSTEM.value.equals(msg.getRole())) {
                chatMessages.add(new SystemMessage(msg.getContent()));
            } else if (Roles.USER.value.equals(msg.getRole())) {

                chatMessages.add(new UserMessage(msg.getContent()));
            } else if (Roles.ASSISTANT.value.equals(msg.getRole())) {
                AssistantMessage message = new AssistantMessage();
                message.setContent(msg.getContent());
                List<ToolFunctionCall> toolCalls = msg.getToolCalls();
                List<ChatToolCall> callList = toolCalls.stream()
                        .map(toolCall -> cast2ChatCall(toolCall, toolNameList))
                        .collect(Collectors.toList());
                message.setToolCalls(callList);
                chatMessages.add(message);
            } else if (Roles.TOOL.value.equals(msg.getRole())) {
                ToolMessage toolMessage = new ToolMessage();
                toolMessage.setToolCallId(msg.getToolResult().getCallId());
                toolMessage.setContent(msg.getToolResult().getResult());
                chatMessages.add(toolMessage);
            }
        }

        // 构造参数，调用AI接口
        return ChatCompletionRequest.builder()
                .model(agent.getModel())
                .messages(chatMessages)
                .n(1)
                .maxTokens(8192)
                .tools(cast2ToolDefinition(agent.getFunctions()))
                .toolChoice(cast2ToolChoice(agent.getToolChoice()))
                .parallelToolCalls(true)
                .build();
    }

    @SneakyThrows
    private ChatToolCall cast2ChatCall(ToolFunctionCall call, List<String> nameList) {
        ChatToolCall chatToolCall = new ChatToolCall();
        chatToolCall.setId(call.getCallId());
        chatToolCall.setType("function");
        chatToolCall.setIndex(nameList.indexOf(call.getName()));
        JsonNode paramNode = mapper.createObjectNode(); // {}
        if (Objects.nonNull(call.getJsonParam())) {
            paramNode = mapper.readTree(call.getJsonParam());
        }
        chatToolCall.setFunction(
                new ChatFunctionCall(call.getName(), paramNode)
        );
        return chatToolCall;
    }

    @SneakyThrows
    private ToolFunctionCall cast2ToolCall(ChatToolCall call) {

        ToolFunctionCall toolCall = new ToolFunctionCall();
        toolCall.setCallId(call.getId());
        toolCall.setName(call.getFunction().getName());
        toolCall.setJsonParam(mapper.writeValueAsString(call.getFunction().getArguments()));
        return toolCall;
    }

    private List<ChatTool> cast2ToolDefinition(FunctionRepository functionRepository) {
        List<String> nameList = functionRepository.getNameList();
        List<ChatTool> chatTools = new ArrayList<>();
        for (String name : nameList) {
            ToolFunction tool = functionRepository.getTool(name);
            chatTools.add(new ChatTool(FunctionDefinition.builder()
                    .name(name)
                    .description(tool.getDescription())
                    .parametersDefinition(tool.getParameters())
                    .build())
            );
        }
        return chatTools;
    }

    private ToolChoice cast2ToolChoice(String choice) {
        if (ToolChoices.AUTO.value.equals(choice)) {
            return ToolChoice.AUTO;
        } else if (ToolChoices.REQUIRED.value.equals(choice)) {
            return ToolChoice.REQUIRED;
        } else if (ToolChoices.NONE.value.equals(choice)) {
            return ToolChoice.NONE;
        }
        return null;
    }

    @SneakyThrows
    private ModelResponse chatChunk2Rsp(ChatCompletionChunk chunk) {
        // 将 chunk 封装成 Response
        ModelResponse response = new ModelResponse();
        if (Objects.isNull(chunk) || chunk.getCreated() == 0L) {
            response.setType(ModelResponse.Type.DUPLICATED.code);
            return response;
        }
        ChatCompletionChoice choice = Optional.ofNullable(chunk.getChoices()).map(a -> a.get(0)).orElse(null);
        if (Objects.isNull(choice)) {
            return response;
        }
        String finishReason = Optional.ofNullable(choice.getFinishReason()).orElse(""); // 避免 null
        switch (finishReason) {
            case "stop":
                response.setType(ModelResponse.Type.FINISH.code);
                break;
            case "tool_calls":
                response.setType(ModelResponse.Type.TOOL_CALL.code);
                break;
            case "length":
                response.setType(ModelResponse.Type.OVER_LENGTH.code);
                break;
        }
        AssistantMessage message = choice.getMessage();
        Optional.ofNullable(message).map(AssistantMessage::getContent).ifPresent(response::setContent);
        List<ChatToolCall> calls = Optional.ofNullable(message).map(AssistantMessage::getToolCalls).orElse(null);
        // calls stream 依次返回调用的部分信息，其中有 index 参数指明位置
        if (Objects.nonNull(calls) && !calls.isEmpty()) {
            List<ToolFunctionCall> myCalls = new ArrayList<>();
            response.setCalls(myCalls);
            for (ChatToolCall call : calls) {
                // call 存在则默认有 index，且默认顺序递增
                int index = call.getIndex();
                while (myCalls.size() <= index) {
                    // 填充空参数
                    myCalls.add(new ToolFunctionCall());
                }
                Optional.ofNullable(call.getId()).ifPresent(id -> myCalls.get(index).setCallId(id));
                Optional.ofNullable(call.getFunction()).map(ChatFunctionCall::getName)
                        .ifPresent(name -> myCalls.get(index).setName(name));
                Optional.ofNullable(call.getFunction()).map(ChatFunctionCall::getArguments).map(JsonNode::asText)
                        .ifPresent(paramStr -> myCalls.get(index).setJsonParam(paramStr));
            }
        }
        return response;

    }
}
