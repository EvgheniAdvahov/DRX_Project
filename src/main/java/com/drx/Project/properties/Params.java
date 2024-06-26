package com.drx.Project.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "param")
@Data
public class Params {

    private String QUOTE_URL;
    private String UPLOAD_DIR_IMG;
}
