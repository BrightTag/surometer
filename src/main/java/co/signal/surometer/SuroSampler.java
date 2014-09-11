package co.signal.surometer;

import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Priority;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.suro.ClientConfig;
import com.netflix.suro.client.SyncSuroClient;
import com.netflix.suro.message.Compression;
import com.netflix.suro.message.Message;
import com.netflix.suro.message.MessageSetBuilder;
import com.netflix.suro.thrift.TMessageSet;

/**
 * A {@link org.apache.jmeter.samplers.Sampler Sampler} which produces Suro messages.
 * 
 * @author starzia
 */
public class SuroSampler extends AbstractJavaSamplerClient {

    /** Suro routing key for this particular message. */
    private static final String PARAMETER_MSG_ROUTING_KEY = "SuroSampler.MsgRoutingKey";
    /** Message payload. */
    private static final String PARAMETER_MSG_PAYLOAD = "SuroSampler.MsgPayload";
    
    /**
     * Parameter for setting the Suro servers; it should be comma separated list of $hostname:$port".
     */
    private static final String PARAMETER_LOAD_BALANCER_SERVER = "SuroClient.loadBalancerServer";
    /**
     * Parameter for making requests either synchronous or asynchronous; should be "sync" or "async".
     * Note that "async" mode implies that the sampler will return immediately and report success
     * once the request is queued.
     */
    private static final String PARAMETER_CLIENT_TYPE = "SuroClient.clientType";
    /**
     * When the number of messages queued is up to this value, the client will create and send MessageSet.
     */
    private static final String PARAMETER_ASYNC_BATCH_SIZE = "SuroClient.asyncBatchSize";
    /**
     * Even the number of messages is less than the above value, the client will send messages in
     * the queue any way if up to this much time has elaspsed. Time unit is millisecond.
     */
    private static final String PARAMETER_ASYNC_TIMEOUT = "SuroClient.asyncTimeout";
    /**
     * Can be either file or memory.
     */
    private static final String PARAMETER_ASYNC_QUEUE_TYPE = "SuroClient.asyncQueueType";
    /**
     * The bound of memory queue. The unit is number of messages.
     */
    private static final String PARAMETER_ASYNC_MEMORYQUEUE_CAPACITY = "SuroClient.asyncMessageQueueCapacity";
    /**
     * file queue directory path
     */
    private static final String PARAMETER_ASYNC_FILEQUEUE_PATH = "SuroClient.asyncFileQueuePath";

    private SyncSuroClient client;

    private ClientConfig config;
    private Compression compression;
    
    private Injector injector;

    private Injector createInjector(final Properties properties) {
        injector = LifecycleInjector
                .builder()
                .withBootstrapModule(
                    new BootstrapModule() {
                        public void configure(BootstrapBinder binder) {
                            binder.bindConfigurationProvider().toInstance(
                                    new PropertiesConfigurationProvider(properties));
                        }
                    }
                )
                .withModules(new SuroSamplerModule())
                .build().createInjector();
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        try {
            manager.start();
        } catch (Exception e) {
            throw new RuntimeException("LifecycleManager cannot start with an exception: " + e.getMessage(), e);
        }
        return injector;
    }


    @Override
    public void setupTest(JavaSamplerContext context) {
        // setup the injection engine with parameters from JMeter
        final Properties clientProperties = new Properties();
        clientProperties.setProperty( ClientConfig.LB_TYPE, "static" );
        clientProperties.setProperty( ClientConfig.LB_SERVER, 
                                      context.getParameter( PARAMETER_LOAD_BALANCER_SERVER ) );
        clientProperties.setProperty( ClientConfig.CLIENT_TYPE, 
                                      context.getParameter( PARAMETER_CLIENT_TYPE ) );
        clientProperties.setProperty( ClientConfig.ASYNC_BATCH_SIZE, 
                                      context.getParameter( PARAMETER_ASYNC_BATCH_SIZE ) );
        clientProperties.setProperty( ClientConfig.ASYNC_TIMEOUT, 
                                      context.getParameter( PARAMETER_ASYNC_TIMEOUT ) );
        clientProperties.setProperty( ClientConfig.ASYNC_QUEUE_TYPE, 
                                      context.getParameter( PARAMETER_ASYNC_QUEUE_TYPE ) );
        clientProperties.setProperty( ClientConfig.ASYNC_MEMORYQUEUE_CAPACITY, 
                                      context.getParameter( PARAMETER_ASYNC_MEMORYQUEUE_CAPACITY ) );
        clientProperties.setProperty( ClientConfig.ASYNC_FILEQUEUE_PATH, 
                                      context.getParameter( PARAMETER_ASYNC_FILEQUEUE_PATH ) );
        createInjector( clientProperties );
        
        // setup the injection engine
        this.config = injector.getInstance(ClientConfig.class);
        this.compression = Compression.create(config.getCompression());

        // setup the Suro client
        client = injector.getInstance(SyncSuroClient.class);
        getLogger().log( Priority.INFO, "setup SuroClient" );
    }


    @Override
    public void teardownTest( JavaSamplerContext context ) {
        // shutdown the injector
        injector.getInstance(LifecycleManager.class).close();
        getLogger().log( Priority.INFO, "shutdown SuroClient" );
    }


    @Override
    public Arguments getDefaultParameters() {
      Arguments defaultParameters = new Arguments();

      defaultParameters.addArgument( PARAMETER_MSG_ROUTING_KEY, "routingKey");
      defaultParameters.addArgument( PARAMETER_MSG_PAYLOAD, "Hello World");

      defaultParameters.addArgument( PARAMETER_LOAD_BALANCER_SERVER, "localhost:7101");
      defaultParameters.addArgument( PARAMETER_CLIENT_TYPE, "sync");
      defaultParameters.addArgument( PARAMETER_ASYNC_BATCH_SIZE, "200");
      defaultParameters.addArgument( PARAMETER_ASYNC_TIMEOUT, "5000");
      defaultParameters.addArgument( PARAMETER_ASYNC_QUEUE_TYPE, "memory");
      defaultParameters.addArgument( PARAMETER_ASYNC_MEMORYQUEUE_CAPACITY, "10000");
      defaultParameters.addArgument( PARAMETER_ASYNC_FILEQUEUE_PATH, "/tmp/SuroClient" );

      return defaultParameters;
    }


    public SampleResult runTest(JavaSamplerContext context) {
        // create message batch
        Message msg = new Message( context.getParameter(PARAMETER_MSG_ROUTING_KEY),
                                   context.getParameter(PARAMETER_MSG_PAYLOAD).getBytes() );
        TMessageSet messageSet = new MessageSetBuilder(config)
                                  .withMessage( msg.getRoutingKey(), msg.getPayload() )
                                  .withCompression(compression)
                                  .build();
        // sent request
        boolean success = client.send( messageSet );

        // create result
        SampleResult result = new SampleResult();
        result.setSuccessful( success );
        return result;
    }
    

}