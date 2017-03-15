package yucl.learn.demo.fs.cfg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author yucl80@163.com
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    public String getFileUploadUri() {
        return fileUploadUri;
    }

    public void setFileUploadUri(String fileUploadUri) {
        this.fileUploadUri = fileUploadUri;
    }

    private String fileUploadUri;

    public String getFileDownloadUri() {
        return fileDownloadUri;
    }

    public void setFileDownloadUri(String fileDownloadUri) {
        this.fileDownloadUri = fileDownloadUri;
    }

    private String fileDownloadUri;


}
