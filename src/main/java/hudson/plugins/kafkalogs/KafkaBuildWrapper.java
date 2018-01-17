/*
 * The MIT License
 *
 * Copyright (c) 2018 OC Tanner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.kafkalogs;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;

import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Build wrapper that decorates the build's logger
 *
 * @author Trevor Linton
 */
@SuppressWarnings("unused")
public final class KafkaBuildWrapper extends SimpleBuildWrapper implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(KafkaBuildWrapper.class.getName());

    private final String kafkaServers;
    private final String kafkaTopic;
    private KafkaWrapper producer;
    private int build_id;
    private String job_name;

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
     * Create a new {@link KafkaBuildWrapper}.
     */
    @DataBoundConstructor
    public KafkaBuildWrapper(String kafkaServers, String kafkaTopic) {
        this.kafkaServers = kafkaServers;
        this.kafkaTopic = kafkaTopic;
    }

    public String getKafkaServers() {
        return this.kafkaServers == null ? "127.0.0.1:9092" : this.kafkaServers;
    }

    public String getKafkaTopic() {
        return this.kafkaTopic == null ? "buildlogs" : this.kafkaTopic;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) 
        throws IOException, InterruptedException 
    {
        this.build_id = build_id = build.getNumber();
        this.job_name = build.getParent().getName();
        // DO ANYTHING TO SETUP
        this.producer = new KafkaWrapper(this.build_id, this.job_name, this.kafkaServers, this.kafkaTopic);
        context.setDisposer(new CleanupDisposer(this.producer));
    }


    /**
     * Registers {@link KafkaBuildWrapper} as a {@link BuildWrapper}.
     */
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(KafkaBuildWrapper.class);
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
