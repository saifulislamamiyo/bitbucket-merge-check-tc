package utils;

import com.wcg.bitbucket.merge.checks.IsAdminMergeCheck;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrapperMain {

    /*
* to get the queued builds for a branch and project, use:
* http://tc01.whiteclarke.com/TeamCity2/httpAuth/app/rest/builds?locator=state:queued,affectedProject:(id:Calms2cms_FeatureBugfix),branch:CMS-18048
*
*
     */
    private static final Logger log = LoggerFactory.getLogger(WrapperMain.class);
    private final String origin = "http://tc01.whiteclarke.com/TeamCity2/";
    private final String mockURL = "http://tc01.whiteclarke.com/TeamCity2/app/rest/server";

    // private String targetURLToCall =
    // "http://tc01.whiteclarke.com/TeamCity2/app/rest/builds?locator=state:any,affectedProject:(id:";
    // add 'webUrl' along with number, status and state. like this: build(number,status,state,webUrl)
    private String targetURLToCall = "http://tc01.whiteclarke.com/TeamCity2/app/rest/buildTypes?locator=affectedProject:(id:%s)&fields="
            + "buildType(id,name,builds($locator(state:any,failedToStart:any,count:1,branch:%s,lookupLimit:1000),build(number,status,state)))";
    private String queueQueryString = "http://tc01.whiteclarke.com/TeamCity2/app/rest/builds?locator=state:queued,"
            + "affectedProject:(id:%s),branch:%s";
    private String encoding = null;//"V0NHRE9NTUtcc2FpZnVsaTpBQkNfYWJjXzEyMw==";

    public WrapperMain(String projectID, String branchName, String headerEncoding) {
        this.targetURLToCall = String.format(this.targetURLToCall, projectID, branchName);
        this.queueQueryString = String.format(this.queueQueryString, projectID, branchName);
        this.encoding = headerEncoding;
    }

    public String callTeamCity(String urlToCall, boolean getCookie, String cookieValue) {
        String retVal = null;
        URL url = null;
        HttpURLConnection connection = null;
        InputStream content = null;
        BufferedReader in = null;

        try {
            url = new URL(urlToCall);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.addRequestProperty("origin", origin);
            if (getCookie) // getting the cookie, has to pass basic authentication header
            {
                connection.setRequestProperty("Authorization", "Basic " + encoding);
            } else {
                connection.setRequestProperty("Cookie", cookieValue);
            }

            if (getCookie) { // if we are going to get the cookie
                if (connection.getResponseCode() == 200) { // checking if we get a response '200 OK'
                    for (int i = 0;; i++) {
                        String headerName = connection.getHeaderFieldKey(i);
                        String headerValue = connection.getHeaderField(i);

                        if (headerName == null && headerValue == null) {
                            break;
                        }
                        if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                            String[] fields = headerValue.split(";\\s*");
                            for (String field : fields) {
                                if (field.indexOf('=') > 0) {
                                    String[] f = field.split("=");
                                    if ("JSESSIONID".equalsIgnoreCase(f[0])) {
                                        // System.out.println("JSESSIONID: " + f[1]);
                                        retVal = f[1];
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    retVal = "error:" + String.valueOf(connection.getResponseCode());
                }
            } else {// we made a request, let's get the response

                content = (InputStream) connection.getInputStream();
                in = new BufferedReader(new InputStreamReader(content));
                retVal = in.lines().collect(Collectors.joining());
            }

        } catch (IOException e) {
            retVal = "" + e;
            e.printStackTrace();
        } finally {
            connection = null;
            url = null;
            in = null;
            content = null;
        }

        return retVal;
    }

    public static void print(String toPrint) {
        ZonedDateTime zdt = ZonedDateTime.now();
        System.out.println(zdt.toLocalTime() + " : " + toPrint);
        log.debug(zdt.toLocalTime() + " : " + toPrint);
    }

    public boolean doAllowMerge(ArrayList<ModelBuild> results) {
        if (results.size() <= 0) {
            return false;
        }

        for (ModelBuild modelBuild : results) {
            if (!"finished".equalsIgnoreCase(modelBuild.getState())) {
                return false;
            } else {
                if (!"SUCCESS".equalsIgnoreCase(modelBuild.getStatus())) {
                    return false;
                }
            }
        }

        return true;
    }

    public String shouldMergeBranch() {
        String output = "OK";
        StringBuilder buffer = new StringBuilder();
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        long startA = System.currentTimeMillis();
        String sessionID = callTeamCity(this.mockURL, true, null);
        long endA = System.currentTimeMillis();
        print("Authentication query took: " + (endA - startA) / 1000 + "s");
//        print(sessionID);
        String rtData = null;
        if (sessionID != null) {
//            print("Trying with URL: " + this.targetURLToCall);
//now we can have response which is not null however can be not accepted reponse too. In such cases the response will be "error:<httpErrorCode>"
//check if the sessionID starts with error
            if (sessionID.startsWith("error")) { //started with Error
                output = sessionID.split(":")[1];
            } else {
                print("Saiful calling: " + this.targetURLToCall);
                long start = System.currentTimeMillis();
                rtData = callTeamCity(this.targetURLToCall, false, "JSESSIONID=" + sessionID);
                long end = System.currentTimeMillis();
                print("TC returns: " + rtData);
                print("Build query took: " + (end - start) / 1000 + "s");

                ArrayList<ModelBuild> results = null;
                BuildResultParser parser = new BuildResultParser();

                try {
                    results = parser.parseBuildReults(new InputStreamReader(new ByteArrayInputStream(rtData.getBytes()))); // TODO Auto-generated catch block
                } catch (Exception ex) {
                    output = "Exception";
                    ex.printStackTrace();
                } finally {
                    // parser.toString(results);
//                print("Should we go ahead and merge this pull request?: " + doAllowMerge(results));
                    if (results != null) {
                        if (results.size() <= 0) {
                            output = "no results from TC";
                        }
                    }

                    for (ModelBuild modelBuild : results) {
                        if (!"finished".equalsIgnoreCase(modelBuild.getState())) {
//                            buffer.append(modelBuild.toString());
                            buffer.append(modelBuild.getRunningStatus());
                            output = buffer.toString();
                        } else {
                            if (!"SUCCESS".equalsIgnoreCase(modelBuild.getStatus())) {
//                                buffer.append(modelBuild.toString());
                                buffer.append(modelBuild.getFailedStatus());
                                output = buffer.toString();
                            }
                        }
                    }

                    results = null;
                }
                //Here, all builds are okay, means output=OK, now we can proceed to do queue check
                //first check whether output is still "OK", if not we have failure already, return then
                if (output.equalsIgnoreCase("OK")) {
                    print("Saiful calling: " + this.queueQueryString);
                    long startQ = System.currentTimeMillis();
                    rtData = callTeamCity(this.queueQueryString, false, "JSESSIONID=" + sessionID);
                    long endQ = System.currentTimeMillis();
                    print("TC returns: " + rtData);
                    print("Queue query took: " + (endQ - startQ) / 1000 + "s");
                    String c = null;
                    try {
                        InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(rtData.getBytes()));
                        c = parser.parseQueueResults(r);
                    } catch (Exception ex) {
                        output = "" + ex;
                        ex.printStackTrace();
                    } finally {
                        if (c != null) {
                            if (Integer.parseInt(c) == 0) {
                                output = "OK";
                            } else {
//                                output = rtData;
                                output = "There are builds in queue";
                            }
                            parser = null;
                        }
                    }
                }
            }
        } else { // sessionID is null;
            output = "Exception";
        }
        return output;
    }

}
