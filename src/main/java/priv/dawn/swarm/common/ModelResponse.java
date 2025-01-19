package priv.dawn.swarm.common;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/19/13:21
 */

@Data
public class ModelResponse {

    private int type;

    private String content;

    private ToolFunctionCall call;

    public enum Type{
        STREAM(0),
        FINISH(1),
        TOOL(2),
        OVER_LENGTH(-1),
        ;
        public final int code;
        Type(int code){
            this.code = code;
        }
    }

}
