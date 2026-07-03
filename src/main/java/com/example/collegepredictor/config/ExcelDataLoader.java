package com.example.collegepredictor.config;

import com.example.collegepredictor.model.College;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the TG EAPCET last-rank statement:
 * Inst Code, Institute Name, Place, Dist Code, Co Education, College Type,
 * Branch Code, Branch Name, OC/BCA-E/SC_I/SC_II/SC_III/ST/EWS (Boys+Girls), Affiliated To
 */
@Component
public class ExcelDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataLoader.class);

    private static final String DATA_FILE = "static/college_data.xlsx";

    private List<College> colleges = new ArrayList<>();

    @PostConstruct
    public void loadExcelData() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(DATA_FILE);
            if (!resource.exists()) {
                throw new FileNotFoundException("Excel file not found: " + DATA_FILE);
            }

            try (InputStream is = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(is)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IllegalStateException("Excel file has no sheets: " + DATA_FILE);
                }

                logger.info("Processing {} rows", sheet.getLastRowNum());
                loadCollegeData(sheet);
            }
        } catch (FileNotFoundException e) {
            logger.error("Excel file not found: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Failed to load Excel file {}: {}", DATA_FILE, e.getMessage());
            throw new IOException("Failed to load Excel file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while loading Excel file {}: {}", DATA_FILE, e.getMessage());
            throw new IllegalStateException("Failed to load Excel data: " + e.getMessage());
        }

        logger.info("Successfully loaded {} colleges", colleges.size());
    }

    // Row 0 is a merged title row, row 1 is the real column header row, data starts at row 2.
    private void loadCollegeData(Sheet sheet) {
        for (Row row : sheet) {
            if (row.getRowNum() < 2) continue;
            try {
                String instCode = getStringValue(row.getCell(0));
                String branchCode = getStringValue(row.getCell(6));
                String branchName = getStringValue(row.getCell(7));
                if (instCode.isEmpty() || branchCode.isEmpty()) continue;

                College college = new College();
                college.setInstCode(instCode);
                college.setInstituteName(getStringValue(row.getCell(1)));
                college.setPlace(getStringValue(row.getCell(2)));
                college.setDistCode(getStringValue(row.getCell(3)));
                college.setEducation(getStringValue(row.getCell(4)));
                college.setCollegeType(getStringValue(row.getCell(5)));
                college.setBranchCode(branchCode);
                college.setBranchName(branchName);
                college.setOcBoys(getIntValue(row.getCell(8)));
                college.setOcGirls(getIntValue(row.getCell(9)));
                college.setBcaBoys(getIntValue(row.getCell(10)));
                college.setBcaGirls(getIntValue(row.getCell(11)));
                college.setBcbBoys(getIntValue(row.getCell(12)));
                college.setBcbGirls(getIntValue(row.getCell(13)));
                college.setBccBoys(getIntValue(row.getCell(14)));
                college.setBccGirls(getIntValue(row.getCell(15)));
                college.setBcdBoys(getIntValue(row.getCell(16)));
                college.setBcdGirls(getIntValue(row.getCell(17)));
                college.setBceBoys(getIntValue(row.getCell(18)));
                college.setBceGirls(getIntValue(row.getCell(19)));
                college.setScIBoys(getIntValue(row.getCell(20)));
                college.setScIGirls(getIntValue(row.getCell(21)));
                college.setScIIBoys(getIntValue(row.getCell(22)));
                college.setScIIGirls(getIntValue(row.getCell(23)));
                college.setScIIIBoys(getIntValue(row.getCell(24)));
                college.setScIIIGirls(getIntValue(row.getCell(25)));
                college.setStBoys(getIntValue(row.getCell(26)));
                college.setStGirls(getIntValue(row.getCell(27)));
                college.setEwsBoys(getIntValue(row.getCell(28)));
                college.setEwsGirls(getIntValue(row.getCell(29)));
                college.setAffiliatedTo(getStringValue(row.getCell(30)));
                college.setId(instCode + "-" + branchCode);

                colleges.add(college);
            } catch (Exception e) {
                logger.error("Error processing row {}: {}", row.getRowNum(), e.getMessage());
            }
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    return String.valueOf((int) cell.getNumericCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private int getIntValue(Cell cell) {
        if (cell == null) return 0;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? 0 : Integer.parseInt(value);
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public List<College> getColleges() {
        return colleges;
    }
}
