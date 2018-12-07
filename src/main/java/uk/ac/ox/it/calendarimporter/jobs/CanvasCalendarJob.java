package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import java.io.IOException;

public abstract class CanvasCalendarJob implements InterruptableJob {

    protected String context;
    protected String url;
    protected CanvasApiFactory canvasApiFactory;
    protected NonRefreshableOauthToken nonRefreshableOauthToken;

    private String canvasUrl;
    private String token;
    private boolean run = true;

    public void setContext(String context) {
        this.context = context;
    }

    public void setCanvasUrl(String canvasUrl) {
        this.canvasUrl = canvasUrl;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void interrupt() {
        run = false;
    }

    protected boolean isInterrupted() {
        return !run;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap config = context.getMergedJobDataMap();
        setUrl(config.getString("url"));
        setCanvasUrl(config.getString("canvas_url"));
        setToken(config.getString("token"));
        setContext(config.getString("context"));

        canvasApiFactory = new CanvasApiFactory(canvasUrl);
        nonRefreshableOauthToken = new NonRefreshableOauthToken(token);

        try {
            run();
        } catch (IOException  e) {
            throw new JobExecutionException(e);
        }
        // TODO Set result object
    }

    public abstract void run() throws IOException, JobExecutionException;

}
