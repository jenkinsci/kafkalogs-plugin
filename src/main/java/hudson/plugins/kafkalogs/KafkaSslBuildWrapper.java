/**
 * 
 */
package hudson.plugins.kafkalogs;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;

/**
 * @author Guneet Bhatia - 000V4T744
 *
 */
public class KafkaSslBuildWrapper extends SimpleBuildWrapper implements Serializable{
    private static final Logger LOGGER = Logger.getLogger(KafkaSslBuildWrapper.class.getName());

    private final String kafkaServers;
    private final String kafkaTopic;
    private final String metadata;
    private KafkaWrapper producer;
    private int buildId;
    private String jobName;
    private final String securityProtocol;
    private final String sslTruststoreLocation;
    private final String sslTruststorePassword;

    private static class CleanupDisposer extends Disposer {
        private KafkaWrapper producer;
        public CleanupDisposer(KafkaWrapper producer) {
            this.producer = producer;
        }

        @Override
        public void tearDown(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
            // DO ANYTHING TO TEAR DOWN KAFKA LOGGING
            if(this.producer != null) {
                this.producer.flush();
                this.producer.close();
                this.producer = null;
            }
        }
    }

    /**
     * Create a new {@link KafkaSslBuildWrapper}.
     */
    @DataBoundConstructor
    public KafkaSslBuildWrapper(String kafkaServers, String kafkaTopic, String metadata, String securityProtocol, String sslTruststoreLocation,
    		String sslTruststorePassword) {
        this.kafkaServers = kafkaServers;
        this.kafkaTopic = kafkaTopic;
        this.metadata = metadata;
        this.securityProtocol = securityProtocol;
        this.sslTruststoreLocation = sslTruststoreLocation;
        this.sslTruststorePassword= sslTruststorePassword;
    }
    
    public String getKafkaServers() {
        return this.kafkaServers == null ? "127.0.0.1:9092" : this.kafkaServers;
    }

    public String getKafkaTopic() {
        return this.kafkaTopic == null ? "buildlogs" : this.kafkaTopic;
    }


    public String getMetadata() {
        return this.kafkaTopic == null ? "" : this.metadata;
    }
    
    public String getSecurityProtocol() {
		return this.securityProtocol;
	}

	public String getSslTruststoreLocation() {
		return this.sslTruststoreLocation;
	}

	public String getSslTruststorePassword() {
		return this.sslTruststorePassword;
	}

	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) 
        throws IOException, InterruptedException 
    {
        this.buildId = build.getNumber();
        this.jobName = build.getParent().getName();
        // DO ANYTHING TO SETUP
        this.producer = new KafkaWrapper(this.buildId, this.jobName, this.metadata, this.kafkaServers, this.kafkaTopic,
        		this.securityProtocol, this.sslTruststoreLocation, this.sslTruststorePassword);
        context.setDisposer(new CleanupDisposer(this.producer));
    }


    /**
     * Registers {@link KafkaBuildWrapper} as a {@link BuildWrapper}.
     */
    @Extension @Symbol("withKafkaLog")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(KafkaSslBuildWrapper.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            return true;
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckName(@QueryParameter final String value) {
            return FormValidation.ok();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
    }



    @Override
    public ConsoleLogFilter createLoggerDecorator(Run<?, ?> build) {
        return new KafkaConsoleLogFilter(this.producer);
    }

	
}
