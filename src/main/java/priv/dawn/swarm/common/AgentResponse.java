package priv.dawn.swarm.common;

import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/16:58
 */

@Data
public class AgentResponse {

    private List<AgentMessage> messages;

}
