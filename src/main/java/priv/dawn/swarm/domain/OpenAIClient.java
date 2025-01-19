package priv.dawn.swarm.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.assistants.run.ToolChoice;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.function.FunctionDefinition;
import com.theokanning.openai.service.OpenAiService;
import lombok.SneakyThrows;
import priv.dawn.swarm.api.BaseAgentClient;
import priv.dawn.swarm.common.*;
import priv.dawn.swarm.enums.Roles;
import priv.dawn.swarm.enums.ToolChoices;

import java.util.ArrayList;
import java.util.List;
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
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage(agent.getInstructions()));

        List<String> toolNameList = agent.getFunctions().getNameList();

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

        //

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(agent.getModel())
                .messages(chatMessages)
                .n(1)
                .maxTokens(8192)
                .tools(cast2ToolDefinition(agent.getFunctions()))
                .toolChoice(cast2ToolChoice(agent.getToolChoice()))
                .parallelToolCalls(true)
                .build();
        ChatCompletionResult chatCompletion = service.createChatCompletion(chatCompletionRequest);
        ChatCompletionChoice originalRsp = chatCompletion.getChoices().get(0);
        String finishReason = originalRsp.getFinishReason();
        AssistantMessage originalRspMsg = originalRsp.getMessage();

        ModelResponse response = new ModelResponse();
        switch (finishReason){
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

    @SneakyThrows
    private ChatToolCall cast2ChatCall(ToolFunctionCall call, List<String> nameList) {
        ChatToolCall chatToolCall = new ChatToolCall();
        chatToolCall.setId(call.getCallId());
        chatToolCall.setType("function");
        chatToolCall.setIndex(nameList.indexOf(call.getName()));
        chatToolCall.setFunction(
                new ChatFunctionCall(call.getName(), mapper.readTree(call.getJsonParam()))
        );
        return chatToolCall;
    }

    @SneakyThrows
    private ToolFunctionCall cast2ToolCall(ChatToolCall call){

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
}
