package utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.TaskData;

public class TaskDataLoader {

    public static List<TaskData> loadTaskData() {

        try {

            ObjectMapper mapper = new ObjectMapper();

            InputStream is = TaskDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream(
                            "testdata/task-data.json");

            if (is == null) {
                throw new IllegalStateException(
                        "Unable to find testdata/task-data.json");
            }

            return Arrays.asList(
                    mapper.readValue(
                            is,
                            TaskData[].class));

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}