package co.signal.surometer;

import java.util.Properties;

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

    public SampleResult runTest(JavaSamplerContext arg0) {
        // TODO: do not allocate a new SuroClient for each request
        final Properties clientProperties = new Properties();
        clientProperties.setProperty(ClientConfig.LB_TYPE, "static");
        clientProperties.setProperty(ClientConfig.LB_SERVER, PARAMETER_LOAD_BALANCER_SERVER);

        SuroClient client = new SuroClient(clientProperties);
        // TODO: send configurable payload
        client.send(new Message("routingKey", "testMessage".getBytes()));
        client.shutdown();

        // create result
        // TODO: create a meaningful result
        SampleResult result = new SampleResult();
        result.setSuccessful(true);
        return result;
    }
    

}