import com.alibaba.dashscope.tools.ToolCallFunction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author Dawn Yang
 * @since 2025/01/22/12:23
 */
public class OtherTest {

    @Test
    public void jsonObjTest(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name","getW");
        jsonObject.addProperty("arguments","{\"city\":\"成都\"}");
        Gson gson = new Gson();
        ToolCallFunction.CallFunction function = gson.fromJson(jsonObject, ToolCallFunction.CallFunction.class);
        System.out.println("function.getArguments() = " + function.getArguments());
        System.out.println("function.getName() = " + function.getName());
    }

}
