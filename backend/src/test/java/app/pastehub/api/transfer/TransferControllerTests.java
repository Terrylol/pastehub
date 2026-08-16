package app.pastehub.api.transfer;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import app.pastehub.api.image.ImageTransferService;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImageTransferService imageTransferService;

    @MockitoBean
    private S3Client s3;

    @MockitoBean
    private S3Presigner presigner;

    @BeforeEach
    void clearTransfers() throws Exception {
        jdbcTemplate.update("DELETE FROM transfers");
        jdbcTemplate.update("DELETE FROM image_transfers");
        PresignedPutObjectRequest put = mock(PresignedPutObjectRequest.class);
        when(put.url()).thenReturn(new URL("http://localhost:8333/pastehub/test-upload"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(put);
        PresignedGetObjectRequest get = mock(PresignedGetObjectRequest.class);
        when(get.url()).thenReturn(new URL("http://localhost:8333/pastehub/test-read"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(get);
    }

    @Test
    void createsAndRetrievesTextUsingIdentifierAndCode() throws Exception {
        JsonNode created = create("从旧电脑发送的命令");
        String id = created.get("id").asText();
        String code = created.get("code").asText();

        mockMvc.perform(get("/api/v1/transfers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(jsonPath("$.content").value("从旧电脑发送的命令"));

        mockMvc.perform(get("/api/v1/transfers/code/{code}", "  " + code.toLowerCase() + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void returnsCreatorTokenOnlyWhenCreatingAndAllowsIdempotentDeletion() throws Exception {
        JsonNode created = create("请在取件后删除");
        String id = created.get("id").asText();
        String token = created.get("deleteToken").asText();

        mockMvc.perform(delete("/api/v1/transfers/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/transfers/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/transfers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_UNAVAILABLE"));
    }

    @Test
    void rejectsInvalidInputAndInvalidDeletionAuthority() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/text").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/transfers/text").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + "a".repeat(10_001) + "\"}"))
                .andExpect(status().isBadRequest());

        JsonNode created = create("保留这条内容");
        mockMvc.perform(delete("/api/v1/transfers/{id}", created.get("id").asText())
                .header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/transfers/{id}", created.get("id").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void makesExpiredTransfersUnavailableBeforeScheduledCleanupRuns() throws Exception {
        JsonNode created = create("即将过期");
        String id = created.get("id").asText();
        jdbcTemplate.update("UPDATE transfers SET expires_at = ? WHERE id = ?", Instant.now().minusSeconds(1), id);

        mockMvc.perform(get("/api/v1/transfers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_UNAVAILABLE"));
        mockMvc.perform(get("/api/v1/transfers/code/{code}", created.get("code").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_UNAVAILABLE"));
    }

    @Test
    void validatesImageInputAndKeepsPendingImagesUnavailable() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/image/init").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mimeType\":\"image/gif\",\"sizeBytes\":70}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/transfers/image/init").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mimeType\":\"image/png\",\"sizeBytes\":10485761}"))
                .andExpect(status().isBadRequest());

        JsonNode pending = initImage();
        mockMvc.perform(get("/api/v1/transfers/image/{id}", pending.get("id").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_UNAVAILABLE"));
    }

    @Test
    void publishesImageThenDeletesItsObjectAndMakesItUnavailable() throws Exception {
        JsonNode pending = initImage();
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                .contentLength(70L).contentType("image/png").build());
        MvcResult complete = mockMvc.perform(post("/api/v1/transfers/image/complete")
                        .header("Origin", "http://localhost:5173").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + pending.get("id").asText() + "\",\"uploadToken\":\"" + pending.get("uploadToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(matchesPattern("[A-Z2-9]{6}")))
                .andReturn();
        JsonNode created = objectMapper.readTree(complete.getResponse().getContentAsString());
        String id = created.get("id").asText();
        mockMvc.perform(get("/api/v1/transfers/image/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mimeType").value("image/png"));
        mockMvc.perform(delete("/api/v1/transfers/image/{id}", id)
                        .header("Authorization", "Bearer " + created.get("deleteToken").asText()))
                .andExpect(status().isNoContent());
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
        mockMvc.perform(get("/api/v1/transfers/image/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void removesExpiredImageObjectsDuringCleanup() throws Exception {
        JsonNode pending = initImage();
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                .contentLength(70L).contentType("image/png").build());
        mockMvc.perform(post("/api/v1/transfers/image/complete").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + pending.get("id").asText() + "\",\"uploadToken\":\"" + pending.get("uploadToken").asText() + "\"}"))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE image_transfers SET expires_at = ? WHERE id = ?", Instant.now().minusSeconds(1), pending.get("id").asText());
        imageTransferService.cleanup();
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
        mockMvc.perform(get("/api/v1/transfers/image/{id}", pending.get("id").asText())).andExpect(status().isNotFound());
    }

    private JsonNode create(String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transfers/text")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRequest(content))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(jsonPath("$.id").value(matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.code").value(matchesPattern("[A-Z2-9]{6}")))
                .andExpect(jsonPath("$.pickupUrl").value(matchesPattern("http://localhost:5173/t/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.deleteToken").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode initImage() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transfers/image/init")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mimeType\":\"image/png\",\"sizeBytes\":70}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.uploadToken").isNotEmpty()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record CreateRequest(String content) { }
}
