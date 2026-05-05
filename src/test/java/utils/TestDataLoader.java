package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.FrequencyData;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class TestDataLoader {

    public static List<FrequencyData> loadFrequencyData() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = TestDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream("testdata/frequency-data.json");

            return Arrays.asList(mapper.readValue(is, FrequencyData[].class));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}