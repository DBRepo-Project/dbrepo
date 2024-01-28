package at.tuwien.annotations;

import at.tuwien.listener.DatabaseListener;
import at.tuwien.listener.MirrorListener;
import at.tuwien.listener.StorageListener;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@MockBeans({@MockBean(DatabaseListener.class), @MockBean(MirrorListener.class), @MockBean(StorageListener.class)})
public @interface MockListeners {
}
