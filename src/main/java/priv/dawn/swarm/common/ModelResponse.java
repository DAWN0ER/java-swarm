package priv.dawn.swarm.common;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/13:21
 */

@Data
public class ModelResponse {

    /**
     * 和 finish reason 对应，所以默认为 stream
     */
    private int type;

    private String content;

    private List<ToolFunctionCall> calls;

    public enum Type {
        STREAM(0),
        DUPLICATED(-2), /** 用于标记其他 jdk 中 stream call 时不需要的消息 **/
        FINISH(1),
        TOOL_CALL(2),
        OVER_LENGTH(-1),
        ;
        public final int code;

        Type(int code) {
            this.code = code;
        }
    }

    /**
     * type,name, tool_calls 中每个 call 的 name, callId 非空则覆盖
     * content, tool_calls 中每个 call 的 jsonParam 非空则增强
     *
     * @param response stream 中携带新字段的
     */
    public void coverFiledWith(ModelResponse response) {
        if (Objects.isNull(response) || response.getType()==Type.DUPLICATED.code) {
            return;
        }
        if (response.getType() != Type.STREAM.code) {
            this.type = response.getType();
        }
        if (StringUtils.isNotEmpty(response.getContent())) {
            if (Objects.isNull(this.content)) {
                this.content = response.getContent();
            } else {
                this.content = this.content + response.getContent();
            }
        }
        if (Objects.nonNull(response.getCalls())) {
            coverCallWith(response.getCalls());
        }
    }

    private void coverCallWith(List<ToolFunctionCall> refCalls) {
        if (Objects.isNull(this.calls)){
            this.calls = new ArrayList<>(refCalls.size());
        }
        while (refCalls.size() > this.calls.size()) {
            // 填充到相同长度
            this.calls.add(new ToolFunctionCall());
        }
        for (int idx = 0; idx < refCalls.size(); idx++) {
            ToolFunctionCall call = refCalls.get(idx);
            if (Objects.isNull(call)) {
                continue;
            }
            ToolFunctionCall ownCall = this.calls.get(idx);
            if(StringUtils.isNotEmpty(call.getCallId())){
                ownCall.setCallId(call.getCallId());
            }
            if(StringUtils.isNotEmpty(call.getName())){
                ownCall.setName(call.getName());
            }
            if (StringUtils.isNotEmpty(call.getJsonParam())) {
                if(Objects.isNull(ownCall.getJsonParam())){
                    ownCall.setJsonParam(call.getJsonParam());
                } else {
                    String temp = ownCall.getJsonParam();
                    ownCall.setJsonParam(temp + call.getJsonParam());
                }
            }
        }
    }

}
