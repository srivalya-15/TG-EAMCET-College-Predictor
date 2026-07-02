package com.example.collegepredictor.controller;

import com.example.collegepredictor.model.College;
import com.example.collegepredictor.service.CollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.collegepredictor.dto.BranchDto;

@RestController
@CrossOrigin(origins = "*")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @GetMapping("/api/branches")
    public List<BranchDto> getBranches() {
        return collegeService.getAllBranchesWithNames();
    }

    @GetMapping("/api/phases")
    public List<String> getPhases() {
        return collegeService.getAvailablePhases();
    }

    @PostMapping("/api/predict")
    public List<College> predictColleges(
            @RequestParam int rank,
            @RequestParam String gender,
            @RequestParam String category,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) List<String> branches) {
        return collegeService.predictColleges(rank, gender, category, phase, branches);
    }
}
