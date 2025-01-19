package priv.dawn.swarm.api;

import priv.dawn.swarm.common.Agent;
import priv.dawn.swarm.common.AgentMessage;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/16:58
 */
public interface AgentClient {

    List<AgentMessage> run(Agent agent, List<AgentMessage> messages, int maxTurn);

}
