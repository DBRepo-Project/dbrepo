package at.tuwien.annotations;

import at.tuwien.listener.BrokerListener;
import com.rabbitmq.client.Channel;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MockBeans({@MockBean(Channel.class), @MockBean(BrokerListener.class)})
public @interface MockAmqp {
}
