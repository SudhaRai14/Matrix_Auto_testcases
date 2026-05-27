package utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.TemplateData;

public class TemplateDataLoader {

    public static List<TemplateData> loadTemplateData() {

        try {

            ObjectMapper mapper = new ObjectMapper();

            InputStream is = TemplateDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream(
                            "testdata/template-data.json");

            if (is == null) {
                throw new IllegalStateException("Unable to find testdata/template-data.json");
            }

            return Arrays.asList(
                    mapper.readValue(
                            is,
                            TemplateData[].class));

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
