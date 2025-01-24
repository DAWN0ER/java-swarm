package priv.dawn.swarm.common;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/16:58
 */

@Data
public class AgentStreamMessage {

    private int msgIndex;
    private AgentMessage message;

}
