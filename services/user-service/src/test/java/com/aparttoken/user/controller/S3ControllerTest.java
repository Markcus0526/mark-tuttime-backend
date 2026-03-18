package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.PresignedUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Controller Tests")
class S3ControllerTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Controller s3Controller;

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_FILE_KEY = "jsons/cities/CC.json";
    private static final String TEST_PRESIGNED_URL = "https://s3.example.com/test-bucket/jsons/cities/CC.json?signature=abc123";

    @BeforeEach
    void setUp() {
        // Inject test values using reflection
        ReflectionTestUtils.setField(s3Controller, "s3Endpoint", "https://s3.example.com");
        ReflectionTestUtils.setField(s3Controller, "s3Region", "us-east-1");
        ReflectionTestUtils.setField(s3Controller, "bucketName", TEST_BUCKET);
        ReflectionTestUtils.setField(s3Controller, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(s3Controller, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(s3Controller, "presignedUrlExpirationHours", 24L);
        
        // Inject mocked S3 clients
        ReflectionTestUtils.setField(s3Controller, "s3Client", s3Client);
        ReflectionTestUtils.setField(s3Controller, "s3Presigner", s3Presigner);
    }

    @Test
    @DisplayName("Should generate presigned URL successfully")
    void testGeneratePresignedUrl_Success() throws Exception {
        // Given
        HeadObjectResponse headResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL presignedUrl = new URL(TEST_PRESIGNED_URL);
        when(presignedRequest.url()).thenReturn(presignedUrl);
        
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // When
        ResponseEntity<ApiResponse<PresignedUrlResponse>> response = 
                s3Controller.generatePresignedUrl(TEST_FILE_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getFileKey()).isEqualTo(TEST_FILE_KEY);
        assertThat(response.getBody().getData().getPresignedUrl()).isEqualTo(TEST_PRESIGNED_URL);
        assertThat(response.getBody().getData().getExpirationHours()).isEqualTo(24L);
        
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when file does not exist")
    void testGeneratePresignedUrl_FileNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When/Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl(TEST_FILE_KEY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found: " + TEST_FILE_KEY);
        
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when S3 access fails")
    void testGeneratePresignedUrl_S3AccessFails_ThrowsRuntimeException() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access denied")
                        .statusCode(403)
                        .build());

        // When/Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl(TEST_FILE_KEY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to access S3 file");
        
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when presigning fails")
    void testGeneratePresignedUrl_PresigningFails_ThrowsRuntimeException() throws Exception {
        // Given
        HeadObjectResponse headResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
        
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("Presigning failed"));

        // When/Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl(TEST_FILE_KEY))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate pre-signed URL");
        
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when fileKey is null")
    void testGeneratePresignedUrl_NullFileKey_ReturnsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl(null))
                .isInstanceOf(com.tuttime.common.exception.ValidationException.class)
                .hasMessageContaining("File key is required");
        
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when fileKey is empty")
    void testGeneratePresignedUrl_EmptyFileKey_ReturnsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl(""))
                .isInstanceOf(com.tuttime.common.exception.ValidationException.class)
                .hasMessageContaining("File key is required");
        
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when fileKey is blank")
    void testGeneratePresignedUrl_BlankFileKey_ReturnsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> s3Controller.generatePresignedUrl("   "))
                .isInstanceOf(com.tuttime.common.exception.ValidationException.class)
                .hasMessageContaining("File key is required");
        
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("Should handle different file paths correctly")
    void testGeneratePresignedUrl_DifferentFilePaths_Success() throws Exception {
        // Given
        String[] testFileKeys = {
                "jsons/cities/CD.json",
                "users/123/avatar.jpg",
                "avatars/user-456.png"
        };

        HeadObjectResponse headResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL presignedUrl = new URL(TEST_PRESIGNED_URL);
        when(presignedRequest.url()).thenReturn(presignedUrl);
        
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // When/Then
        for (String fileKey : testFileKeys) {
            ResponseEntity<ApiResponse<PresignedUrlResponse>> response = 
                    s3Controller.generatePresignedUrl(fileKey);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSuccess()).isTrue();
            assertThat(response.getBody().getData().getFileKey()).isEqualTo(fileKey);
        }
        
        verify(s3Client, times(testFileKeys.length)).headObject(any(HeadObjectRequest.class));
        verify(s3Presigner, times(testFileKeys.length)).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
