package cn.perf4j;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by LinShunkang on 2018/3/16
 */

/**
 * 该类用于在JVM关闭前通过调用asyncRecordProcessor把内存中的数据处理完，保证尽量不丢失采集的数据
 */
public class ShutdownHook implements InitializingBean {

    private RecorderContainer recorderContainer;

    private AsyncRecordProcessor asyncRecordProcessor;

    public void setRecorderContainer(RecorderContainer recorderContainer) {
        this.recorderContainer = recorderContainer;
    }

    public void setAsyncRecordProcessor(AsyncRecordProcessor asyncRecordProcessor) {
        this.asyncRecordProcessor = asyncRecordProcessor;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(recorderContainer, "recorderContainer is required!!!");
        Assert.notNull(asyncRecordProcessor, "asyncRecordProcessor is required!!!");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("ENTER ShutdownHook...");
                try {
                    Map<String, AbstractRecorder> recorderMap = recorderContainer.getRecorderMap();
                    for (Map.Entry<String, AbstractRecorder> entry : recorderMap.entrySet()) {
                        AbstractRecorder recorder = entry.getValue();
                        asyncRecordProcessor.process(recorder.getApi(), recorder.getStartMilliTime(), recorder.getStopMilliTime(), recorder.getSortedTimingRecords());
                    }

                    ThreadPoolExecutor executor = asyncRecordProcessor.getExecutor();
                    executor.shutdown();
                    executor.awaitTermination(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("EXIT ShutdownHook...");
                }
            }
        }));
    }
}
