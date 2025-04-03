package at.ac.tuwien.ifs.dbrepo.oaipmh;

import org.apache.commons.lang3.StringUtils;

public abstract class RequestParameters {

    public abstract String getParametersString();

    protected void appendIfNotEmpty(StringBuilder builder, String name, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.append(name).append("=").append("\"").append(value).append("\" ");
        }
    }

}
