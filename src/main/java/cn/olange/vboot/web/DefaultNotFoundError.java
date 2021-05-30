package cn.olange.vboot.web;

import cn.olange.vboot.annotation.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Verticle
public class DefaultNotFoundError extends ErrorHandler{
    @Override
    public int getCode() {
        return 404;
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject data = new JsonObject();
        data.put("code", 0);
        data.put("msg", "无效的资源");
        data.put("data", "");
        context.response().end(data.toBuffer());
    }
}