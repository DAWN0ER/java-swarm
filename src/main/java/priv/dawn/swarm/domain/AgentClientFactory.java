package priv.dawn.swarm.domain;

import lombok.extern.slf4j.Slf4j;
import priv.dawn.swarm.api.AgentClient;
import priv.dawn.swarm.enums.ClientType;

/**
 * Created with IntelliJ IDEA.
 * Description: AgentClientFactory
 *
 * @author Dawn Yang
 * @since 2025/01/22/18:54
 */

@Slf4j
public class AgentClientFactory {

    public static AgentClient create(String apiKey, String baseUrl, ClientType core){
        switch (core){
            case OPEN_AI:
                return new OpenAIClient(apiKey,baseUrl);
            case DASH_SCOPE:
                return new DashScopeClient(apiKey,baseUrl);
        }
        return null;
    }

}
