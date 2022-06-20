package at.tuwien.seeder.impl;

import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.repository.jpa.ImageDateRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.seeder.Seeder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ImageSeederImpl extends AbstractSeeder implements Seeder {

    private final ImageRepository imageRepository;
    private final ImageDateRepository imageDateRepository;

    @Autowired
    public ImageSeederImpl(ImageRepository imageRepository, ImageDateRepository imageDateRepository) {
        this.imageRepository = imageRepository;
        this.imageDateRepository = imageDateRepository;
    }

    @Override
    @Transactional
    public void seed() {
        if (imageRepository.existsById(IMAGE_1_ID)) {
            log.warn("Already seeded. Skip.");
            return;
        }
        final ContainerImage imageMariaDb = imageRepository.save(IMAGE_1);
        log.info("Seeded image id {}", imageMariaDb.getId());
        final ContainerImageDate date1 = imageDateRepository.save(IMAGE_DATE_1);
        log.info("Seeded image date id {}", date1.getId());
        final ContainerImageDate date2 = imageDateRepository.save(IMAGE_DATE_2);
        log.info("Seeded image date id {}", date2.getId());
        final ContainerImageDate date3 = imageDateRepository.save(IMAGE_DATE_3);
        log.info("Seeded image date id {}", date3.getId());
        final ContainerImageDate date4 = imageDateRepository.save(IMAGE_DATE_4);
        log.info("Seeded image date id {}", date4.getId());
        final ContainerImageDate date5 = imageDateRepository.save(IMAGE_DATE_5);
        log.info("Seeded image date id {}", date5.getId());
        final ContainerImageDate date6 = imageDateRepository.save(IMAGE_DATE_6);
        log.info("Seeded image date id {}", date6.getId());
        final ContainerImageDate date7 = imageDateRepository.save(IMAGE_DATE_7);
        log.info("Seeded image date id {}", date7.getId());
        final ContainerImageDate date8 = imageDateRepository.save(IMAGE_DATE_8);
        log.info("Seeded image date id {}", date8.getId());
    }

}
