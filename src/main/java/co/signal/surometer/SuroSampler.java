package co.signal.surometer;

import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.netflix.suro.ClientConfig;
import com.netflix.suro.client.SuroClient;
import com.netflix.suro.message.Message;

/**
 * A {@link org.apache.jmeter.samplers.Sampler Sampler} which produces Suro messages.
 * 
 * @author starzia
 */
public class SuroSampler extends AbstractJavaSamplerClient {

    /**
     * Parameter for setting the Suro servers; it should be comma separated list of $hostname:$port".
     */
    private static final String PARAMETER_LOAD_BALANCER_SERVER = "SuroClient.loadBalancerServer";

    private SuroClient client;


    @Override
    public void setupTest(JavaSamplerContext context) {
        // setup the SuroClient
        final Properties clientProperties = new Properties();
        clientProperties.setProperty( ClientConfig.LB_TYPE, "static" );
        clientProperties.setProperty( ClientConfig.LB_SERVER, 
                                      context.getParameter( PARAMETER_LOAD_BALANCER_SERVER ) );
        // requests are synchronous to allow SampleResult() to get the timing and response
        clientProperties.setProperty( ClientConfig.CLIENT_TYPE, "sync" );
        client = new SuroClient(clientProperties);
    }


    @Override
    public void teardownTest( JavaSamplerContext context ) {
        // shutdown the SuroClient
        client.shutdown();
    }


    @Override
    public Arguments getDefaultParameters() {
      Arguments defaultParameters = new Arguments();
      defaultParameters.addArgument( PARAMETER_LOAD_BALANCER_SERVER, "localhost:7101");
      return defaultParameters;
    }


    public SampleResult runTest(JavaSamplerContext context) {
        // TODO: send configurable routingKey and payload
        client.send( new Message("routingKey", "testMessage".getBytes()) );

        // create result
        // !!!: I am not aware of any error reporting mechanism for SuroClient.send()
        SampleResult result = new SampleResult();
        result.setSuccessful(true);
        return result;
    }
    

}