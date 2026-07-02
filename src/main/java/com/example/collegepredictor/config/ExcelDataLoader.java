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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Each TG EAPCET counseling phase publishes its last-rank statement in a different layout:
 *  - FINAL:  Inst Code, Institute Name, Place, Dist Code, Co Education, College Type,
 *            Branch Code, Branch Name, OC/BCA-E/SC_I/SC_II/SC_III/ST/EWS (Boys+Girls), Affiliated To
 *  - PHASE1: College Code, College Name, Branch Code, Branch Name, OC/BCA-E/SC/ST/EWS (Boys+Girls)
 *  - PHASE2: College Code, College Name, Branch Name (no Branch Code!),
 *            OC/EWS/SC/ST/BCA-E (Boys+Girls)
 * Phase1/Phase2 don't split SC into SC_I/II/III, so their single SC value is applied to all
 * three SC sub-categories - it's the most accurate representation of the source data available.
 */
@Component
public class ExcelDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataLoader.class);

    // Drop a new cutoff-rank file into static/ and add its phase+path here to make it
    // selectable. If it uses a layout not covered by loadFinalPhaseFile/loadPhase1File/
    // loadPhase2File, add a new parsing method and route to it in loadPhaseFile below.
    // FINAL and PHASE1 carry their own branch codes and must load before PHASE2, which
    // has no code column and depends on the name->code lookup they populate.
    private static final Map<String, String> PHASE_FILES = new LinkedHashMap<>();
    static {
        PHASE_FILES.put("FINAL", "static/college_data_finalphase.xlsx");
        PHASE_FILES.put("PHASE1", "static/college_data_phase1.xlsx");
        PHASE_FILES.put("PHASE2", "static/college_data_phase2.xlsx");
    }

    private List<College> colleges = new ArrayList<>();

    // Branch-name -> branch-code lookup, built from phases that carry a real code column.
    // Used to backfill branch codes for phases (like Phase2) that only publish branch names.
    private final Map<String, String> branchNameToCode = new HashMap<>();

    @PostConstruct
    public void loadExcelData() throws IOException {
        for (Map.Entry<String, String> entry : PHASE_FILES.entrySet()) {
            loadPhaseFile(entry.getKey(), entry.getValue());
        }
        logger.info("Successfully loaded {} colleges across {} phase(s)", colleges.size(), PHASE_FILES.size());
    }

    private void loadPhaseFile(String phase, String excelFilePath) throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(excelFilePath);
            if (!resource.exists()) {
                throw new FileNotFoundException("Excel file not found: " + excelFilePath);
            }

            try (InputStream is = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(is)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IllegalStateException("Excel file has no sheets: " + excelFilePath);
                }

                logger.info("Processing {} rows for phase {}", sheet.getLastRowNum(), phase);

                switch (phase) {
                    case "FINAL":
                        loadFinalPhaseFile(sheet, phase, excelFilePath);
                        break;
                    case "PHASE1":
                        loadPhase1File(sheet, phase, excelFilePath);
                        break;
                    case "PHASE2":
                        loadPhase2File(sheet, phase, excelFilePath);
                        break;
                    default:
                        throw new IllegalStateException("No parser registered for phase: " + phase);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("Excel file not found: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Failed to load Excel file {}: {}", excelFilePath, e.getMessage());
            throw new IOException("Failed to load Excel file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while loading Excel file {}: {}", excelFilePath, e.getMessage());
            throw new IllegalStateException("Failed to load Excel data: " + e.getMessage());
        }
    }

    // Row 0 is a merged title row, row 1 is the real column header row, data starts at row 2.
    private void loadFinalPhaseFile(Sheet sheet, String phase, String excelFilePath) {
        for (Row row : sheet) {
            if (row.getRowNum() < 2) continue;
            try {
                String instCode = getStringValue(row.getCell(0));
                String branchCode = getStringValue(row.getCell(6));
                String branchName = getStringValue(row.getCell(7));
                if (instCode.isEmpty() || branchCode.isEmpty()) continue;
                rememberBranchCode(branchName, branchCode);

                College college = new College();
                college.setPhase(phase);
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
                college.setId(instCode + "-" + branchCode + "-" + phase);

                colleges.add(college);
            } catch (Exception e) {
                logger.error("Error processing row {} in {}: {}", row.getRowNum(), excelFilePath, e.getMessage());
            }
        }
    }

    // Row 0 is the column header row, data starts at row 1. Single generic SC column.
    private void loadPhase1File(Sheet sheet, String phase, String excelFilePath) {
        for (Row row : sheet) {
            if (row.getRowNum() < 1) continue;
            try {
                String instCode = getStringValue(row.getCell(0));
                String branchCode = getStringValue(row.getCell(2));
                String branchName = getStringValue(row.getCell(3));
                if (instCode.isEmpty() || branchCode.isEmpty()) continue;
                rememberBranchCode(branchName, branchCode);

                int scBoys = getIntValue(row.getCell(16));
                int scGirls = getIntValue(row.getCell(17));

                College college = new College();
                college.setPhase(phase);
                college.setInstCode(instCode);
                college.setInstituteName(getStringValue(row.getCell(1)));
                college.setBranchCode(branchCode);
                college.setBranchName(branchName);
                college.setOcBoys(getIntValue(row.getCell(4)));
                college.setOcGirls(getIntValue(row.getCell(5)));
                college.setBcaBoys(getIntValue(row.getCell(6)));
                college.setBcaGirls(getIntValue(row.getCell(7)));
                college.setBcbBoys(getIntValue(row.getCell(8)));
                college.setBcbGirls(getIntValue(row.getCell(9)));
                college.setBccBoys(getIntValue(row.getCell(10)));
                college.setBccGirls(getIntValue(row.getCell(11)));
                college.setBcdBoys(getIntValue(row.getCell(12)));
                college.setBcdGirls(getIntValue(row.getCell(13)));
                college.setBceBoys(getIntValue(row.getCell(14)));
                college.setBceGirls(getIntValue(row.getCell(15)));
                college.setScIBoys(scBoys);
                college.setScIGirls(scGirls);
                college.setScIIBoys(scBoys);
                college.setScIIGirls(scGirls);
                college.setScIIIBoys(scBoys);
                college.setScIIIGirls(scGirls);
                college.setStBoys(getIntValue(row.getCell(18)));
                college.setStGirls(getIntValue(row.getCell(19)));
                college.setEwsBoys(getIntValue(row.getCell(20)));
                college.setEwsGirls(getIntValue(row.getCell(21)));
                college.setId(instCode + "-" + branchCode + "-" + phase);

                colleges.add(college);
            } catch (Exception e) {
                logger.error("Error processing row {} in {}: {}", row.getRowNum(), excelFilePath, e.getMessage());
            }
        }
    }

    // Row 0 is the column header row, data starts at row 1. No Branch Code column -
    // branch code is looked up by (normalized) Branch Name from phases that do have one.
    // Single generic SC column, and categories are ordered differently than Phase1/Final.
    private void loadPhase2File(Sheet sheet, String phase, String excelFilePath) {
        for (Row row : sheet) {
            if (row.getRowNum() < 1) continue;
            try {
                String instCode = getStringValue(row.getCell(0));
                String branchName = getStringValue(row.getCell(2));
                if (instCode.isEmpty() || branchName.isEmpty()) continue;

                String branchCode = branchNameToCode.get(normalizeBranchName(branchName));
                if (branchCode == null) {
                    logger.warn("No branch code found for branch name '{}' in {} - skipping row {}",
                            branchName, excelFilePath, row.getRowNum());
                    continue;
                }

                int scBoys = getIntValue(row.getCell(7));
                int scGirls = getIntValue(row.getCell(8));

                College college = new College();
                college.setPhase(phase);
                college.setInstCode(instCode);
                college.setInstituteName(getStringValue(row.getCell(1)));
                college.setBranchCode(branchCode);
                college.setBranchName(branchName);
                college.setOcBoys(getIntValue(row.getCell(3)));
                college.setOcGirls(getIntValue(row.getCell(4)));
                college.setEwsBoys(getIntValue(row.getCell(5)));
                college.setEwsGirls(getIntValue(row.getCell(6)));
                college.setScIBoys(scBoys);
                college.setScIGirls(scGirls);
                college.setScIIBoys(scBoys);
                college.setScIIGirls(scGirls);
                college.setScIIIBoys(scBoys);
                college.setScIIIGirls(scGirls);
                college.setStBoys(getIntValue(row.getCell(9)));
                college.setStGirls(getIntValue(row.getCell(10)));
                college.setBcaBoys(getIntValue(row.getCell(11)));
                college.setBcaGirls(getIntValue(row.getCell(12)));
                college.setBcbBoys(getIntValue(row.getCell(13)));
                college.setBcbGirls(getIntValue(row.getCell(14)));
                college.setBccBoys(getIntValue(row.getCell(15)));
                college.setBccGirls(getIntValue(row.getCell(16)));
                college.setBcdBoys(getIntValue(row.getCell(17)));
                college.setBcdGirls(getIntValue(row.getCell(18)));
                college.setBceBoys(getIntValue(row.getCell(19)));
                college.setBceGirls(getIntValue(row.getCell(20)));
                college.setId(instCode + "-" + branchCode + "-" + phase);

                colleges.add(college);
            } catch (Exception e) {
                logger.error("Error processing row {} in {}: {}", row.getRowNum(), excelFilePath, e.getMessage());
            }
        }
    }

    private void rememberBranchCode(String branchName, String branchCode) {
        if (!branchName.isEmpty() && !branchCode.isEmpty()) {
            branchNameToCode.putIfAbsent(normalizeBranchName(branchName), branchCode);
        }
    }

    private String normalizeBranchName(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
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

    public List<String> getAvailablePhases() {
        return new ArrayList<>(PHASE_FILES.keySet());
    }
}
