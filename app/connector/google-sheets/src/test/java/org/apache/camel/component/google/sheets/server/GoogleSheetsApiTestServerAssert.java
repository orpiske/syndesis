/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.component.google.sheets.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.consol.citrus.message.MessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.syndesis.common.util.Json;
import org.apache.camel.util.ObjectHelper;
import org.assertj.core.api.AbstractAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * @author Christoph Deppisch
 */
public class GoogleSheetsApiTestServerAssert extends AbstractAssert<GoogleSheetsApiTestServerAssert, GoogleSheetsApiTestServer> {

    private GoogleSheetsApiTestServerAssert(GoogleSheetsApiTestServer server) {
        super(server, GoogleSheetsApiTestServerAssert.class);
    }

    /**
     * A fluent entry point to the assertion class.
     * @param server the target server to perform assertions to.
     * @return
     */
    public static GoogleSheetsApiTestServerAssert assertThatGoogleApi(GoogleSheetsApiTestServer server) {
        return new GoogleSheetsApiTestServerAssert(server);
    }

    public GetSpreadsheetAssert getSpreadsheetRequest(String spreadsheetId) {
        return new GetSpreadsheetAssert(spreadsheetId);
    }

    public class GetSpreadsheetAssert {
        GetSpreadsheetAssert(String spreadsheetId) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
        }

        public void andReturnSpreadsheet(Spreadsheet spreadsheet) throws IOException {
            String spreadsheetJson = spreadsheet.toPrettyString();
            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .get("/v4/spreadsheets/${spreadsheetId}")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload(spreadsheetJson))
            );
        }
    }

    public ClearValuesAssert clearValuesRequest(String spreadsheetId, String range) {
        return new ClearValuesAssert(spreadsheetId, range);
    }

    public class ClearValuesAssert {
        ClearValuesAssert(String spreadsheetId, String range) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("range", range);
        }

        public void andReturnClearResponse(String clearedRange) throws IOException {
            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .post("/v4/spreadsheets/${spreadsheetId}/values/${range}:clear")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload("{" +
                            "\"spreadsheetId\": \"${spreadsheetId}\"," +
                            "\"clearedRange\": \"" + clearedRange + "\"" +
                        "}"))
            );
        }
    }

    public UpdateValuesAssert updateValuesRequest(String spreadsheetId, String range, List<List<Object>> data) {
        return new UpdateValuesAssert(spreadsheetId, range, data);
    }

    public class UpdateValuesAssert {
        private final List<List<Object>> data;

        UpdateValuesAssert(String spreadsheetId, String range, List<List<Object>> data) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("range", range);
            this.data = data;
        }

        public void andReturnUpdateResponse() throws IOException {
            String valuesJson = Json.writer().writeValueAsString(data);

            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .put("/v4/spreadsheets/${spreadsheetId}/values/${range}")
                    .validate("$.values.toString()", valuesJson)),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload("{" +
                            "\"spreadsheetId\": \"${spreadsheetId}\"," +
                            "\"updatedRange\": \"${range}\"," +
                            "\"updatedRows\": " + data.size() + "," +
                            "\"updatedColumns\": " + Optional.ofNullable(data.get(0)).map(Collection::size).orElse(0) + "," +
                            "\"updatedCells\": " + data.size() * Optional.ofNullable(data.get(0)).map(Collection::size).orElse(0) +
                        "}"))
            );
        }
    }

    public AppendValuesAssert appendValuesRequest(String spreadsheetId, String range, List<List<Object>> data) {
        return new AppendValuesAssert(spreadsheetId, range, data);
    }

    public class AppendValuesAssert {
        private final List<List<Object>> data;

        AppendValuesAssert(String spreadsheetId, String range, List<List<Object>> data) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("range", range);
            this.data = data;
        }

        public void andReturnAppendResponse(String updatedRange) throws IOException {
            String valuesJson = Json.writer().writeValueAsString(data);

            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .post("/v4/spreadsheets/${spreadsheetId}/values/${range}:append")
                    .validate("$.values.toString()", valuesJson)),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload("{" +
                        "\"spreadsheetId\": \"${spreadsheetId}\"," +
                        "\"updates\":" +
                            "{" +
                                "\"spreadsheetId\": \"${spreadsheetId}\"," +
                                "\"updatedRange\": \"" + updatedRange + "\"," +
                                "\"updatedRows\": " + data.size() + "," +
                                "\"updatedColumns\": " + Optional.ofNullable(data.get(0)).map(Collection::size).orElse(0) + "," +
                                "\"updatedCells\": " + data.size() * Optional.ofNullable(data.get(0)).map(Collection::size).orElse(0) +
                            "}" +
                        "}"))
            );
        }
    }

    public GetValuesAssert getValuesRequest(String spreadsheetId, String range) {
        return new GetValuesAssert(spreadsheetId, range);
    }

    public class GetValuesAssert {
        GetValuesAssert(String spreadsheetId, String range) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("range", range);
        }

        public void andReturnValueRange(ValueRange valueRange) throws IOException {
            String valueJson = valueRange.toPrettyString();
            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .get("/v4/spreadsheets/${spreadsheetId}/values/${range}")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload(valueJson))
            );
        }

        public void andReturnValues(List<List<Object>> data) throws JsonProcessingException {
            String valueRangeJson;
            if (ObjectHelper.isEmpty(data)) {
                valueRangeJson = "{" +
                        "\"range\": \"${range}\"," +
                        "\"majorDimension\": \"ROWS\"" +
                    "}";
            } else {
                valueRangeJson = "{" +
                        "\"range\": \"${range}\"," +
                        "\"majorDimension\": \"ROWS\"," +
                        "\"values\":" + Json.writer().writeValueAsString(data) +
                    "}";
            }

            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .get("/v4/spreadsheets/${spreadsheetId}/values/${range}")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload(valueRangeJson))
            );
        }
    }

    public BatchGetValuesAssert batchGetValuesRequest(String spreadsheetId, String range) {
        return new BatchGetValuesAssert(spreadsheetId, range);
    }

    public class BatchGetValuesAssert {
        BatchGetValuesAssert(String spreadsheetId, String range) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("range", range);
        }

        public void andReturnValues(List<List<Object>> data) throws JsonProcessingException {
            String valueRangeJson;
            if (ObjectHelper.isEmpty(data)) {
                valueRangeJson = "{\"spreadsheetId\": \"${spreadsheetId}\"," +
                    "\"valueRanges\": [" +
                        "{" +
                            "\"range\": \"${range}\"," +
                            "\"majorDimension\": \"ROWS\"" +
                        "}" +
                    "]}";
            } else {
                valueRangeJson = "{\"spreadsheetId\": \"${spreadsheetId}\"," +
                    "\"valueRanges\": [" +
                        "{" +
                            "\"range\": \"${range}\"," +
                            "\"majorDimension\": \"ROWS\"," +
                            "\"values\":" + Json.writer().writeValueAsString(data) +
                        "}" +
                    "]}";
            }

            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .get("/v4/spreadsheets/${spreadsheetId}/values:batchGet")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload(valueRangeJson))
            );
        }
    }

    public CreateSpreadsheetAssert createSpreadsheetRequest() {
        return new CreateSpreadsheetAssert();
    }

    public class CreateSpreadsheetAssert {
        private String title = "@ignore@";
        private String sheetTitle;

        public CreateSpreadsheetAssert hasTitle(String title) {
            this.title = title;
            return this;
        }

        public CreateSpreadsheetAssert hasSheetTitle(String sheetTitle) {
            this.sheetTitle = sheetTitle;
            return this;
        }

        public void andReturnRandomSpreadsheet() {
            andReturnSpreadsheet("citrus:randomString(44)");
        }

        public void andReturnSpreadsheet(String spreadsheetId) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
            actual.getRunner().createVariable("title", title);

            String spreadsheetJson;
            if (ObjectHelper.isNotEmpty(sheetTitle)) {
                actual.getRunner().createVariable("sheetTitle", sheetTitle);
                spreadsheetJson = "{\"properties\":{\"title\":\"${title}\"},\"sheets\":[{\"properties\":{\"title\":\"${sheetTitle}\"}}]}";
            } else {
                spreadsheetJson = "{\"properties\":{\"title\":\"${title}\"}}";
            }

            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .post("/v4/spreadsheets")
                    .name("create.request")
                    .messageType(MessageType.JSON)
                    .payload(spreadsheetJson)),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload("{\"spreadsheetId\":\"${spreadsheetId}\",\"properties\":{\"title\":\"citrus:jsonPath(citrus:message(create.request.payload()), '$.properties.title')\"}}"))
            );
        }
    }

    public BatchUpdateSpreadsheetAssert batchUpdateSpreadsheetRequest(String spreadsheetId) {
        return new BatchUpdateSpreadsheetAssert(spreadsheetId);
    }

    public class BatchUpdateSpreadsheetAssert {
        private List<String> fields = new ArrayList<>();

        BatchUpdateSpreadsheetAssert(String spreadsheetId) {
            actual.getRunner().createVariable("spreadsheetId", spreadsheetId);
        }

        public BatchUpdateSpreadsheetAssert updateTitle(String title) {
            actual.getRunner().createVariable("title", title);
            fields.add("title");
            return this;
        }

        public void andReturnUpdated() {
            actual.getRunner().async().actions(
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .receive()
                    .post("/v4/spreadsheets/${spreadsheetId}:batchUpdate")
                    .messageType(MessageType.JSON)
                    .payload("{" +
                        "\"includeSpreadsheetInResponse\":true," +
                        "\"requests\":[" +
                                "{" +
                                    "\"updateSpreadsheetProperties\": {" +
                                    "\"fields\":\"" + String.join(",", fields) + "\"," +
                                    "\"properties\":{" + fields.stream().map(field -> String.format("\"%s\":\"${%s}\"", field, field)).collect(Collectors.joining(",")) + "}" +
                                "}" +
                            "}" +
                        "]}")),
                actual.getRunner().http(action -> action.server(actual.getHttpServer())
                    .send()
                    .response(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .payload("{\"spreadsheetId\":\"${spreadsheetId}\",\"updatedSpreadsheet\":{\"properties\":{\"title\":\"${title}\"},\"spreadsheetId\":\"${spreadsheetId}\"}}"))
            );
        }
    }
}