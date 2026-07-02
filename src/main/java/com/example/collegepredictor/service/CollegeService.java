package com.example.collegepredictor.service;

import com.example.collegepredictor.config.ExcelDataLoader;
import com.example.collegepredictor.model.College;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.example.collegepredictor.dto.BranchDto;

@Service
public class CollegeService {

    @Autowired
    private ExcelDataLoader excelDataLoader;

    // Safety margin so a student also sees "reach" colleges near their rank,
    // not only colleges they're guaranteed to clear.
    private static final int RANK_BUFFER = 1000;

    public List<College> predictColleges(int rank, String gender, String category, String phase, List<String> branches) {
        List<College> allColleges = excelDataLoader.getColleges();

        int eligibleRank = Math.max(rank - RANK_BUFFER, 1);
        List<String> branchFilter = (branches == null) ? List.of() : branches;
        boolean noBranchFilter = branchFilter.isEmpty();
        boolean noPhaseFilter = (phase == null || phase.isBlank());

        return allColleges.stream()
                .filter(college -> noPhaseFilter || phase.equalsIgnoreCase(college.getPhase()))
                .filter(college -> noBranchFilter || branchFilter.contains(college.getBranchCode()))
                .filter(college -> {
                    int cutoffRank = getCutoffRank(college, gender, category);
                    return eligibleRank <= cutoffRank;
                })
                .sorted(Comparator.comparingInt(college -> getCutoffRank(college, gender, category)))
                .collect(Collectors.toList());
    }

    private int getCutoffRank(College college, String gender, String category) {
        if (category == null || gender == null) {
            return Integer.MAX_VALUE;
        }

        boolean male = "MALE".equalsIgnoreCase(gender);
        switch (category.toUpperCase()) {
            case "OC":
                return male ? college.getOcBoys() : college.getOcGirls();
            case "BC-A":
                return male ? college.getBcaBoys() : college.getBcaGirls();
            case "BC-B":
                return male ? college.getBcbBoys() : college.getBcbGirls();
            case "BC-C":
                return male ? college.getBccBoys() : college.getBccGirls();
            case "BC-D":
                return male ? college.getBcdBoys() : college.getBcdGirls();
            case "BC-E":
                return male ? college.getBceBoys() : college.getBceGirls();
            case "SC-I":
                return male ? college.getScIBoys() : college.getScIGirls();
            case "SC-II":
                return male ? college.getScIIBoys() : college.getScIIGirls();
            case "SC-III":
                return male ? college.getScIIIBoys() : college.getScIIIGirls();
            case "ST":
                return male ? college.getStBoys() : college.getStGirls();
            case "EWS":
                return male ? college.getEwsBoys() : college.getEwsGirls();
            default:
                return Integer.MAX_VALUE;
        }
    }

    public List<BranchDto> getAllBranchesWithNames() {
        return excelDataLoader.getColleges().stream()
                .collect(Collectors.toMap(
                        College::getBranchCode,
                        College::getBranchName,
                        (name1, name2) -> name1))
                .entrySet().stream()
                .map(e -> new BranchDto(e.getKey(), e.getValue()))
                .sorted((a, b) -> a.code.compareTo(b.code))
                .collect(Collectors.toList());
    }

    public List<String> getAvailablePhases() {
        return excelDataLoader.getAvailablePhases();
    }
}
