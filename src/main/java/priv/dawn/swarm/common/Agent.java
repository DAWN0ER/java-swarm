package priv.dawn.swarm.common;

import lombok.Builder;
import lombok.Getter;
import priv.dawn.swarm.domain.FunctionRepository;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/19:48
 */

@Builder
@Getter
public class Agent {

    @Builder.Default
    private String name = "Agent";

    @Builder.Default
    private String model = null;

    @Builder.Default
    private String instructions = "You are a helpful agent.";

    @Builder.Default
    private FunctionRepository functions = null;

    @Builder.Default
    private String toolChoice = ToolChoice.NONE.value;

    @Builder.Default
    private boolean parallelToolCalls = true;

    enum ToolChoice{
        AUTO("auto"),
        NONE("none"),
        ;
        public final String value;

        ToolChoice(String value) {
            this.value = value;
        }
    }

}
