package com.yourproject.backend.services.impl;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.responses.StaffPatientSearchResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.StaffPatientSearchService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffPatientSearchServiceImpl implements StaffPatientSearchService {
    private static final int MAX_RESULTS = 50;

    private final UserRepository userRepository;
    private final PatientDataProtectionService patientDataProtectionService;

    @Override
    public List<StaffPatientSearchResponse> searchByName(String name, int limit) {
        String query = normalize(name);
        if (query.isBlank()) {
            throw new BadRequestException("Patient name is required.");
        }
        if (limit < 1 || limit > MAX_RESULTS) {
            throw new BadRequestException("Result count must be between 1 and 50.");
        }

        return userRepository.findActivePatients().stream()
                .map(user -> UserResponse.from(user, patientDataProtectionService))
                .filter(user -> user.getFullName() != null && !user.getFullName().isBlank())
                .map(user -> new RankedPatient(
                        StaffPatientSearchResponse.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .build(),
                        rank(query, normalize(user.getFullName()))))
                .sorted(Comparator.comparing(RankedPatient::rank)
                        .thenComparing(item -> normalize(item.patient().getFullName()))
                        .thenComparing(item -> item.patient().getId()))
                .limit(limit)
                .map(RankedPatient::patient)
                .toList();
    }

    private MatchRank rank(String query, String candidate) {
        int category;
        if (candidate.equals(query)) {
            category = 0;
        } else if (candidate.startsWith(query)) {
            category = 1;
        } else if (containsWordStartingWith(candidate, query)) {
            category = 2;
        } else if (candidate.contains(query)) {
            category = 3;
        } else {
            category = 4;
        }
        return new MatchRank(category, levenshteinDistance(query, candidate), Math.abs(candidate.length() - query.length()));
    }

    private boolean containsWordStartingWith(String candidate, String query) {
        for (String word : candidate.split(" ")) {
            if (word.startsWith(query)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int substitutionCost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                        previous[rightIndex - 1] + substitutionCost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private record RankedPatient(StaffPatientSearchResponse patient, MatchRank rank) {
    }

    private record MatchRank(int category, int editDistance, int lengthDifference) implements Comparable<MatchRank> {
        @Override
        public int compareTo(MatchRank other) {
            int categoryComparison = Integer.compare(category, other.category);
            if (categoryComparison != 0) return categoryComparison;
            int distanceComparison = Integer.compare(editDistance, other.editDistance);
            if (distanceComparison != 0) return distanceComparison;
            return Integer.compare(lengthDifference, other.lengthDifference);
        }
    }
}
