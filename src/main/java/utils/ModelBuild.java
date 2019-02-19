package utils;

public class ModelBuild {

    private String buildName;
    private String revNumber;
    private String status;
    private String state;
    private String webUrl;

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getRevNumber() {
        return revNumber;
    }

    public void setRevNumber(String revNumber) {
        this.revNumber = revNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Build name: " + getBuildName() + " state: " + getState() + " status: " + getStatus() + "\r\n";
    }

    public String getRunningStatus() {
        return "Build " +getState()+" : "+ getBuildName() + "\r\n";
    }

    public String getFailedStatus() {
        return "Build "+getStatus()+" : " + getBuildName() + "\r\n";
    }
}
