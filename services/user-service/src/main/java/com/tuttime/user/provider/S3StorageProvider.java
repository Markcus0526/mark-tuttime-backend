package com.tuttime.user.provider;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.media.storage.type", havingValue = "s3")
public class S3StorageProvider implements StorageProvider {

	private final S3Client s3Client;
	@Value("${app.media.s3.bucket}")
	private String bucket;
	@Value("${app.media.s3.region}")
	private String region;
	@Value("${app.media.max-dimension}")
	private int maxDim;
	@Value("${app.media.quality}")
	private float quality;

	@Override
	public String store(MultipartFile file) {
		String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
		String key = UUID.randomUUID() + (extension != null ? "." + extension : "");

		try {
			if (isImage(file.getContentType())) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				Thumbnails.of(file.getInputStream()).size(maxDim, maxDim)
						.outputQuality(quality).toOutputStream(os);
				byte[] bytes = os.toByteArray();

				s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
						.contentType(file.getContentType()).build(), RequestBody.fromBytes(bytes));
			} else {
				s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
								.contentType(file.getContentType()).build(),
						RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
			}
			return key;
		} catch (IOException e) {
			throw new RuntimeException("S3 Upload Failed", e);
		}
	}

	@Override
	public String getUrl(String key) {
		// Generates the public S3 URL
		return String.format("https://%s.s3.%://amazonaws.com", bucket, region, key);
	}

	@Override
	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
	}

	private boolean isImage(String type) {
		return type != null && type.startsWith("image/");
	}
}
