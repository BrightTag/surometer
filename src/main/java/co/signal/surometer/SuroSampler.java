package co.signal.surometer;

import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Priority;

import com.google.inject.Injector;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.suro.ClientConfig;
import com.netflix.suro.client.SuroClient;
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
     * Parameter for setting the Suro servers; it should be comma separated list of $hostname:$port".
     */
    private static final String PARAMETER_CLIENT_TYPE = "SuroClient.clientType";
    private static final String PARAMETER_MSG_COUNT = "SuroSampler.MsgCount";

    private SyncSuroClient syncClient;
    private SuroClient asyncClient;

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
        createInjector( clientProperties );
        
        // setup the injection engin
        this.config = injector.getInstance(ClientConfig.class);
        this.compression = Compression.create(config.getCompression());

        // setup the Suro client
        syncClient = injector.getInstance(SyncSuroClient.class);
        asyncClient = new SuroClient( clientProperties );
        getLogger().log( Priority.DEBUG, "setup SuroClient" );
    }


    @Override
    public void teardownTest( JavaSamplerContext context ) {
        // shutdown the injector
        injector.getInstance(LifecycleManager.class).close();
        getLogger().log( Priority.DEBUG, "shutdown SuroClient" );
    }


    @Override
    public Arguments getDefaultParameters() {
      Arguments defaultParameters = new Arguments();

      defaultParameters.addArgument( PARAMETER_MSG_ROUTING_KEY, "routingKey");
      defaultParameters.addArgument( PARAMETER_MSG_PAYLOAD, "Hello World");

      defaultParameters.addArgument( PARAMETER_LOAD_BALANCER_SERVER, "localhost:7101");
      defaultParameters.addArgument( PARAMETER_CLIENT_TYPE, "sync");
      defaultParameters.addArgument( PARAMETER_MSG_COUNT, "1");

      return defaultParameters;
    }


    public SampleResult runTest(JavaSamplerContext context) {
        // create result
        SampleResult result = new SampleResult();

        // create message batch
        Message msg = new Message( context.getParameter(PARAMETER_MSG_ROUTING_KEY),
                                   context.getParameter(PARAMETER_MSG_PAYLOAD).getBytes() );
        MessageSetBuilder builder = new MessageSetBuilder(config);
        for( int i=0; i<Long.parseLong(context.getParameter(PARAMETER_MSG_COUNT)); i++ ){
            builder.withMessage( msg.getRoutingKey(), msg.getPayload() );
        }
        TMessageSet messageSet = builder.withCompression(compression).build();

        // sent request
        if( context.getParameter( PARAMETER_CLIENT_TYPE ).equals("sync")){
            // send synchronously
            boolean success = syncClient.send( messageSet );
            // set result status
            result.setSuccessful( success );
        }else{
            // send asynchronously
            asyncClient.send( msg );
        }
        return result;


    }
    

}