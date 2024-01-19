package at.tuwien.listener;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

public interface MirrorListener {
    @Scheduled(fixedRateString = "${fda.mirrorRate}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    void mirrorEntities();
}
