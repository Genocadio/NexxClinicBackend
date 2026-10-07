package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinioStorageServiceTest {

    @Test
    void movePrefixCopiesObjectsThenDeletesSourcesServerSide() throws Exception {
        SupabaseProperties properties = new SupabaseProperties();
        MinioClient client = mock(MinioClient.class);
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        @SuppressWarnings("unchecked")
        Result<Item> result = mock(Result.class);
        Item item = mock(Item.class);
        when(result.get()).thenReturn(item);
        when(item.objectName()).thenReturn("clinic/a/visit-old/invoices/invoice.pdf");
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(result));

        MinioStorageService storage = new MinioStorageService(properties, client, client);
        storage.movePrefix("clinic-files", "clinic/a/visit-old", "clinic/a/visit-new");

        verify(client).copyObject(any(CopyObjectArgs.class));
        verify(client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void signedUrlRejectsExpiryOutsideS3PresigningLimit() {
        SupabaseProperties properties = new SupabaseProperties();
        MinioClient client = mock(MinioClient.class);
        MinioStorageService storage = new MinioStorageService(properties, client, client);

        assertThrows(IOException.class,
            () -> storage.getSignedUrl("clinic-files", "patient/visit/invoice.pdf", 7 * 24 * 60 * 60 + 1));
    }
}
