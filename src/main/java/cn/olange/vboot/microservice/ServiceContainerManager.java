package cn.olange.vboot.microservice;

import cn.olange.vboot.VerticleEvent;
import cn.olange.vboot.annotation.Verticle;
import io.micronaut.inject.BeanDefinition;
import io.vertx.core.Future;

@Verticle
public class ServiceContainerManager extends VerticleEvent {
    private ServiceAnnotatedBuilder serviceAnnotatedBuilder;
    private MicroServiceRegister serviceRegister;

    public ServiceContainerManager(ServiceAnnotatedBuilder serviceAnnotatedBuilder, MicroServiceRegister serviceRegister) {
        this.serviceAnnotatedBuilder = serviceAnnotatedBuilder;
        this.serviceRegister = serviceRegister;
    }

    @Override
    public Future<Void> start(BeanDefinition<?> beanDefinition) {
        for (BeanDefinition record : serviceAnnotatedBuilder.getRecords()) {
            this.serviceRegister.registerService(record);
        }
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> stop(BeanDefinition<?> beanDefinition) {
        return null;
    }
}