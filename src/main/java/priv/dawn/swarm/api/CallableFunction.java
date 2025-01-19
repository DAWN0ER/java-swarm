package priv.dawn.swarm.api;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/18/20:15
 */

public interface CallableFunction {

    /**
     *
     * @param jsonParam 函数入参，会传递一个 json 字符串
     * @return 返回工具函数调用结果，要求序列化成字符串
     */
    String call(String jsonParam);

}