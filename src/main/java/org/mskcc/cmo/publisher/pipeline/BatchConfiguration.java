package org.mskcc.cmo.publisher.pipeline;

import java.util.Map;
import java.util.concurrent.Future;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.publisher.pipeline.limsrest.LimsRequestProcessor;
import org.mskcc.cmo.publisher.pipeline.limsrest.LimsRequestReader;
import org.mskcc.cmo.publisher.pipeline.limsrest.LimsRequestWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 *
 * @author ochoaa
 */
@Configuration
@EnableBatchProcessing
@EnableAsync
@ComponentScan(basePackages = "org.mskcc.cmo.messaging")
public class BatchConfiguration {

    public static final String LIMS_REQUEST_PUBLISHER_JOB = "limsRequestPublisherJob";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    private Gateway messagingGateway;

    @Autowired
    public void initGatewayConnection() throws Exception {
        messagingGateway.connect();
    }

    /**
     * limsRequestPublisherJob
     * @return
     */
    @Bean
    public Job limsRequestPublisherJob() {
        return jobBuilderFactory.get(LIMS_REQUEST_PUBLISHER_JOB)
                .start(limsRequestPublisherStep())
                .build();
    }

    /**
     * limsRequestPublisherStep
     * @return
     */
    @Bean
    public Step limsRequestPublisherStep() {
        return stepBuilderFactory.get("limsRequestPublisherStep")
                .<String, Future<Map<String,Object>>>chunk(10)
                .reader(limsRequestReader())
                .processor(asyncItemProcessor())
                .writer(asyncItemWriter())
                .build();
    }

    /**
     * asyncLimsRequestThreadPoolTaskExecutor
     * @return
     */
    @Bean(name = "asyncLimsRequestThreadPoolTaskExecutor")
    @StepScope
    public ThreadPoolTaskExecutor asyncLimsRequestThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.initialize();
        return executor;
    }

    /**
     * processorThreadPoolTaskExecutor
     * @return
     */
    @Bean(name = "processorThreadPoolTaskExecutor")
    @StepScope
    public ThreadPoolTaskExecutor processorThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    /**
     * asyncItemProcessor
     * @return
     */
    @Bean
    @StepScope
    public ItemProcessor<String, Future<Map<String, Object>>> asyncItemProcessor() {
        AsyncItemProcessor<String, Map<String, Object>> asyncItemProcessor = new AsyncItemProcessor();
        asyncItemProcessor.setTaskExecutor(processorThreadPoolTaskExecutor());
        asyncItemProcessor.setDelegate(limsRequestProcessor());
        return asyncItemProcessor;
    }

    /**
     * limsRequestProcessor
     * @return
     */
    @Bean
    @StepScope
    public LimsRequestProcessor limsRequestProcessor() {
        return new LimsRequestProcessor();
    }

    /**
     * asyncItemWriter
     * @return
     */
    @Bean
    @StepScope
    public ItemWriter<Future<Map<String, Object>>> asyncItemWriter() {
        AsyncItemWriter<Map<String, Object>> asyncItemWriter = new AsyncItemWriter();
        asyncItemWriter.setDelegate(limsRequestWriter());
        return asyncItemWriter;
    }

    /**
     * limsRequestWriter
     * @return
     */
    @Bean
    @StepScope
    public ItemStreamWriter<Map<String, Object>> limsRequestWriter() {
        return new LimsRequestWriter();
    }

    /**
     * limsRequestReader
     * @return
     */
    @Bean
    @StepScope
    public ItemStreamReader<String> limsRequestReader() {
        return new LimsRequestReader();
    }
}
