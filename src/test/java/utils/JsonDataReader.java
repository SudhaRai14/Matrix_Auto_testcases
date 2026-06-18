package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.TaskData;

import java.io.File;
import java.util.List;

public class JsonDataReader {

    public static List<TaskData> getTaskData() {

        try {

            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(
                    new File("src/test/resources/testdata/task-data.json"),
                    new TypeReference<List<TaskData>>() {
                    });

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to read task-data.json", e);
        }
    }
}
