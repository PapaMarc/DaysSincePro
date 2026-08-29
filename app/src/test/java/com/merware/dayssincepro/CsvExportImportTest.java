package com.merware.dayssincepro;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying RFC-4180 CSV parsing, serialization, quoting, header detection,
 * date validation, and legacy format recovery in CsvExporter and CsvImporter.
 */
public class CsvExportImportTest {

    @Test
    public void testEscapeFieldRfc4180() {
        assertEquals("\"\"", CsvExporter.escapeField(null));
        assertEquals("\"hello\"", CsvExporter.escapeField("hello"));
        assertEquals("\"hello, world\"", CsvExporter.escapeField("hello, world"));
        assertEquals("\"Say \"\"Hello\"\"\"", CsvExporter.escapeField("Say \"Hello\""));
        assertEquals("\"Tags Papa (53Chev/HD)\"", CsvExporter.escapeField("Tags Papa (53Chev/HD)"));
    }

    @Test
    public void testFormatRowRfc4180() {
        String row = CsvExporter.formatRow("Bills", "2024-01-15", "30");
        assertEquals("\"Bills\",\"2024-01-15\",\"30\"", row);

        String multiCatRow = CsvExporter.formatRow("Finance", "Taxes, State & Fed", "2024-04-15", "365");
        assertEquals("\"Finance\",\"Taxes, State & Fed\",\"2024-04-15\",\"365\"", multiCatRow);
    }

    @Test
    public void testFormatIsoDate() {
        assertEquals("2024-05-01", CsvExporter.formatIsoDate("2024-05-01"));
        assertEquals("", CsvExporter.formatIsoDate(null));
        assertEquals("", CsvExporter.formatIsoDate("  "));
    }

    @Test
    public void testParseAndFormatIsoDate() {
        assertEquals("2019-05-01", CsvImporter.parseAndFormatIsoDate("2019-05-01"));
        assertEquals("2023-12-31", CsvImporter.parseAndFormatIsoDate("2023-12-31"));
        assertEquals("2022-07-04", CsvImporter.parseAndFormatIsoDate("2022/07/04"));
        assertEquals("2021-09-15", CsvImporter.parseAndFormatIsoDate("09/15/2021"));
        assertNull(CsvImporter.parseAndFormatIsoDate("invalid-date"));
        assertNull(CsvImporter.parseAndFormatIsoDate("2019-13-45"));
        assertNull(CsvImporter.parseAndFormatIsoDate(""));
        assertNull(CsvImporter.parseAndFormatIsoDate(null));
    }

    @Test
    public void testCleanFieldLegacySingleQuotes() {
        assertEquals("KirPaint Annual Report Due", CsvImporter.cleanField("'KirPaint Annual Report Due'"));
        assertEquals("2019-05-01", CsvImporter.cleanField("'2019-05-01'"));
        assertEquals("Tags BigMama (97VWEVC  99BMW323ic)", CsvImporter.cleanField("Tags BigMama (97VWEVC  99BMW323ic)'"));
        assertEquals("Papa's Truck", CsvImporter.cleanField("Papa's Truck"));
        assertEquals("Standard Text", CsvImporter.cleanField("Standard Text"));
    }

    @Test
    public void testHeaderRowDetection() {
        assertTrue(CsvImporter.isHeaderRow(Arrays.asList("event", "date", "recur")));
        assertTrue(CsvImporter.isHeaderRow(Arrays.asList("Category", "Event", "Date", "Recur")));
        assertTrue(CsvImporter.isHeaderRow(Arrays.asList("category_name", "title", "due_date", "recurrence")));
        assertFalse(CsvImporter.isHeaderRow(Arrays.asList("KirPaint Annual Report Due", "2019-05-01", "365")));
        assertFalse(CsvImporter.isHeaderRow(Arrays.asList("Oil Change", "2024-01-01")));
    }

    @Test
    public void testParseStandardRfc4180Csv() throws IOException {
        String csv = "\"event\",\"date\",\"recur\"\r\n" +
                "\"Dentist Visit\",\"2024-06-01\",\"180\"\r\n" +
                "\"Oil change, filter & lube\",\"2024-07-15\",\"90\"\r\n" +
                "\"Quote test: \"\"Important\"\"\",\"2024-08-01\",\"0\"\r\n";

        BufferedReader reader = new BufferedReader(new StringReader(csv));
        List<List<String>> records = CsvImporter.parseRecords(reader);

        assertEquals(4, records.size());

        // Header
        assertEquals(Arrays.asList("event", "date", "recur"), records.get(0));

        // Row 1
        assertEquals("Dentist Visit", records.get(1).get(0));
        assertEquals("2024-06-01", records.get(1).get(1));
        assertEquals("180", records.get(1).get(2));

        // Row 2 (comma inside field)
        assertEquals("Oil change, filter & lube", records.get(2).get(0));
        assertEquals("2024-07-15", records.get(2).get(1));
        assertEquals("90", records.get(2).get(2));

        // Row 3 (escaped double quote)
        assertEquals("Quote test: \"Important\"", records.get(3).get(0));
        assertEquals("2024-08-01", records.get(3).get(1));
        assertEquals("0", records.get(3).get(2));
    }

    @Test
    public void testParseManualPaymentsLegacyAttachment() throws IOException {
        // Exact content from attachment ManualPayments.csv
        String manualPaymentsCsv =
                "'KirPaint Annual Report Due','2019-05-01',365\n" +
                "'KirPaint Annual Report PAID (late+$400)','2019-06-19',0\n" +
                "'FLL Citizens 3316ins','2019-07-17',90\n" +
                "'Tags Papa (53ChevPU/HD)','2019-08-03',365\n" +
                "Tags BigMama (97VWEVC  99BMW323ic)','2019-08-03',365\n" +
                "'Tags KirPaint (2012Pilot)','2019-08-03',365\n" +
                "'OaklandParkBizTaxReceipt-$0','2019-08-08',365\n" +
                "'PropertyTax by Nov30 - FLL&PBC','2019-11-11',365\n";

        BufferedReader reader = new BufferedReader(new StringReader(manualPaymentsCsv));
        List<List<String>> records = CsvImporter.parseRecords(reader);

        assertEquals(8, records.size());

        // Row 1
        assertEquals("KirPaint Annual Report Due", records.get(0).get(0));
        assertEquals("2019-05-01", records.get(0).get(1));
        assertEquals("365", records.get(0).get(2));

        // Row 5 (the malformed single quote row)
        assertEquals("Tags BigMama (97VWEVC  99BMW323ic)", records.get(4).get(0));
        assertEquals("2019-08-03", records.get(4).get(1));
        assertEquals("365", records.get(4).get(2));

        // Row 8
        assertEquals("PropertyTax by Nov30 - FLL&PBC", records.get(7).get(0));
        assertEquals("2019-11-11", records.get(7).get(1));
        assertEquals("365", records.get(7).get(2));

        // Validate all dates parse
        for (List<String> record : records) {
            String iso = CsvImporter.parseAndFormatIsoDate(record.get(1));
            assertNotNull("Date must be valid ISO date: " + record.get(1), iso);
        }
    }

    @Test
    public void testUtf8BomHandling() throws IOException {
        byte[] bomBytes = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String content = "\"event\",\"date\",\"recur\"\n\"Event A\",\"2024-01-01\",0";
        byte[] fullBytes = new byte[bomBytes.length + content.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(bomBytes, 0, fullBytes, 0, bomBytes.length);
        System.arraycopy(content.getBytes(StandardCharsets.UTF_8), 0, fullBytes, bomBytes.length, content.getBytes(StandardCharsets.UTF_8).length);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fullBytes), StandardCharsets.UTF_8));
        List<List<String>> records = CsvImporter.parseRecords(reader);

        assertEquals(2, records.size());
        assertEquals("event", records.get(0).get(0));
        assertEquals("Event A", records.get(1).get(0));
    }

    @Test
    public void testMultiCategoryHeaders() throws IOException {
        String csv = "\"category\",\"event\",\"date\",\"recur\"\n" +
                "\"Vehicles\",\"Oil Change\",\"2024-01-01\",\"90\"\n" +
                "\"Health\",\"Dental Checkup\",\"2024-03-01\",\"180\"\n";

        BufferedReader reader = new BufferedReader(new StringReader(csv));
        List<List<String>> records = CsvImporter.parseRecords(reader);

        assertEquals(3, records.size());
        assertTrue(CsvImporter.isHeaderRow(records.get(0)));
        assertEquals("category", records.get(0).get(0));
        assertEquals("Vehicles", records.get(1).get(0));
        assertEquals("Oil Change", records.get(1).get(1));
    }

    @Test
    public void testSpecialCharactersAndApostrophes() throws IOException {
        String csv = "\"event\",\"date\",\"recur\"\n" +
                "\"Papa's Truck (53' Chevy)\",\"2024-05-10\",\"365\"\n" +
                "\"Special & Symbols: @#$% \u00E9 \u00F1\",\"2024-06-01\",\"0\"\n";

        BufferedReader reader = new BufferedReader(new StringReader(csv));
        List<List<String>> records = CsvImporter.parseRecords(reader);

        assertEquals(3, records.size());
        assertEquals("Papa's Truck (53' Chevy)", records.get(1).get(0));
        assertEquals("2024-05-10", records.get(1).get(1));
        assertEquals("365", records.get(1).get(2));
        assertEquals("Special & Symbols: @#$% \u00E9 \u00F1", records.get(2).get(0));
    }

    @Test
    public void testSanitizeFilename() {
        assertEquals("Vehicles_Maintenance", CsvExporter.sanitizeFilename("Vehicles/Maintenance"));
        assertEquals("Bills_Taxes", CsvExporter.sanitizeFilename("Bills:Taxes"));
        assertEquals("Exported_Category", CsvExporter.sanitizeFilename(""));
        assertEquals("CleanCategory", CsvExporter.sanitizeFilename("CleanCategory"));
    }
}
