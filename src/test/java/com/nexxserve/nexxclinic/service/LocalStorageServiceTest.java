package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void movesVisitArchivePrefixAndPreservesFilesAndDirectories() throws Exception {
        SupabaseProperties properties = new SupabaseProperties();
        properties.setLocalStoragePath(temporaryDirectory.toString());
        properties.setBucketInvoices("clinic-files");
        LocalStorageService storage = new LocalStorageService(properties);

        String oldPrefix = "clinic-clinic/patient-patient_Jane_Doe_2501234567_2025-02-03_1/visit-2025-02-03_visit";
        String newPrefix = "clinic-clinic/patient-patient_Jane_Doe_2501234567_2025-02-04_1/visit-2025-02-04_visit";
        byte[] pdf = new byte[] { 1, 2, 3 };
        storage.ensureDirectory("clinic-files", oldPrefix + "/departments/Cardiology/consultations");
        storage.upload(pdf, "clinic-files", oldPrefix + "/departments/Cardiology/invoices/invoice.pdf", "application/pdf");
        assertArrayEquals(pdf,
            storage.download("clinic-files", oldPrefix + "/departments/Cardiology/invoices/invoice.pdf"));

        String oldPatientPrefix = oldPrefix.substring(0, oldPrefix.lastIndexOf("/visit-"));
        String newPatientPrefix = newPrefix.substring(0, newPrefix.lastIndexOf("/visit-"));
        storage.movePrefix("clinic-files", oldPatientPrefix, newPatientPrefix);
        storage.movePrefix(
            "clinic-files",
            newPatientPrefix + oldPrefix.substring(oldPatientPrefix.length()),
            newPrefix
        );

        Path movedPdf = temporaryDirectory.resolve("clinic-files")
            .resolve(newPrefix + "/departments/Cardiology/invoices/invoice.pdf");
        assertArrayEquals(pdf, Files.readAllBytes(movedPdf));
        assertTrue(Files.isDirectory(temporaryDirectory.resolve("clinic-files")
            .resolve(newPrefix + "/departments/Cardiology/consultations")));
        assertFalse(Files.exists(temporaryDirectory.resolve("clinic-files")
            .resolve(oldPrefix.substring(0, oldPrefix.lastIndexOf("/visit-")))));
    }

    @Test
    void rejectsPathsThatEscapeTheStorageRoot() {
        SupabaseProperties properties = new SupabaseProperties();
        properties.setLocalStoragePath(temporaryDirectory.toString());
        LocalStorageService storage = new LocalStorageService(properties);

        assertThrows(IOException.class, () -> storage.ensureDirectory("uploads-private", "../outside"));
        assertThrows(IOException.class,
            () -> storage.upload(new byte[] { 1 }, "../outside", "file.pdf", "application/pdf"));
    }
}
