package io.github.parvez3019;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class PerformanceTest {

    @Test
    void TestingPerformanceOfInfo() {
        org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);
        int runs = 100 * 1000;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++)
            LOGGER.info("here");
        long time = System.nanoTime() - start;
        System.out.printf("Average log time was %,d ns%n", time / runs);

        Logger logger = new Logger();
        long infoStackingStartTime = System.nanoTime();
        for (int i = 0; i < runs; i++)
            logger.info("here");
        long infoStackingEndTime = System.nanoTime() - infoStackingStartTime;
        System.out.printf("Average log time was %,d ns%n", infoStackingEndTime / runs);
    }

    @Test
    void TestingPerformanceOfError() {
        org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);
        int runs = 100 * 1000;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++)
            LOGGER.error("here");
        long time = System.nanoTime() - start;
        System.out.printf("Average log time was %,d ns%n", time / runs);

        Logger logger = new Logger();
        for (int i = 0; i < runs; i++)
            logger.info("Printing logs! {}" + i);
        long errorStartTime = System.nanoTime();
        logger.error("print error {}", "some error");
        long errorEndTime = System.nanoTime() - errorStartTime;
        System.out.printf("Average log time was %,d ns%n", errorEndTime / runs);
    }
}
